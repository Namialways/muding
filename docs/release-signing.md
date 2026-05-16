# Release Signing

GitHub Actions signs release APKs from repository secrets. Do not commit the keystore or passwords.

## 1. Create A Keystore

Use Android Studio's **Build > Generate Signed Bundle / APK** wizard, or run `keytool` locally:

```bash
keytool -genkeypair -v \
  -keystore muding-release.jks \
  -alias muding \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Keep `muding-release.jks` and the passwords somewhere private.

## 2. Convert The Keystore To Base64

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("muding-release.jks")) | Set-Clipboard
```

## 3. Add GitHub Secrets

Add these in **GitHub repository > Settings > Secrets and variables > Actions > New repository secret**:

- `ANDROID_RELEASE_KEYSTORE_BASE64`: the base64 text from the keystore file
- `ANDROID_RELEASE_STORE_PASSWORD`: keystore password
- `ANDROID_RELEASE_KEY_ALIAS`: key alias, for example `muding`
- `ANDROID_RELEASE_KEY_PASSWORD`: key password

## 4. Create A Release

Recommended: open the **Android Package** workflow in GitHub Actions, click **Run workflow**, and enter the next version such as `1.1.5`. The workflow will:

- build with `versionName = 1.1.5`
- derive `versionCode` from the version, for example `1.1.5` becomes `1001005`
- create the `v1.1.5` tag if it does not already exist
- create or update the GitHub Release
- upload the signed ABI APKs

You can still use the tag-based release flow if you prefer to create tags locally:

```bash
git tag -a v1.0.1 -m "v1.0.1"
git push github v1.0.1
```

When a `v*` tag is pushed, the same `Android Package` workflow derives `versionName` from the tag and builds the release from that tagged commit.

The workflow requires signing secrets, builds only signed release APKs, and uploads the GitHub Release assets as:

- `muding-arm64-v8a.apk`: recommended for most modern Android phones
- `muding-armeabi-v7a.apk`: compatibility build for older 32-bit Android devices
