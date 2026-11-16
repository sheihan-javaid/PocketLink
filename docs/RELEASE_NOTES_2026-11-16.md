# Release Notes - 2026-11-16

### Settings & Migration
- **Scoped Settings Storage**: Settings scope moved from app-wide to per-device and per-plugin (`<deviceId>_<Plugin>_preferences` / `<Plugin>_preferences`).
- **Downgrade Behavior**: Downgrading the app resets migrated settings to default values.
- **Downgrade + Re-upgrade Handling**: After a downgrade followed by a re-upgrade, values changed in between may be lost (the existing per-device value takes precedence).
- **First-Device Pairing Behavior**: A device paired after an upgrade when no paired devices exist inherits the old global settings; subsequent devices start with defaults.

### Reliability & Networking
- **Bounded Send Queue**: Outbound send queue is bounded at 64 packets. When the buffer is full, packets fail fast with an `IOException` to prevent unbounded memory growth and coroutine leaks.

### Security
- **TransactionService Non-Exported**: `TransactionService` is no longer exported in the Android Manifest (`android:exported="false"`) and does not accept external intents.
- **URL-Triggered Command Confirmation**: URL-triggered commands now ask for confirmation (device and command name shown).

### User Interface
- **Plugin Settings Access**: Plugin settings now open directly from a settings gear icon on the modern dashboard; the legacy "Plugins" menu item has been removed.
