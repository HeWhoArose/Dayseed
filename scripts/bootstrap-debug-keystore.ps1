<#
Dayseed — local debug-keystore bootstrap (Windows / PowerShell).

Development infrastructure only. Both the `debug` and `release` signing configs
in app/build.gradle.kts point at ${rootDir}/debug.keystore, which is gitignored,
so a fresh checkout cannot assemble any APK until this file exists. This script
creates it with the identity the build already expects:

    alias      androiddebugkey
    store/key  android
    subject    CN=Android Debug,O=Android,C=US

This is NOT a production signing solution and must never be used as one. It
never overwrites an existing keystore and never handles real credentials.

NOTE: this script has NOT been executed by the project's automated evidence
runs (the cloud Linux environment has no PowerShell). Treat it as unverified
until it has been run once on Windows and the result recorded.
#>

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $repoRoot 'debug.keystore'
$aliasName = 'androiddebugkey'
$storePass = 'android'
$keyPass = 'android'
$dname = 'CN=Android Debug,O=Android,C=US'

function Fail([string]$Message) {
    Write-Error "bootstrap-debug-keystore: FAILED: $Message"
    exit 1
}

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    Fail 'keytool not found on PATH. Install a JDK (21 required by this project) and retry.'
}

if (Test-Path $keystore) {
    Write-Host "bootstrap-debug-keystore: $keystore already exists - refusing to overwrite."
    & keytool -list -keystore $keystore -storepass $storePass -alias $aliasName 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Fail "the existing keystore does not contain the expected '$aliasName' entry. Move it aside manually, then retry."
    }
    Write-Host 'bootstrap-debug-keystore: existing keystore verified; nothing to do.'
    exit 0
}

& keytool -genkeypair `
    -keystore $keystore `
    -storepass $storePass `
    -keypass $keyPass `
    -alias $aliasName `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -dname $dname 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Fail "keytool could not create $keystore."
}

& keytool -list -keystore $keystore -storepass $storePass -alias $aliasName 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Fail "$keystore was created but cannot be read back."
}

Write-Host "bootstrap-debug-keystore: created $keystore (alias $aliasName, debug-only identity)."
Write-Host 'bootstrap-debug-keystore: next: .\gradlew.bat :app:assembleDebug'
