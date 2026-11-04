package com.task.lumiform.domain.model

/**
 * Where the user's answers are on their way to the server, shown on the form screen.
 */
enum class SyncStatus {
    // Every answer has been sent.
    Synced,

    // Answers are saved in Room but their page has not been queued yet (it is queued on Next or Finish).
    SavedOnDevice,

    // A page is queued and the device is offline, so it waits for a connection.
    WaitingForNetwork,

    // A page is being sent, or is queued and about to be sent.
    Sending,

    // The last send failed and the next attempt is waiting for its backoff.
    Retrying,
}
