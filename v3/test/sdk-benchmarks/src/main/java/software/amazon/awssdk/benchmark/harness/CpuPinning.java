/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.harness;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Disjoint CPU pinning of the client and mock-service processes (Req 20.5).
 *
 * <p>On Linux with {@code taskset} the processes are launched under {@code taskset -c <cpus>}, on disjoint CPU sets,
 * and the kernel's effective affinity ({@code Cpus_allowed_list} in {@code /proc/<pid>/status}) is read back and
 * recorded. With no explicit sets, CPU 0 is left to the OS (when there are at least four CPUs) and the rest is split
 * in half, mock service first. Elsewhere, notably macOS, which has no process CPU affinity API (no
 * {@code taskset}/cpuset; Mach affinity tags are advisory hints), nothing is pinned and the reason is recorded.
 *
 * <p>In {@link HarnessMode#RELEASE} an unpinnable host is a policy violation: {@link #plan} throws
 * {@link HarnessPolicyException} instead of returning an unpinned plan.
 */
public final class CpuPinning {

    /** How a process is pinned. */
    public enum Mechanism {
        TASKSET,
        NONE
    }

    /**
     * One process's pinning: the mechanism and CPUs applied, or {@link Mechanism#NONE} with the reason.
     *
     * @param role {@code client} or {@code mock-service}
     */
    public record Assignment(String role, Mechanism mechanism, List<Integer> cpus, String reason) {
        public Assignment {
            cpus = List.copyOf(cpus);
        }

        public boolean applied() {
            return mechanism != Mechanism.NONE;
        }

        /** The argv prefix that applies the pinning, e.g. {@code [/usr/bin/taskset, -c, 4-7]}; empty if none. */
        public List<String> commandPrefix(String tasksetPath) {
            return applied() ? List.of(tasksetPath, "-c", formatCpuList(cpus)) : List.of();
        }
    }

    /** The client and mock-service assignments, and the {@code taskset} binary that applies them. */
    public record Plan(Assignment client, Assignment mockService, String tasksetPath) {
        public List<String> clientPrefix() {
            return client.commandPrefix(tasksetPath);
        }

        public List<String> mockServicePrefix() {
            return mockService.commandPrefix(tasksetPath);
        }
    }

    private CpuPinning() {
    }

    /**
     * Plans the pinning for this host.
     *
     * @param clientCpus explicit client CPUs, or empty for the default split
     * @param serviceCpus explicit mock-service CPUs, or empty for the default split
     * @param logicalCpus CPUs available to the harness
     */
    public static Plan plan(SystemProbe probe, HarnessMode mode, List<Integer> clientCpus, List<Integer> serviceCpus,
                            int logicalCpus) {
        String unavailable = null;
        Optional<Path> taskset = Optional.empty();
        if (!probe.isLinux()) {
            unavailable = probe.isMac()
                          ? "macOS has no process CPU affinity API (no taskset/cpuset; thread affinity tags are"
                            + " advisory), so client and mock service are scheduled freely on all cores"
                          : "no supported CPU pinning mechanism on " + probe.osName();
        } else {
            taskset = probe.which("taskset");
            if (taskset.isEmpty()) {
                unavailable = "taskset not found on PATH (install util-linux)";
            }
        }
        List<Integer> service = serviceCpus;
        List<Integer> client = clientCpus;
        if (unavailable == null) {
            if (client.isEmpty() != service.isEmpty()) {
                throw new IllegalArgumentException("give both --client-cpus and --service-cpus, or neither");
            }
            if (client.isEmpty()) {
                List<List<Integer>> split = defaultSplit(logicalCpus);
                if (split.isEmpty()) {
                    unavailable = logicalCpus + " CPU(s) cannot be split into disjoint client and mock-service sets";
                } else {
                    service = split.get(0);
                    client = split.get(1);
                }
            } else {
                Set<Integer> overlap = new HashSet<>(client);
                overlap.retainAll(service);
                if (!overlap.isEmpty()) {
                    throw new IllegalArgumentException("client and mock-service CPUs overlap: " + overlap);
                }
            }
        }
        if (unavailable != null) {
            if (mode == HarnessMode.RELEASE) {
                throw new HarnessPolicyException("release-gate collection requires disjoint pinned client and"
                                                 + " mock-service CPUs (Req 20.5), but " + unavailable);
            }
            return new Plan(new Assignment("client", Mechanism.NONE, List.of(), unavailable),
                            new Assignment("mock-service", Mechanism.NONE, List.of(), unavailable), "");
        }
        return new Plan(new Assignment("client", Mechanism.TASKSET, client, ""),
                        new Assignment("mock-service", Mechanism.TASKSET, service, ""),
                        taskset.get().toString());
    }

    /**
     * Mock service first half, client second half; CPU 0 is left out when there are at least four CPUs. Empty if
     * fewer than two CPUs.
     */
    static List<List<Integer>> defaultSplit(int logicalCpus) {
        if (logicalCpus < 2) {
            return List.of();
        }
        int first = logicalCpus >= 4 ? 1 : 0;
        int usable = logicalCpus - first;
        int half = usable / 2;
        List<Integer> service = new ArrayList<>();
        List<Integer> client = new ArrayList<>();
        for (int i = 0; i < usable; i++) {
            (i < half ? service : client).add(first + i);
        }
        return List.of(service, client);
    }

    /**
     * The kernel's effective CPU list of {@code pid} ({@code Cpus_allowed_list}), when the OS exposes it (Linux).
     */
    public static Optional<String> effectiveCpus(SystemProbe probe, long pid) {
        return probe.readFile(Path.of("/proc", Long.toString(pid), "status"))
                    .flatMap(status -> status.lines().filter(l -> l.startsWith("Cpus_allowed_list:")).findFirst())
                    .map(l -> l.substring("Cpus_allowed_list:".length()).trim());
    }

    /** Parses a Linux CPU list ({@code 0-3,6,8-9}). */
    public static List<Integer> parseCpuList(String list) {
        if (list == null || list.isBlank()) {
            return List.of();
        }
        TreeSet<Integer> cpus = new TreeSet<>();
        for (String part : list.split(",")) {
            String p = part.trim();
            int dash = p.indexOf('-');
            if (dash < 0) {
                cpus.add(nonNegative(p));
            } else {
                int lo = nonNegative(p.substring(0, dash));
                int hi = nonNegative(p.substring(dash + 1));
                if (hi < lo) {
                    throw new IllegalArgumentException("descending CPU range: " + p);
                }
                for (int c = lo; c <= hi; c++) {
                    cpus.add(c);
                }
            }
        }
        return List.copyOf(cpus);
    }

    private static int nonNegative(String s) {
        int v = Integer.parseInt(s.trim());
        if (v < 0) {
            throw new IllegalArgumentException("negative CPU: " + s);
        }
        return v;
    }

    /** Formats CPUs as a compact Linux CPU list, the inverse of {@link #parseCpuList}. */
    public static String formatCpuList(List<Integer> cpus) {
        List<Integer> sorted = new ArrayList<>(new TreeSet<>(cpus));
        Collections.sort(sorted);
        List<String> parts = new ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            int j = i;
            while (j + 1 < sorted.size() && sorted.get(j + 1) == sorted.get(j) + 1) {
                j++;
            }
            parts.add(i == j ? Integer.toString(sorted.get(i)) : sorted.get(i) + "-" + sorted.get(j));
            i = j + 1;
        }
        return parts.stream().collect(Collectors.joining(","));
    }
}
