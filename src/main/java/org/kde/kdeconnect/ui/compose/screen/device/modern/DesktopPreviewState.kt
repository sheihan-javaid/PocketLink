/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import androidx.compose.runtime.Immutable

/**
 * State of the desktop wallpaper preview for a connected companion device.
 *
 * Tracks whether wallpaper preview data is available for a connected Mac desktop
 * companion, and whether it can be displayed online or only offline (cached).
 */
sealed interface DesktopPreviewState {

    /**
     * Initial state: preview availability is being determined or has not been determined yet.
     */
    object Waiting : DesktopPreviewState

    /**
     * The remote companion does not advertise wallpaper capability in its outgoing capabilities.
     */
    object NotSupported : DesktopPreviewState

    /**
     * The wallpaper plugin is present but disabled on the remote companion.
     */
    object Disabled : DesktopPreviewState

    /**
     * Wallpaper preview data is available and the companion device is reachable.
     *
     * @param path absolute filesystem path to the cached wallpaper image
     * @param sha256 SHA-256 hash of the wallpaper image for cache validation
     */
    @Immutable
    data class Available(val path: String, val sha256: String) : DesktopPreviewState

    /**
     * Wallpaper preview data is available but the companion device is currently unreachable.
     * Only cached data can be displayed.
     *
     * @param path absolute filesystem path to the cached wallpaper image
     * @param sha256 SHA-256 hash of the wallpaper image for cache validation
     */
    @Immutable
    data class AvailableOffline(val path: String, val sha256: String) : DesktopPreviewState
}
