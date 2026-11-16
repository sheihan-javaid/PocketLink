# Changelog

All notable changes to PocketLink are listed here. Versions match `versionName` in
`build.gradle.kts` and the `v<version>` Git tags. Paste the section for a release into
its GitHub Release notes.

<!--
Template for a new release (copy above the latest entry):

## [X.Y.Z] - YYYY-MM-DD

### Added
-
### Changed
-
### Fixed
-
### Security
-

**APK:** `PocketLink-X.Y.Z.apk` (universal, Android 6.0+)
**Signing certificate SHA-256:** `<same digest every release; from apksigner verify --print-certs>`
**Checksum:** see `PocketLink-X.Y.Z.apk.sha256`
-->

## [0.1.0] - 2026-11-16

**Pre-release.** First PocketLink release, based on KDE Connect for Android 1.37.2.

### Changed
- Rebranded as PocketLink with its own app ID, `com.pocketlink`. It can be installed alongside KDE Connect.
- Modern Compose dashboard with flat cards, a violet-tinted background and plugin settings from a gear icon.
- Settings are now stored per device and per plugin. See [docs/RELEASE_NOTES_2026-11-16.md](docs/RELEASE_NOTES_2026-11-16.md) for migration details.
- About screen links point to the PocketLink GitHub repository and show an "A fork of KDE Connect" line. KDE Connect credit is kept.
- Version numbering restarted at 0.1.0 (`versionCode` 100).

### Fixed
- The outbound send queue is bounded at 64 packets, which prevents unbounded memory growth.

### Security
- `TransactionService` is no longer exported.
- URL-triggered run-command requests require confirmation.
- `FileProvider` is private.

**APK:** `PocketLink-0.1.0.apk` (universal, Android 6.0+)
