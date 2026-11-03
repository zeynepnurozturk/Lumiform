---
name: review-changes
description: Reviews finished code changes for correctness bugs and safety issues in a read-only subagent, runs the project's tests, and returns a verdict with ranked findings. Use when a coding task is complete, before reporting it as done.
when_to_use: Use after any task that changed source, tests, build files or config; skip docs-only changes. Pass what the change was meant to do, the files it touched, and the base branch if the work is on a branch. Verify each finding in the code before fixing it, re-review after non-trivial fixes, and report the actual result to the user.
argument-hint: <what the change does, files touched> [base branch]
context: fork
agent: change-reviewer
model: sonnet
background: false
allowed-tools: Bash(git status *) Bash(git diff *)
---

Review the change described below.

## What the change is meant to do

$ARGUMENTS

## Working tree

```!
git status --short || true
git diff HEAD --stat || true
```

Read the full diff with `git diff HEAD`. If the brief names a base branch, use `git diff <base>...HEAD` plus any uncommitted changes. Read new untracked files directly. If git shows nothing, review the files named in the brief.

Then review, run the tests, and report as your instructions describe.
