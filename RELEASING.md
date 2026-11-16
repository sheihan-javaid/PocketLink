# Releasing PocketLink

PocketLink is distributed **only** as a signed APK attached to
[GitHub Releases](https://github.com/sheihan-javaid/PocketLink/releases).
There is no Google Play or F-Droid build. The `fastlane/` metadata is inherited from
KDE Connect and is **not used** by this release process.

Every release ships one **universal APK** (all ABIs). The app bundles only about 37 KB
of native code across all ABIs, so per-ABI split APKs would not save meaningful space.

---

## 1. One-time setup: the release keystore

> [!WARNING]
> **Back up the keystore and its passwords before you publish anything.**
> Android only installs an update over an existing app when the update is signed
> with the *same* key. If the key is lost or leaked, existing users can **never**
> upgrade in place again. They would have to uninstall PocketLink, which deletes its
> data and pairings, and install a build signed with a new key.
> Keep at least two offline copies, for example a password manager plus an
> encrypted USB drive.

Create the keystore **outside the repository**:

```sh
keytool -genkeypair -v \
  -keystore ~/keys/pocketlink-release.jks \
  -storetype PKCS12 \
  -alias pocketlink \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -dname "CN=PocketLink"
```

`keytool` prompts for the store password. With PKCS12, the key password is the same
as the store password.

Never commit the keystore or passwords. `.gitignore` already excludes `*.jks`,
`*.keystore`, `signing.properties`, `.env*` and `local.properties`.

## 2. Signing configuration

`build.gradle.kts` reads four values. Environment variables take precedence over
`local.properties`:

| Environment variable           | `local.properties` key          | Meaning                        |
|--------------------------------|---------------------------------|--------------------------------|
| `POCKETLINK_KEYSTORE_PATH`     | `pocketlink.keystore.path`      | Absolute path to the `.jks`    |
| `POCKETLINK_KEYSTORE_PASSWORD` | `pocketlink.keystore.password`  | Keystore password              |
| `POCKETLINK_KEY_ALIAS`         | `pocketlink.key.alias`          | Key alias (e.g. `pocketlink`)  |
| `POCKETLINK_KEY_PASSWORD`      | `pocketlink.key.password`       | Key password                   |

If any value is missing, or the keystore file does not exist, the build **does not
fail**. It prints a warning and produces an *unsigned* APK named
`PocketLink-<version>-unsigned.apk`. Debug builds and CI without secrets keep working.
An unsigned APK cannot be installed, so never publish one.

Example using environment variables, which keeps the passwords out of files:

```sh
export POCKETLINK_KEYSTORE_PATH="$HOME/keys/pocketlink-release.jks"
export POCKETLINK_KEY_ALIAS=pocketlink
read -rs POCKETLINK_KEYSTORE_PASSWORD && export POCKETLINK_KEYSTORE_PASSWORD
export POCKETLINK_KEY_PASSWORD="$POCKETLINK_KEYSTORE_PASSWORD"
```

## 3. Bump the version

Both values are in `defaultConfig` in `build.gradle.kts`:

```kotlin
versionCode = 100
versionName = "0.1.0"
```

- `versionName` is the user-visible version and the APK/tag name (`v0.1.0`).
  It follows semantic versioning, `MAJOR.MINOR.PATCH`.
- `versionCode` **must increase with every release**, or Android refuses the update.
  The formula is `MAJOR * 10000 + MINOR * 100 + PATCH`, so `0.1.0` gives `100`,
  `0.1.1` gives `101` and `1.0.0` gives `10000`. Keep `MINOR` and `PATCH` below 100.
  Upstream KDE Connect used 100 for 0.1.0, so a phone that still has a pre-0.1.0
  PocketLink build must uninstall it first (Android refuses version-code downgrades).

Add an entry to `CHANGELOG.md` in the same commit.

## 4. Build, verify, checksum (manual release)

```sh
./gradlew testDebugUnitTest lintDebug
./gradlew clean assembleRelease
```

Outputs in `build/outputs/apk/release/`:

| File                               | Purpose                                 |
|------------------------------------|-----------------------------------------|
| `PocketLink-<version>.apk`         | Signed universal APK, the file to upload |
| `PocketLink-<version>.apk.sha256`  | SHA-256 checksum (`sha256sum` format)    |
| `mapping.txt` (in `build/outputs/mapping/release/`) | R8 mapping. Keep it privately to de-obfuscate crash traces |

Verify the signature and print the certificate fingerprint:

```sh
APKSIGNER="$ANDROID_HOME/build-tools/37.0.0/apksigner"   # macOS default SDK: ~/Library/Android/sdk
"$APKSIGNER" verify --verbose --print-certs build/outputs/apk/release/PocketLink-<version>.apk
```

Expect `Verifies`, with `Verified using v1 scheme`, `v2 scheme` and `v3 scheme` all
`true`. The `Signer #1 certificate SHA-256 digest` must be the same in every release.
Publish it in the release notes so users can check it.

Verify the checksum:

```sh
cd build/outputs/apk/release
sha256sum -c PocketLink-<version>.apk.sha256        # Linux
shasum -a 256 -c PocketLink-<version>.apk.sha256    # macOS
```

Then publish:

```sh
git tag -a v<version> -m "PocketLink <version>"
git push origin v<version>
gh release create v<version> \
  build/outputs/apk/release/PocketLink-<version>.apk \
  build/outputs/apk/release/PocketLink-<version>.apk.sha256 \
  --title "PocketLink <version>" --notes-file <notes.md>
```

## 5. Automated release (GitHub Actions)

`.github/workflows/release.yml` runs when a `v*` tag is pushed. It:

1. checks that the tag matches `versionName` (tag `v0.1.0` needs `versionName = "0.1.0"`),
2. runs the unit tests,
3. decodes the keystore from secrets and builds the signed release APK,
4. runs `apksigner verify` and checks the SHA-256 file,
5. creates a **draft pre-release** on GitHub with the APK and checksum attached.

You review the draft, paste the notes from `CHANGELOG.md`, and click **Publish**. While the major version is 0, it stays marked as a pre-release.

Add these repository secrets under *Settings → Secrets and variables → Actions*:

| Secret                         | Value                                              |
|--------------------------------|----------------------------------------------------|
| `POCKETLINK_KEYSTORE_BASE64`   | `base64 -i ~/keys/pocketlink-release.jks` (one line) |
| `POCKETLINK_KEYSTORE_PASSWORD` | Keystore password                                  |
| `POCKETLINK_KEY_ALIAS`         | Key alias                                          |
| `POCKETLINK_KEY_PASSWORD`      | Key password                                       |

On macOS, `base64 -i file | pbcopy` copies the value to the clipboard. On Linux, use
`base64 -w0 file`.

## Release checklist

- [ ] `versionCode` increased and `versionName` bumped
- [ ] `CHANGELOG.md` updated
- [ ] Tests and lint pass
- [ ] `apksigner verify` passes with the usual certificate digest
- [ ] Checksum verifies
- [ ] Tag pushed, release published with the APK and `.sha256`
