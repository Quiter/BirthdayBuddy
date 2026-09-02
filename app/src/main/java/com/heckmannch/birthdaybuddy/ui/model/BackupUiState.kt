package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable

/**
 * Sealed interface representing one-time user-facing status messages resulting from backup operations.
 */
@Immutable
sealed interface BackupMessage {
    /**
     * Indicates that the gift ideas were exported successfully.
     */
    data object ExportSuccess : BackupMessage

    /**
     * Indicates that exporting gift ideas failed.
     *
     * @property errorMessage The detail error message, if available.
     */
    data class ExportError(val errorMessage: String?) : BackupMessage

    /**
     * Indicates that gift ideas were imported successfully.
     *
     * @property count The number of imported gift ideas.
     */
    data class ImportSuccess(val count: Int) : BackupMessage

    /**
     * Indicates that the selected backup file has an invalid format.
     */
    data object ImportInvalid : BackupMessage

    /**
     * Indicates that importing gift ideas failed.
     *
     * @property errorMessage The detail error message, if available.
     */
    data class ImportError(val errorMessage: String?) : BackupMessage
}

/**
 * Immutable UI state for the Backup screen.
 *
 * @property isLoading Indicates whether a backup export or import operation is currently in progress.
 * @property message The current one-time status message to display to the user, or `null` if none.
 */
@Immutable
data class BackupUiState(
    val isLoading: Boolean = false,
    val message: BackupMessage? = null,
)
