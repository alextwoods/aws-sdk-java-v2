#!/usr/bin/env bash
# Starts the standalone mock DynamoDB server in the foreground.
# Usage: scripts/server.sh [--jar PATH] [--port N]
#
# With --jar, the server runs entirely from a shaded benchmark jar (no local build required),
# which is how it runs on a remote benchmark host.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

JAR=""
ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --jar) JAR="$2"; shift 2 ;;
        *)     ARGS+=("$1"); shift ;;
    esac
done

if [[ -n "$JAR" ]]; then
    if [[ ! -f "$JAR" ]]; then
        echo "error: --jar $JAR not found" >&2
        exit 2
    fi
    CP="$JAR"
else
    if [[ ! -f "$DIR/target/classpath.txt" || ! -d "$DIR/target/classes" ]]; then
        echo "Building (first run)..."
        (cd "$DIR" && mvn -q package)
    fi
    CP="$DIR/target/classes:$(cat "$DIR/target/classpath.txt")"
fi

exec java --enable-native-access=ALL-UNNAMED -cp "$CP" \
    software.amazon.awssdk.benchmark.e2e.MockDdbServer ${ARGS[@]+"${ARGS[@]}"}
