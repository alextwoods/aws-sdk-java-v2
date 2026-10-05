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

package software.amazon.awssdk.benchmark.dynamodb;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Binds a {@link CpuTimeSource}, ported from racecar's {@code CpuTimeSources}.
 *
 * <p>Deviation: racecar's default source is OSHI, which gives a user/system split on macOS. OSHI is a third-party
 * dependency that V3 does not carry, so this port has only {@code procfs} (Linux, split) and {@code mxbean}
 * ({@code OperatingSystemMXBean.getProcessCpuTime()}, total only). On macOS {@code auto} therefore binds
 * {@code mxbean} and the {@code cpu_user_ms}/{@code cpu_sys_ms} columns stay empty. The headline metric,
 * {@code app_cpu_us_per_op}, comes from {@link AppCpuMeter} and is unaffected.
 */
final class CpuTimeSources {

    private CpuTimeSources() {
    }

    static CpuTimeSource bind(String preference) {
        return switch (preference) {
            case "auto" -> {
                CpuTimeSource procfs = ProcfsSource.tryCreate();
                yield procfs != null ? procfs : require(MxBeanSource.tryCreate(), "mxbean");
            }
            case "procfs" -> require(ProcfsSource.tryCreate(), "procfs");
            case "mxbean" -> require(MxBeanSource.tryCreate(), "mxbean");
            default -> throw new IllegalArgumentException("unknown cpu source: " + preference + " (auto|procfs|mxbean)");
        };
    }

    private static CpuTimeSource require(CpuTimeSource source, String name) {
        if (source == null) {
            throw new IllegalStateException("cpu source '" + name + "' is not available on this platform");
        }
        return source;
    }

    /** {@code /proc/self/stat} utime/stime (Linux only). */
    private static final class ProcfsSource implements CpuTimeSource {
        private static final Path PROC_SELF_STAT = Path.of("/proc/self/stat");
        private final long ticksPerSec = Long.getLong("clkTck", 100);

        static ProcfsSource tryCreate() {
            try {
                ProcfsSource source = new ProcfsSource();
                source.snapshot();
                return source;
            } catch (Exception | Error e) {
                return null;
            }
        }

        @Override
        public String name() {
            return "/proc/self/stat (ticks=" + ticksPerSec + "/s)";
        }

        @Override
        public boolean hasUserSystemSplit() {
            return true;
        }

        @Override
        public Snapshot snapshot() {
            try {
                String stat = new String(Files.readAllBytes(PROC_SELF_STAT), StandardCharsets.US_ASCII);
                // Fields after the parenthesised command name; utime and stime are fields 14 and 15 (1-based).
                String[] fields = stat.substring(stat.lastIndexOf(')') + 2).split(" ");
                long nanosPerTick = 1_000_000_000L / ticksPerSec;
                return Snapshot.ofSplit(Long.parseLong(fields[11]) * nanosPerTick,
                                        Long.parseLong(fields[12]) * nanosPerTick);
            } catch (Exception e) {
                throw new IllegalStateException("cannot read " + PROC_SELF_STAT, e);
            }
        }
    }

    /** {@code OperatingSystemMXBean.getProcessCpuTime()}: total only. */
    private static final class MxBeanSource implements CpuTimeSource {
        private final com.sun.management.OperatingSystemMXBean os;

        private MxBeanSource(com.sun.management.OperatingSystemMXBean os) {
            this.os = os;
        }

        static MxBeanSource tryCreate() {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os
                && os.getProcessCpuTime() >= 0) {
                return new MxBeanSource(os);
            }
            return null;
        }

        @Override
        public String name() {
            return "mxbean (OperatingSystemMXBean.getProcessCpuTime)";
        }

        @Override
        public boolean hasUserSystemSplit() {
            return false;
        }

        @Override
        public Snapshot snapshot() {
            return Snapshot.ofTotal(os.getProcessCpuTime());
        }
    }
}
