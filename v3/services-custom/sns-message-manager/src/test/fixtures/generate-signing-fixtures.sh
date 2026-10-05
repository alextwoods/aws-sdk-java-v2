#!/usr/bin/env bash
#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# Regenerates the checked-in, test-only SNS signing fixtures under
# src/test/resources/software/amazon/awssdk/messagemanager/sns/fixtures with the JDK keytool:
#
#   signing-key.p12   PKCS#12 key store (password "test-only") with one RSA-2048 key, alias "signing"
#   *.pem             self-signed X.509 certificates of that key, with fixed validity windows (starting at local
#                     midnight of the generating host), so the tests evaluate them at a fixed clock
#                     (2025-01-01T00:00:00Z) and never depend on the current date:
#     sns-global.pem      SAN dNSName sns.amazonaws.com, valid from 2020-01-01 for 7305 days (to 2040)
#     sns-signing-ap-east-1.pem
#                         SAN dNSName sns-signing.ap-east-1.amazonaws.com, valid from 2020-01-01 for 7305 days
#     cn-only.pem         no SAN; CN=sns-cn-north-1.amazonaws.com.cn, valid from 2020-01-01 for 7305 days
#     wildcard.pem        SAN dNSName *.amazonaws.com, valid from 2020-01-01 for 7305 days
#     wrong-name.pem      CN=sns.amazonaws.com but SAN dNSName sns.example.com (SANs win), valid from 2020-01-01
#     expired.pem         SAN dNSName sns.amazonaws.com, valid from 2000-01-01 for 365 days
#     not-yet-valid.pem   SAN dNSName sns.amazonaws.com, valid from 2090-01-01 for 365 days
#
# The key is a test fixture with no other use. Nothing is fetched from the network; the tests serve these
# certificates through an in-memory SdkSyncHttpClient.
set -euo pipefail

KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/}keytool"
OUT="$(cd "$(dirname "$0")/.." && pwd)/resources/software/amazon/awssdk/messagemanager/sns/fixtures"
STORE="$OUT/signing-key.p12"
PASS=test-only

mkdir -p "$OUT"
rm -f "$STORE" "$OUT"/*.pem

# The first certificate has no extensions: keytool -selfcert keeps the extensions of the certificate it replaces, so
# the certificate without a SAN must be exported before any certificate with one exists.
"$KEYTOOL" -genkeypair -keystore "$STORE" -storetype PKCS12 -storepass "$PASS" -keypass "$PASS" -alias signing \
    -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -dname "CN=sns-cn-north-1.amazonaws.com.cn, O=Test Fixture" \
    -startdate "2020/01/01 00:00:00" -validity 7305

# selfcert NAME DNAME STARTDATE DAYS [EXT]: re-signs the key's certificate and exports it as NAME.pem.
selfcert() {
    local name="$1" dname="$2" start="$3" days="$4" ext="${5:-}"
    local args=(-selfcert -keystore "$STORE" -storepass "$PASS" -alias signing -sigalg SHA256withRSA
                -dname "$dname" -startdate "$start" -validity "$days")
    if [[ -n "$ext" ]]; then
        args+=(-ext "$ext")
    fi
    "$KEYTOOL" "${args[@]}"
    export_pem "$name"
}

# export_pem NAME: the key's current certificate as NAME.pem, with LF line endings as SNS serves them (keytool -rfc
# wraps the Base64 body with CRLF, which the V2-compatible PEM format check rejects).
export_pem() {
    "$KEYTOOL" -exportcert -rfc -keystore "$STORE" -storepass "$PASS" -alias signing -file "$OUT/$1.pem.tmp"
    tr -d '\r' < "$OUT/$1.pem.tmp" > "$OUT/$1.pem"
    rm "$OUT/$1.pem.tmp"
}

export_pem cn-only

DN="CN=sns.amazonaws.com, O=Test Fixture"
selfcert expired "$DN" "2000/01/01 00:00:00" 365 SAN=dns:sns.amazonaws.com
selfcert not-yet-valid "$DN" "2090/01/01 00:00:00" 365 SAN=dns:sns.amazonaws.com
selfcert wrong-name "$DN" "2020/01/01 00:00:00" 7305 SAN=dns:sns.example.com
selfcert wildcard "CN=wildcard, O=Test Fixture" "2020/01/01 00:00:00" 7305 "SAN=dns:*.amazonaws.com"
selfcert sns-signing-ap-east-1 "CN=signing, O=Test Fixture" "2020/01/01 00:00:00" 7305 \
    SAN=dns:sns-signing.ap-east-1.amazonaws.com
# Last, so the key store's own certificate is the valid global one.
selfcert sns-global "$DN" "2020/01/01 00:00:00" 7305 SAN=dns:sns.amazonaws.com
