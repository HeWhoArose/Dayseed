#!/usr/bin/env bash
#
# Dayseed — local debug-keystore bootstrap (development infrastructure only).
#
# Both the `debug` and `release` signing configs in app/build.gradle.kts point at
# ${rootDir}/debug.keystore, which is gitignored. A fresh checkout therefore
# cannot assemble any APK until that file exists, so this script creates it with
# the identity the build already expects:
#
#   alias      androiddebugkey
#   store/key  android
#   subject    CN=Android Debug,O=Android,C=US
#
# This is NOT a production signing solution and must never be used as one. It
# never overwrites an existing keystore, and it never prints or stores any
# credential other than the fixed local debug values above.

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
keystore="${repo_root}/debug.keystore"
alias_name="androiddebugkey"
store_pass="android"
key_pass="android"
dname="CN=Android Debug,O=Android,C=US"

fail() {
  echo "bootstrap-debug-keystore: FAILED: $*" >&2
  exit 1
}

command -v keytool >/dev/null 2>&1 || fail "keytool not found on PATH. Install a JDK (21 required by this project) and retry."

if [[ -f "${keystore}" ]]; then
  echo "bootstrap-debug-keystore: ${keystore} already exists — refusing to overwrite."
  # Confirm it is usable rather than assuming; a corrupt file would fail later
  # in the build with a less obvious message.
  keytool -list -keystore "${keystore}" -storepass "${store_pass}" -alias "${alias_name}" >/dev/null 2>&1 \
    || fail "the existing keystore does not contain the expected '${alias_name}' entry. Move it aside manually, then retry."
  echo "bootstrap-debug-keystore: existing keystore verified; nothing to do."
  exit 0
fi

keytool -genkeypair \
  -keystore "${keystore}" \
  -storepass "${store_pass}" \
  -keypass "${key_pass}" \
  -alias "${alias_name}" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "${dname}" >/dev/null 2>&1 \
  || fail "keytool could not create ${keystore}."

keytool -list -keystore "${keystore}" -storepass "${store_pass}" -alias "${alias_name}" >/dev/null 2>&1 \
  || fail "${keystore} was created but cannot be read back."

echo "bootstrap-debug-keystore: created ${keystore} (alias ${alias_name}, debug-only identity)."
echo "bootstrap-debug-keystore: next: ./gradlew :app:assembleDebug"
