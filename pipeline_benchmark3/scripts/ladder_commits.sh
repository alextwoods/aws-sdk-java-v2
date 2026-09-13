#!/usr/bin/env bash
# For each adjacent pair of released tags, list the commits that touched the request path of a
# JSON-protocol client (core modules, codegen, apache clients), excluding the release/endpoint-data
# churn. Output is what the regression ladder's per-step deltas get attributed against.
#   usage: ladder_commits.sh [--stat] 2.46.10 2.47.0 2.48.0 ...
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
STAT=0; [[ ${1:-} == --stat ]] && { STAT=1; shift; }
PATHS=(core/sdk-core/src/main core/aws-core/src/main core/http-auth-aws/src/main core/http-auth-spi/src/main
       core/http-auth/src/main core/protocols/aws-json-protocol/src/main core/protocols/protocol-core/src/main
       core/json-utils/src/main core/regions/src/main utils/src/main utils-lite/src/main
       http-clients/apache5-client/src/main http-clients/apache-client/src/main http-clients/aws-crt-client/src/main
       codegen/src/main core/identity-spi/src/main core/auth/src/main core/checksums/src/main
       core/checksums-spi/src/main core/metrics-spi/src/main core/endpoints-spi/src/main core/retries/src/main)
prev=""
for v in "$@"; do
    if [[ -n $prev ]]; then
        echo "=== $prev -> $v"
        git -P log --format='%h %s' "$prev".."$v" -- "${PATHS[@]}" \
            | grep -v "Updated endpoints.json\|Release 2\.\|Update to next snapshot\|Bump versions\|Updated partitions"
        if [[ $STAT -eq 1 ]]; then
            git -P diff --stat "$prev" "$v" -- "${PATHS[@]}" | tail -1
        fi
    fi
    prev=$v
done
