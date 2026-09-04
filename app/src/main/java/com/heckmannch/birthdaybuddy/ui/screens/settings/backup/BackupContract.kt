package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

/**
 * Sealed interface representing all MVI intents (actions) that can be sent
 * from the view/composable to the [BackupViewModel].
 */
sealed interface BackupIntent {
    /**
     * Intent to export all gift ideas to the given destination [uriString].
     */
    data class ExportGiftIdeas(val uriString: String) : BackupIntent

    /**
     * Intent to import gift ideas from the given source [uriString].
     */
    data class ImportGiftIdeas(val uriString: String) : BackupIntent

    /**
     * Intent to clear the current status message after it has been displayed to the user.
     */
    data object ClearMessage : BackupIntent
}
