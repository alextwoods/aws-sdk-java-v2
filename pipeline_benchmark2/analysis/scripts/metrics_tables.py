#!/usr/bin/env python3
"""Map each SDK's native metrics onto comparable pipeline phases and emit markdown tables.

Reads <rundir>/<client>_<scenario>/metrics.txt for every case. All values are avg microseconds
per operation. Phases without a native metric are printed as "-". The smithy transport value is
derived (attempt - serialize - sign - deserialize) and marked with *.
"""
import re
import sys
from pathlib import Path

CLIENTS = ["v1", "v2-sync", "v2-async", "smithy"]
SCENARIOS = ["small-get", "small-put", "batch-get", "batch-put"]

# phase -> client -> metric name (None = not available)
PHASES = [
    ("total call", {"v1": "ClientExecuteTime", "v2-sync": "ApiCallDuration",
                    "v2-async": "ApiCallDuration", "smithy": "smithy.client.call.duration"}),
    ("marshall", {"v1": "RequestMarshallTime", "v2-sync": "MarshallingDuration",
                  "v2-async": "MarshallingDuration", "smithy": "smithy.client.call.serialization_duration"}),
    ("sign", {"v1": "RequestSigningTime", "v2-sync": "SigningDuration",
              "v2-async": "SigningDuration", "smithy": "smithy.client.call.auth.signing_duration"}),
    ("endpoint resolve", {"v1": None, "v2-sync": "EndpointResolveDuration",
                          "v2-async": "EndpointResolveDuration",
                          "smithy": "smithy.client.call.resolve_endpoint_duration"}),
    ("credentials", {"v1": "CredentialsRequestTime", "v2-sync": "CredentialsFetchDuration",
                     "v2-async": "CredentialsFetchDuration",
                     "smithy": "smithy.client.call.auth.resolve_identity_duration"}),
    ("http transport", {"v1": "HttpRequestTime", "v2-sync": "ServiceCallDuration",
                        "v2-async": "ServiceCallDuration", "smithy": "@derived_transport"}),
    ("unmarshall", {"v1": "ResponseProcessingTime", "v2-sync": "UnmarshallingDuration",
                    "v2-async": "UnmarshallingDuration",
                    "smithy": "smithy.client.call.deserialization_duration"}),
]

METRIC_RE = re.compile(r"^METRIC (\S+)\s+count=[\d,]+ (?:totalMs=[\d,.]+ avgUs=([\d,.]+)|total=[\d,]+ avg=([\d,]+))")


def load_case(rundir, client, scenario):
    metrics = {}
    path = Path(rundir) / f"{client}_{scenario}" / "metrics.txt"
    for line in path.read_text().splitlines():
        m = METRIC_RE.match(line)
        if m:
            val = m.group(2) or m.group(3)
            metrics[m.group(1)] = float(val.replace(",", ""))
    return metrics


def smithy_derived_transport(m):
    # Serialization happens at call level, OUTSIDE attempt_duration (the call.duration -
    # attempt_duration gap matches serialization_duration across scenarios), so the attempt
    # decomposes as sign + transport + deserialize.
    return (m["smithy.client.call.attempt_duration"]
            - m["smithy.client.call.auth.signing_duration"]
            - m["smithy.client.call.deserialization_duration"])


def main(rundir):
    for scenario in SCENARIOS:
        data = {c: load_case(rundir, c, scenario) for c in CLIENTS}
        print(f"### {scenario} — per-op phase timings (avg us, SDK-native metrics)\n")
        print("| phase | " + " | ".join(CLIENTS) + " |")
        print("|-------|" + "----:|" * len(CLIENTS))
        for phase, mapping in PHASES:
            cells = []
            for client in CLIENTS:
                name = mapping[client]
                if name is None:
                    cells.append("-")
                elif name == "@derived_transport":
                    cells.append(f"{smithy_derived_transport(data[client]):.2f}*")
                else:
                    cells.append(f"{data[client].get(name, float('nan')):.2f}")
            print(f"| {phase} | " + " | ".join(cells) + " |")
        # unattributed = total - sum of phases (excl total; endpoint/creds tiny but included)
        cells = []
        for client in CLIENTS:
            m = data[client]
            total = None
            parts = 0.0
            for phase, mapping in PHASES:
                name = mapping[client]
                if name is None:
                    continue
                v = smithy_derived_transport(m) if name == "@derived_transport" else m.get(name, 0.0)
                if phase == "total call":
                    # V2's ApiCallDuration excludes marshalling; add it for a comparable total.
                    total = v + m["MarshallingDuration"] if client.startswith("v2") else v
                else:
                    parts += v
            cells.append(f"{total - parts:.2f}" if total is not None else "-")
        print("| unattributed (total - phases) | " + " | ".join(cells) + " |")
        # V2's ApiCallDuration timer starts AFTER marshalling (BaseClientHandler marshals before
        # invoking the pipeline that hosts ApiCallMetricCollectionStage), so a comparable
        # whole-call number for V2 is ApiCallDuration + MarshallingDuration.
        cells = []
        for client in CLIENTS:
            m = data[client]
            if client.startswith("v2"):
                cells.append(f"{m['ApiCallDuration'] + m['MarshallingDuration']:.2f}")
            elif client == "v1":
                cells.append(f"{m['ClientExecuteTime']:.2f}")
            else:
                cells.append(f"{m['smithy.client.call.duration']:.2f}")
        print("| total incl marshall (comparable) | " + " | ".join(cells) + " |")
        print()
    print("\\* smithy has no transport metric; derived as attempt - sign - deserialize"
          " (serialization is outside attempt_duration).\n")
    print("Note: V2 ApiCallDuration excludes marshalling (measured before the pipeline timer starts),")
    print("contrary to the CoreMetric javadoc; the last row adds it back for cross-SDK comparison.\n")


if __name__ == "__main__":
    main(sys.argv[1])
