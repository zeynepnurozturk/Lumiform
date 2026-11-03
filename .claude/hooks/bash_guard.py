#!/usr/bin/env python3
"""PreToolUse guard for Bash commands.

Rejects dangerous commands, asks before risky ones and allows read-only ones.
A command line is split into its simple commands (&&, ||, ;, |, newlines,
subshells, command substitutions); each one is classified and the strictest
result wins. A command on no list gets no decision, so Claude Code's normal
permission check decides.

Edit the sets below to change what is rejected, asked or allowed.
"""

import json
import os
import re
import shlex
import sys

# Ordered so max() picks the strictest result. UNKNOWN outranks ALLOW: one
# unrecognized command means the whole line is not known to be safe.
ALLOW, UNKNOWN, ASK, DENY = range(4)

DENY_COMMANDS = {"sudo", "su", "doas", "dd", "shutdown", "reboot", "halt", "poweroff"}

ASK_COMMANDS = {
    # deleting, moving and permissions
    "rm", "rmdir", "unlink", "shred", "mv", "truncate", "chmod", "chown", "chgrp",
    # processes
    "kill", "killall", "pkill",
    # network and remote access
    "curl", "wget", "ssh", "scp", "sftp", "rsync", "ftp", "nc", "telnet",
    # environment dumps can contain secrets
    "printenv",
    # system configuration
    "crontab", "osascript", "security", "launchctl",
}

READ_ONLY_COMMANDS = {
    "ls", "pwd", "cd", "pushd", "popd", "cat", "head", "tail", "less", "more",
    "wc", "grep", "egrep", "fgrep", "rg", "ag", "fd", "tree", "stat", "file",
    "du", "df", "which", "whereis", "type", "echo", "printf", "true", "false",
    "test", "[", "date", "cal", "uname", "whoami", "id", "uptime", "sw_vers",
    "ps", "sort", "uniq", "cut", "tr", "nl", "column", "fold", "rev", "paste",
    "join", "diff", "cmp", "comm", "basename", "dirname", "realpath",
    "readlink", "jq", "md5", "md5sum", "shasum", "sha1sum", "sha256sum",
    "cksum", "xxd", "hexdump", "od", "strings", "sleep", "seq", "expr", "man",
}

GIT_READ_ONLY = {
    "status", "diff", "log", "show", "blame", "annotate", "shortlog",
    "describe", "rev-parse", "rev-list", "ls-files", "ls-tree", "ls-remote",
    "cat-file", "grep", "show-ref", "show-branch", "for-each-ref", "name-rev",
    "merge-base", "whatchanged", "count-objects", "check-ignore", "check-attr",
    "var", "help", "version", "range-diff", "cherry", "diff-tree",
    "diff-files", "diff-index", "verify-commit", "verify-tag", "fsck", "fetch",
}

GIT_ASK = {
    "commit", "add", "rm", "mv", "merge", "rebase", "cherry-pick", "revert",
    "reset", "restore", "checkout", "switch", "clean", "pull", "init", "clone",
    "am", "apply", "gc", "prune", "repack", "filter-branch", "filter-repo",
    "update-ref", "update-index", "symbolic-ref", "replace", "bisect",
    "mergetool", "rerere", "format-patch", "send-email", "lfs",
}

GIT_DENY = {"push"}

GH_DENY = {("pr", "merge"), ("repo", "delete"), ("release", "create"),
           ("release", "delete"), ("release", "upload")}
GH_READ_ONLY_ACTIONS = {"view", "list", "status", "diff", "checks", "watch"}

# tool: (deny subcommands, ask subcommands, allow subcommands, fallback)
SUBCOMMAND_RULES = {
    "npm": ({"publish", "unpublish"},
            {"install", "i", "ci", "add", "uninstall", "remove", "rm", "un",
             "update", "upgrade", "up", "link", "unlink"},
            {"ls", "list", "view", "outdated", "why", "help"}, UNKNOWN),
    "yarn": ({"publish"},
             {"install", "add", "remove", "upgrade", "up", "link", "unlink"},
             {"list", "info", "why", "outdated"}, UNKNOWN),
    "pnpm": ({"publish"},
             {"install", "i", "add", "remove", "rm", "update", "up", "link", "unlink"},
             {"list", "ls", "why", "outdated"}, UNKNOWN),
    "pip": (set(), {"install", "uninstall", "download"},
            {"list", "show", "freeze", "check"}, UNKNOWN),
    "pip3": (set(), {"install", "uninstall", "download"},
             {"list", "show", "freeze", "check"}, UNKNOWN),
    "brew": (set(),
             {"install", "uninstall", "remove", "rm", "reinstall", "upgrade",
              "update", "tap", "untap", "cleanup", "link", "unlink", "services"},
             {"list", "ls", "info", "search", "outdated", "deps", "leaves",
              "config", "doctor"}, UNKNOWN),
    "cargo": ({"publish"}, {"install", "uninstall"}, set(), UNKNOWN),
    "adb": (set(), set(), {"devices", "version", "get-state", "get-serialno", "help"}, ASK),
    "defaults": (set(), set(), {"read", "domains", "find"}, ASK),
    "diskutil": (set(), set(), {"list", "info"}, DENY),
}

SHELLS = {"sh", "bash", "zsh", "dash", "ksh"}
WRAPPERS = {"time", "nice", "nohup", "command", "builtin", "exec"}
XARGS_OPTIONS_WITH_VALUE = {"-I", "-J", "-L", "-n", "-P", "-R", "-S", "-s", "-d", "-E", "-a"}
FIND_WRITES = {"-delete", "-exec", "-execdir", "-ok", "-okdir",
               "-fprint", "-fprint0", "-fprintf", "-fls"}
PROTECTED_LITERALS = {"/", "/*", "~", "~/", "~/*", "*", ".", "./", "..", "../",
                      "$HOME", "${HOME}", "$HOME/", "${HOME}/"}

ASSIGNMENT = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=")
HEREDOC = re.compile(
    r"<<-?[ \t]*(['\"]?)([A-Za-z_][A-Za-z0-9_]*)\1([^\n]*)\n.*?^[ \t]*\2[ \t]*$",
    re.S | re.M)
SUBSTITUTION = re.compile(r"\$\(([^()]*)\)|`([^`]*)`")
DOWNLOAD_TO_SHELL = re.compile(
    r"\b(curl|wget)\b[^\n]*\|\s*(sudo\s+)?(env\s+)?(ba|z|k|da)?sh\b"
    r"|\b(curl|wget)\b[^\n]*\|\s*(python3?|perl|ruby|node)\b")
OPERATOR_CHARS = set("();<>|&")


def first_positional(args):
    return next((a for a in args if not a.startswith("-")), None)


def is_protected_path(arg, cwd):
    """True for /, home, the project and any folder that contains them."""
    if arg in PROTECTED_LITERALS:
        return True
    path = os.path.realpath(os.path.join(cwd, os.path.expanduser(os.path.expandvars(arg))))
    home = os.path.realpath(os.path.expanduser("~"))
    project = os.path.realpath(os.environ.get("CLAUDE_PROJECT_DIR") or cwd)
    prefix = path.rstrip("/") + "/"
    return any(p == path or p.startswith(prefix) for p in (home, project))


def classify_git(args):
    i = 0
    while i < len(args):
        if args[i] in ("-C", "-c", "--git-dir", "--work-tree", "--namespace"):
            i += 2
        elif args[i].startswith("-"):
            i += 1
        else:
            break
    if i >= len(args):
        return ALLOW, "git"
    sub, rest = args[i], args[i + 1:]
    label = "git " + sub
    first = rest[0] if rest else None
    if sub in GIT_DENY:
        return DENY, label
    if sub in GIT_READ_ONLY:
        return ALLOW, label
    if sub in GIT_ASK:
        return ASK, label
    if sub == "branch":
        writes = {"-d", "-D", "--delete", "-m", "-M", "--move", "-c", "-C", "--copy",
                  "-f", "--force", "-u", "--unset-upstream", "--edit-description",
                  "-t", "--track", "--no-track"}
        lists = {"-l", "--list", "--contains", "--no-contains", "--merged",
                 "--no-merged", "--points-at"}
        creates = first_positional(rest) and not lists.intersection(rest)
        changes = writes.intersection(rest) or any(a.startswith("--set-upstream-to") for a in rest)
        return (ASK if changes or creates else ALLOW), label
    if sub == "tag":
        writes = {"-a", "-s", "-u", "-d", "--delete", "-f", "--force", "-m", "-F",
                  "--annotate", "--sign"}
        lists = {"-l", "--list", "--contains", "--no-contains", "--merged",
                 "--no-merged", "--points-at"}
        creates = first_positional(rest) and not lists.intersection(rest)
        return (ASK if writes.intersection(rest) or creates else ALLOW), label
    if sub == "stash":
        return (ALLOW if first in ("list", "show") else ASK), label
    if sub == "config":
        writes = {"--unset", "--unset-all", "--add", "--replace-all",
                  "--rename-section", "--remove-section", "-e", "--edit"}
        positionals = [a for a in rest if not a.startswith("-")]
        return (ASK if writes.intersection(rest) or len(positionals) >= 2 else ALLOW), label
    if sub == "remote":
        return (ALLOW if first in (None, "-v", "--verbose", "show", "get-url") else ASK), label
    if sub == "reflog":
        return (ASK if first in ("expire", "delete") else ALLOW), label
    if sub in ("worktree", "notes", "submodule"):
        return (ALLOW if first in ("list", "show", "status") else ASK), label
    return UNKNOWN, label


def classify_gh(args):
    words = [a for a in args if not a.startswith("-")]
    if not words:
        return ALLOW, "gh"
    group = words[0]
    action = words[1] if len(words) > 1 else None
    label = " ".join(["gh"] + words[:2])
    if (group, action) in GH_DENY:
        return DENY, label
    if group in ("search", "status", "help", "version") or (group, action) == ("auth", "status"):
        return ALLOW, label
    if action in GH_READ_ONLY_ACTIONS:
        return ALLOW, label
    return ASK, label


def classify_gradle(args):
    tasks = [a.rsplit(":", 1)[-1] for a in args if not a.startswith("-")]
    if any(t.startswith("publish") or t == "uploadArchives" for t in tasks):
        return DENY, "gradle publish"
    return ALLOW, "gradle"


def skip_options(args, with_value=()):
    i = 0
    while i < len(args) and args[i].startswith("-"):
        i += 2 if args[i] in with_value else 1
    return args[i:]


def classify_argv(argv, cwd, depth):
    while argv and ASSIGNMENT.match(argv[0]):
        argv = argv[1:]
    if not argv:
        return ALLOW, "variable assignment"
    name, args = os.path.basename(argv[0]), argv[1:]

    if name in WRAPPERS:
        return classify_argv(skip_options(args, {"-n"}), cwd, depth)
    if name == "env":
        if "-S" in args or "--split-string" in args:
            return ASK, "env -S"
        rest = skip_options(args, {"-u", "--unset", "-C", "--chdir"})
        while rest and ASSIGNMENT.match(rest[0]):
            rest = rest[1:]
        return classify_argv(rest, cwd, depth) if rest else (ASK, "env (prints the environment)")
    if name == "timeout":
        rest = skip_options(args, {"-s", "--signal", "-k", "--kill-after"})
        return classify_argv(rest[1:], cwd, depth)
    if name == "xargs":
        rest = skip_options(args, XARGS_OPTIONS_WITH_VALUE)
        return classify_argv(rest, cwd, depth) if rest else (ALLOW, "xargs")
    if name in SHELLS:
        for i, a in enumerate(args):
            if a.startswith("-") and not a.startswith("--") and "c" in a:
                script = args[i + 1] if i + 1 < len(args) else ""
                return classify_line(script, cwd, depth + 1)
        if first_positional(args) is None:
            return ASK, f"{name} reading commands from input"
        return UNKNOWN, name
    if name == "eval":
        return classify_line(" ".join(args), cwd, depth + 1)

    if name in DENY_COMMANDS or name.startswith("mkfs"):
        return DENY, name
    if name == "git":
        return classify_git(args)
    if name == "gh":
        return classify_gh(args)
    if name in ("gradlew", "gradle"):
        return classify_gradle(args)
    if name in SUBCOMMAND_RULES:
        deny, ask, allow, fallback = SUBCOMMAND_RULES[name]
        sub = first_positional(args)
        label = f"{name} {sub}" if sub else name
        if sub in deny:
            return DENY, label
        if sub in ask:
            return ASK, label
        if sub is None or sub in allow:
            return ALLOW, label
        return fallback, label
    if name == "rm":
        targets = [a for a in args if not a.startswith("-")]
        if "--no-preserve-root" in args or any(is_protected_path(t, cwd) for t in targets):
            return DENY, "rm on /, home or the project folder"
        return ASK, "rm"
    if name in ("chmod", "chown", "chgrp"):
        recursive = any(a == "--recursive" or (a.startswith("-") and not a.startswith("--") and "R" in a)
                        for a in args)
        targets = [a for a in args if not a.startswith("-")]
        if recursive and any(is_protected_path(t, cwd) for t in targets[1:]):
            return DENY, f"recursive {name} on /, home or the project folder"
        return ASK, name
    if name in ASK_COMMANDS:
        return ASK, name
    if name == "find":
        return (ASK if FIND_WRITES.intersection(args) else ALLOW), "find"
    if name == "sed":
        in_place = any(a == "--in-place" or a.startswith("--in-place=")
                       or (a.startswith("-") and not a.startswith("--") and "i" in a[1:])
                       for a in args)
        return (ASK if in_place else ALLOW), "sed"
    if name == "sort" and any(a.startswith("-o") or a.startswith("--output") for a in args):
        return UNKNOWN, "sort -o"
    if name in READ_ONLY_COMMANDS:
        return ALLOW, name
    return UNKNOWN, name


def tokenize(text):
    lexer = shlex.shlex(text, posix=True, punctuation_chars=True)
    lexer.whitespace_split = True
    lexer.commenters = ""  # classify comment text too rather than let it hide commands
    return list(lexer)


def classify_line(command, cwd, depth=0):
    if depth > 5:
        return UNKNOWN, "deeply nested command"
    text = HEREDOC.sub(lambda m: "<< HEREDOC" + m.group(3), command).replace("\\\n", " ")
    results = []

    if DOWNLOAD_TO_SHELL.search(text):
        results.append((DENY, "piping a download into an interpreter"))
    if "$(" in text or "`" in text or "<(" in text or ">(" in text:
        results.append((UNKNOWN, "command substitution"))
        for groups in SUBSTITUTION.findall(text):
            inner = groups[0] or groups[1]
            if inner.strip():
                results.append(classify_line(inner, cwd, depth + 1))

    try:
        tokens = tokenize(text.replace("\n", " ; "))
    except ValueError:
        results.append((UNKNOWN, "unparseable command"))
        tokens = []

    segment, writes_file = [], False

    def flush():
        nonlocal segment, writes_file
        if segment:
            level, label = classify_argv(segment, cwd, depth)
            if writes_file and level == ALLOW:
                level, label = UNKNOWN, label + " writing to a file"
            results.append((level, label))
        segment, writes_file = [], False

    i = 0
    while i < len(tokens):
        tok = tokens[i]
        if tok and set(tok) <= OPERATOR_CHARS:
            if "(" in tok or ")" in tok or not ({"<", ">"} & set(tok)):
                flush()
                i += 1
                continue
            target = tokens[i + 1] if i + 1 < len(tokens) else ""
            if segment and segment[-1].isdigit():
                segment.pop()  # file descriptor number, as in 2>/dev/null
            if ">" in tok and not tok.endswith("&") and target != "/dev/null":
                writes_file = True
            i += 2
            continue
        segment.append(tok)
        i += 1
    flush()

    return max(results, key=lambda r: r[0]) if results else (UNKNOWN, "empty command")


def main():
    data = json.load(sys.stdin)
    command = (data.get("tool_input") or {}).get("command") or ""
    cwd = data.get("cwd") or os.getcwd()
    level, label = classify_line(command, cwd)
    if level == UNKNOWN:
        return  # no opinion: the normal permission check decides
    decision, reason = {
        DENY: ("deny", f"Blocked by the project's Bash guard: {label}. "
                       "Ask the user to run this themselves if it is needed."),
        ASK: ("ask", f"Bash guard: {label} needs your approval."),
        ALLOW: ("allow", "Bash guard: read-only command."),
    }[level]
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": decision,
        "permissionDecisionReason": reason,
    }}))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # never let a guard bug silently allow a command
        print(json.dumps({"hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "ask",
            "permissionDecisionReason": f"Bash guard error ({error}); approve manually.",
        }}))
