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

Push a version tag:

```bash
git tag v1.0.1
git push origin v1.0.1
```

The `Android Package` workflow will build and upload `muding-release-signed.apk` to the GitHub Release.
