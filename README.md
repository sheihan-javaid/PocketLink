# PocketLink

PocketLink lets your Android phone and your computer work together over your local
Wi-Fi network, with all traffic TLS-encrypted.

> **PocketLink is a fork of [KDE Connect for Android](https://invent.kde.org/network/kdeconnect-android)**
> by the KDE community. It keeps the KDE Connect protocol, so it pairs with the
> regular KDE Connect desktop app. All credit for the original app goes to the KDE
> Connect developers. PocketLink is an independent project, not affiliated with or
> endorsed by KDE. Please report PocketLink bugs
> [here](https://github.com/sheihan-javaid/PocketLink/issues), not to KDE.

## Features

- **Shared clipboard**: copy on one device, paste on the other.
- **Notification sync**: read and reply to Android notifications from the desktop.
- **Share files and URLs** between devices.
- **Media remote control** for desktop media players.
- **Virtual touchpad and keyboard**: use the phone as the computer's mouse and keyboard.
- **SMS from the desktop**, call notifications, find my phone, battery status, remote commands, and more.

## Install

PocketLink is distributed only through
[GitHub Releases](https://github.com/sheihan-javaid/PocketLink/releases). It is
**not** on Google Play or F-Droid.

1. Download `PocketLink-<version>.apk`, and optionally `PocketLink-<version>.apk.sha256`, from the latest release.
2. Optional: verify the download with `sha256sum -c PocketLink-<version>.apk.sha256`
   (macOS: `shasum -a 256 -c ...`).
3. Open the APK on your phone. Android asks you to allow installs from that source
   (*Settings → Apps → Special app access → Install unknown apps*). Allow it for your
   browser or file manager, and turn it off again afterwards if you like.
4. Install, then open PocketLink.
5. On your computer, install the KDE Connect desktop app, for example the
   [KDE Connect desktop app](https://kdeconnect.kde.org/download.html) (Linux, Windows, macOS)
   or GSConnect (GNOME). Both devices must be on the same network.

**Updates:** install the newer APK over the existing one. Your data and pairings are kept.
Every release is signed with the same key, and Android rejects an update signed with a
different key.

PocketLink uses its own app ID (`com.pocketlink`), so it can be installed alongside the
official KDE Connect app.

## Permissions

PocketLink asks for these permissions. Most are requested only when you enable the
feature that needs them, and you can deny any of them. The related feature then stops working.

| Permission(s) | Why |
|---|---|
| Internet, network state, Wi-Fi state/multicast, change network/Wi-Fi state, local network | Discover and talk to paired devices on your local network |
| Bluetooth, Bluetooth connect/scan | Optional Bluetooth connections to devices |
| Location (coarse, fine, background) | Android requires it to read the Wi-Fi network name (SSID), for the *trusted networks* feature and connectivity report |
| Notifications listener (granted in system settings), post notifications | Sync and reply to notifications; show pairing and transfer notifications |
| Read phone state, read call log | Notify the desktop about incoming and missed calls, and show caller name and number |
| SMS / MMS (send, read, receive), read contacts | Read and send text messages from the desktop; share contacts with paired devices |
| Storage (read/write, all files access) | Send and receive files; browse the phone from the desktop (SFTP) |
| Accessibility service (granted in system settings) | *Remote input*: lets the desktop move a cursor and type on the phone |
| Record audio | Voice input (mic button) in the Plasma Bigscreen remote |
| Camera | Flashlight for *Find my phone* |
| Draw over other apps, read logs | Clipboard sync on Android 10+, which restricts background clipboard access |
| Full-screen intent, vibrate, wake lock, foreground service | Ring loudly for *Find my phone*; keep the connection alive in the background |
| Run at startup | Reconnect to paired devices after a reboot |
| Query all packages | Choose which apps' notifications are synced |
| Install packages, download without notification | Open received APK files; receive files silently |

`READ_LOGS` can only be granted over `adb`:
`adb shell pm grant com.pocketlink android.permission.READ_LOGS`.

## Building

```sh
./gradlew assembleDebug        # debug APK: build/outputs/apk/com.pocketlink.apk
./gradlew testDebugUnitTest    # unit tests
./gradlew assembleRelease      # release APK (signed if a release key is configured)
```

See [RELEASING.md](RELEASING.md) for signing and publishing releases, and
[CHANGELOG.md](CHANGELOG.md) for release notes.

## Translations

Translations come from KDE Connect and are maintained by the
[KDE localization teams](https://l10n.kde.org/). PocketLink-specific strings are
English-only for now.

## License

PocketLink, like KDE Connect, is free software under the
[GNU GPL v2](https://www.gnu.org/licenses/gpl-2.0.html) or
[GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html), or any later version accepted
by KDE e.V. See [COPYING](COPYING) and the [LICENSES/](LICENSES/) folder. Individual
files carry SPDX headers naming their copyright holders. Third-party library licenses
are listed in the app under *About → Licenses*.

The full source code of every release is in this repository, at the matching `v<version>` tag.
