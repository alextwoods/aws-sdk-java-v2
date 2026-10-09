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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HostAndPinningTest {
    private static final String JAVA = "/jdk/bin/java";
    private static final String SETTINGS = String.join("\n",
        "Property settings:",
        "    java.home = /Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home",
        "    java.runtime.version = 21.0.8+9-LTS",
        "    java.vendor = Amazon.com Inc.",
        "    java.version = 21.0.8",
        "    java.vm.name = OpenJDK 64-Bit Server VM",
        "    java.vm.version = 21.0.8+9-LTS",
        "openjdk version \"21.0.8\" 2025-07-15 LTS");

    private static RecordedProbe mac() {
        return new RecordedProbe("mac os x")
            .command("bench-host\n", "hostname")
            .command("15.6\n", "sw_vers", "-productVersion")
            .command("24G84\n", "sw_vers", "-buildVersion")
            .command("Mac16,8\n", "sysctl", "-n", "hw.model")
            .command("Apple M4 Pro\n", "sysctl", "-n", "machdep.cpu.brand_string")
            .command("14\n", "sysctl", "-n", "hw.physicalcpu")
            .command("10\n", "sysctl", "-n", "hw.perflevel0.physicalcpu")
            .command("4\n", "sysctl", "-n", "hw.perflevel1.physicalcpu")
            .command("51539607552\n", "sysctl", "-n", "hw.memsize")
            .command("Now drawing from 'AC Power'\n -InternalBattery-0 (id=1)\t100%; charged; 0:00 remaining present: true\n",
                     "pmset", "-g", "batt")
            .command("System-wide power settings:\nCurrently in use:\n lowpowermode         0\n sleep                1\n"
                     + " displaysleep         10\n powermode            0\n", "pmset", "-g")
            .command("  1\n  2\n  3\n", "ps", "-A", "-o", "pid=")
            .command(" 12.5 /usr/bin/java\n  3.0 /System/Library/WindowServer\n", "ps", "-A", "-r", "-o", "%cpu=,comm=")
            .command(SETTINGS, JAVA, "-XshowSettings:properties", "-version");
    }

    private static RecordedProbe linux() {
        return new RecordedProbe("linux")
            .command("bench-linux\n", "hostname")
            .command("6.1.0-aws\n", "uname", "-r")
            .file("/etc/os-release", "NAME=\"Amazon Linux\"\nPRETTY_NAME=\"Amazon Linux 2023\"\n")
            .file("/proc/cpuinfo", "processor\t: 0\nmodel name\t: AMD EPYC 9R14\nphysical id\t: 0\ncore id\t: 0\n\n"
                                   + "processor\t: 1\nmodel name\t: AMD EPYC 9R14\nphysical id\t: 0\ncore id\t: 1\n\n"
                                   + "processor\t: 2\nmodel name\t: AMD EPYC 9R14\nphysical id\t: 0\ncore id\t: 1\n")
            .file("/proc/meminfo", "MemTotal:       16000000 kB\n")
            .file("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "performance\n")
            .file("/sys/devices/system/cpu/cpufreq/boost", "0\n")
            .command("  1\n  2\n", "ps", "-A", "-o", "pid=")
            .command(" 50.0 java\n", "ps", "-A", "--sort=-%cpu", "-o", "%cpu=,comm=")
            .command(SETTINGS, JAVA, "-XshowSettings:properties", "-version")
            .executable("taskset", "/usr/bin/taskset")
            .file("/proc/4242/status", "Name:\tjava\nCpus_allowed:\tf0\nCpus_allowed_list:\t4-7\n");
    }

    @Test
    void macMetadataIsParsedFromRecordedOutput() {
        HostMetadata m = HostMetadata.capture(mac(), JAVA, List.of("-Xmx2g"), "unmanaged");
        assertTrue(m.entries().keySet().containsAll(HostMetadata.REQUIRED_KEYS));
        assertEquals("bench-host", m.get("host.name"));
        assertEquals("macOS 15.6 (24G84)", m.get("os.release"));
        assertEquals("Apple M4 Pro", m.get("cpu.model"));
        assertEquals("14 (performance=10 efficiency=4)", m.get("cpu.physical"));
        assertEquals("51539607552", m.get("memory.bytes"));
        assertEquals("AC Power", m.get("power.source"));
        assertEquals("-InternalBattery-0 (id=1) 100%; charged; 0:00 remaining", m.get("power.state"));
        assertEquals("lowpowermode=0 sleep=1 displaysleep=10 powermode=0", m.get("power.settings"));
        assertEquals("Amazon.com Inc.", m.get("jdk.vendor"));
        assertEquals("21.0.8+9-LTS", m.get("jdk.version"));
        assertEquals("OpenJDK 64-Bit Server VM 21.0.8+9-LTS", m.get("jdk.vm"));
        assertEquals("-Xmx2g", m.get("jvm.flags"));
        assertEquals("3", m.get("background.process_count"));
        assertEquals("java(12.5%) WindowServer(3.0%)", m.get("background.top_cpu"));
        assertEquals("unmanaged", m.get("background.policy"));
        assertTrue(HostMetadata.isUnavailable(m.get("cpu.frequency_policy")));
    }

    @Test
    void linuxMetadataIsParsedAndMissingFactsSayWhy() {
        HostMetadata m = HostMetadata.capture(linux(), JAVA, List.of("-Xmx2g"), "isolated-runbook-1");
        assertTrue(m.entries().keySet().containsAll(HostMetadata.REQUIRED_KEYS));
        assertEquals("Amazon Linux 2023 kernel 6.1.0-aws", m.get("os.release"));
        assertEquals("AMD EPYC 9R14", m.get("cpu.model"));
        assertEquals("2", m.get("cpu.physical"));
        assertEquals("governor=performance", m.get("cpu.frequency_policy"));
        assertEquals(Long.toString(16000000L * 1024), m.get("memory.bytes"));
        assertEquals("cpufreq.boost=0", m.get("power.settings"));
        assertTrue(m.get("power.source").startsWith(HostMetadata.UNAVAILABLE), m.get("power.source"));
        assertTrue(HostMetadata.isUnavailable(m.get("host.model")));
        assertEquals("java(50.0%)", m.get("background.top_cpu"));
    }

    @Test
    void liveCaptureHasEveryKeyAndTheRunningJdk() {
        HostMetadata m = HostMetadata.capture(SystemProbe.live(), HarnessConfig.currentJava(), List.of(), "unmanaged");
        assertTrue(m.entries().keySet().containsAll(HostMetadata.REQUIRED_KEYS));
        assertEquals(System.getProperty("java.vendor"), m.get("jdk.vendor"));
        assertEquals(System.getProperty("java.runtime.version"), m.get("jdk.version"));
        assertFalse(HostMetadata.isUnavailable(m.get("os.name")));
        assertFalse(HostMetadata.isUnavailable(m.get("cpu.model")), m.get("cpu.model"));
    }

    @Test
    void macIsRecordedUnpinnedInExploratoryModeAndFailsClosedInReleaseMode() {
        CpuPinning.Plan plan = CpuPinning.plan(mac(), HarnessMode.EXPLORATORY, List.of(), List.of(), 14);
        assertFalse(plan.client().applied());
        assertFalse(plan.mockService().applied());
        assertTrue(plan.client().reason().startsWith("macOS has no process CPU affinity API"), plan.client().reason());
        assertEquals(List.of(), plan.clientPrefix());
        HarnessPolicyException e = assertThrows(HarnessPolicyException.class,
            () -> CpuPinning.plan(mac(), HarnessMode.RELEASE, List.of(), List.of(), 14));
        assertTrue(e.getMessage().contains("Req 20.5"), e.getMessage());
    }

    @Test
    void linuxPinsDisjointSetsWithTaskset() {
        CpuPinning.Plan plan = CpuPinning.plan(linux(), HarnessMode.RELEASE, List.of(), List.of(), 8);
        assertEquals(List.of(1, 2, 3), plan.mockService().cpus());
        assertEquals(List.of(4, 5, 6, 7), plan.client().cpus());
        assertEquals(List.of("/usr/bin/taskset", "-c", "4-7"), plan.clientPrefix());
        assertEquals(List.of("/usr/bin/taskset", "-c", "1-3"), plan.mockServicePrefix());
        Set<Integer> overlap = new HashSet<>(plan.client().cpus());
        overlap.retainAll(plan.mockService().cpus());
        assertTrue(overlap.isEmpty());

        CpuPinning.Plan explicit = CpuPinning.plan(linux(), HarnessMode.RELEASE, List.of(8, 9), List.of(2), 16);
        assertEquals(List.of("/usr/bin/taskset", "-c", "8-9"), explicit.clientPrefix());
        assertThrows(IllegalArgumentException.class,
            () -> CpuPinning.plan(linux(), HarnessMode.EXPLORATORY, List.of(1, 2), List.of(2), 16));
        assertThrows(IllegalArgumentException.class,
            () -> CpuPinning.plan(linux(), HarnessMode.EXPLORATORY, List.of(1, 2), List.of(), 16));
        assertEquals(Optional.of("4-7"), CpuPinning.effectiveCpus(linux(), 4242));
    }

    @Test
    void linuxWithoutTasksetOrWithOneCpuIsUnpinnedWithAReason() {
        RecordedProbe noTaskset = new RecordedProbe("linux");
        CpuPinning.Plan plan = CpuPinning.plan(noTaskset, HarnessMode.EXPLORATORY, List.of(), List.of(), 8);
        assertFalse(plan.client().applied());
        assertTrue(plan.client().reason().contains("taskset"));
        assertThrows(HarnessPolicyException.class,
            () -> CpuPinning.plan(noTaskset, HarnessMode.RELEASE, List.of(), List.of(), 8));
        CpuPinning.Plan single = CpuPinning.plan(linux(), HarnessMode.EXPLORATORY, List.of(), List.of(), 1);
        assertFalse(single.client().applied());
        assertNotNull(single.client().reason());
    }

    @Test
    void cpuListsRoundTrip() {
        assertEquals(List.of(0, 1, 2, 3, 6, 8, 9), CpuPinning.parseCpuList("0-3,6,8-9"));
        assertEquals("0-3,6,8-9", CpuPinning.formatCpuList(List.of(9, 8, 6, 3, 2, 1, 0)));
        assertEquals(List.of(), CpuPinning.parseCpuList(""));
        assertThrows(IllegalArgumentException.class, () -> CpuPinning.parseCpuList("3-1"));
        assertEquals(List.of(List.of(0), List.of(1)), CpuPinning.defaultSplit(2));
        assertEquals(List.of(List.of(1), List.of(2, 3)), CpuPinning.defaultSplit(4));
    }
}
