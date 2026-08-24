#!/usr/bin/env bash
# Starts the standalone mock DynamoDB server in the foreground.
# Usage: scripts/server.sh [--port N]
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ ! -f "$DIR/target/classpath.txt" || ! -d "$DIR/target/classes" ]]; then
    echo "Building (first run)..."
    (cd "$DIR" && mvn -q package)
fi

CP="$DIR/target/classes:$(cat "$DIR/target/classpath.txt")"
exec java -cp "$CP" software.amazon.awssdk.benchmark.e2e.MockDdbServer "$@"
