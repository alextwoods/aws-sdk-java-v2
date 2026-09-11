#!/usr/bin/env bash
# Do the bridged and stock pipelines return the same thing? Take the same response digest from each
# benchmark jar against one canned server and diff them as text.
set -uo pipefail
cd /Users/alexwoo/java/java2-repo2/test/standalone-e2e-benchmarks
JARS=../../pipeline_benchmark2/jars
PORT=19093
OUT=$(mktemp -d)
trap 'kill ${SRV:-0} 2>/dev/null; rm -rf "$OUT"' EXIT

RACECAR_JAR=$JARS/racecar-bench3-racecar-ac977512cbe.jar

# One server for every arm, so the bytes on the wire are identical by construction.
java -cp "$RACECAR_JAR" software.amazon.awssdk.benchmark.e2e.MockDdbServer --port $PORT \
    > "$OUT/server.log" 2>&1 &
SRV=$!
for i in $(seq 1 60); do grep -q READY "$OUT/server.log" && break; sleep 0.25; done
grep -q READY "$OUT/server.log" || { cat "$OUT/server.log"; exit 1; }

digest() { # label jar
    java -cp "$2" software.amazon.awssdk.benchmark.e2e.ResponseDigest "http://127.0.0.1:$PORT" \
        > "$OUT/$1.txt" 2>"$OUT/$1.err"
    printf "%-9s %s lines\n" "$1" "$(wc -l < "$OUT/$1.txt" | tr -d ' ')"
    if [[ ! -s "$OUT/$1.txt" ]]; then
        echo "  !! empty digest; stderr tail:"; tail -5 "$OUT/$1.err"
    fi
}

digest racecar  "$RACECAR_JAR"
digest bridged  "$JARS/racecar-bench3-bridged-c1d88972e18.jar"
digest stock246 "$JARS/racecar-bench3-stock246-published-2.46.10.jar"
digest stock254 "$JARS/racecar-bench3-stock254-published-2.54.0.jar"

echo
for label in bridged stock246 stock254; do
    if diff -q "$OUT/racecar.txt" "$OUT/$label.txt" >/dev/null 2>&1; then
        echo "IDENTICAL  racecar vs $label"
    else
        echo "DIFFERENT  racecar vs $label:"
        diff "$OUT/racecar.txt" "$OUT/$label.txt" | head -24
    fi
done
