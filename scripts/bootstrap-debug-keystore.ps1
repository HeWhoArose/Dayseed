<#
Dayseed - local debug-keystore bootstrap (Windows / PowerShell).

Development infrastructure only. Both the `debug` and `release` signing configs
in app/build.gradle.kts point at ${rootDir}/debug.keystore, which is gitignored,
so a fresh checkout cannot assemble any APK until this file exists. This script
creates it with the identity the build already expects:

    alias      androiddebugkey
    store/key  android
    subject    CN=Android Debug,O=Android,C=US

This is NOT a production signing solution and must never be used as one. It
never overwrites an existing keystore and never handles real credentials.
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

# Native tools may write informational text to stderr - a JDK with JAVA_TOOL_OPTIONS
# set prints "Picked up JAVA_TOOL_OPTIONS: ..." on every invocation. Windows
# PowerShell promotes native stderr to a terminating error while
# $ErrorActionPreference = 'Stop', which aborted this script even when keytool had
# already succeeded. Native calls therefore run with 'Continue' and are judged by
# exit code alone.
function Invoke-Keytool {
    param([string[]]$KeytoolArgs)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & keytool @KeytoolArgs 2>&1 | Out-Null
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    return $exitCode
}

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    Fail 'keytool not found on PATH. Install a JDK (21 required by this project) and retry.'
}

if (Test-Path $keystore) {
    Write-Host "bootstrap-debug-keystore: $keystore already exists - refusing to overwrite."
    $existing = Invoke-Keytool @('-list', '-keystore', $keystore, '-storepass', $storePass, '-alias', $aliasName)
    if ($existing -ne 0) {
        Fail "the existing keystore does not contain the expected '$aliasName' entry (keytool exit $existing). Move it aside manually, then retry."
    }
    Write-Host 'bootstrap-debug-keystore: existing keystore verified; nothing to do.'
    exit 0
}

$created = Invoke-Keytool @(
    '-genkeypair', '-keystore', $keystore,
    '-storepass', $storePass, '-keypass', $keyPass,
    '-alias', $aliasName, '-keyalg', 'RSA', '-keysize', '2048',
    '-validity', '10000', '-dname', $dname
)
if ($created -ne 0) {
    Fail "keytool could not create $keystore (exit $created)."
}

$verified = Invoke-Keytool @('-list', '-keystore', $keystore, '-storepass', $storePass, '-alias', $aliasName)
if ($verified -ne 0) {
    Fail "$keystore was created but cannot be read back (exit $verified)."
}

Write-Host "bootstrap-debug-keystore: created $keystore (alias $aliasName, debug-only identity)."
Write-Host 'bootstrap-debug-keystore: next: .\gradlew.bat :app:assembleDebug'
