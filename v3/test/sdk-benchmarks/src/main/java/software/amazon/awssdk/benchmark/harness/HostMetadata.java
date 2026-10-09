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

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Host, OS, CPU, memory, JDK, JVM-flag, power and background-load metadata of a run (Req 20.5, 20.10).
 *
 * <p>Every key in {@link #REQUIRED_KEYS} is always present. A fact the host does not expose is recorded as
 * {@code unavailable: <reason>} rather than omitted, so a report can tell "not detectable here" from "not captured".
 * The JDK facts describe the {@code java} executable the forked processes run, which the harness queries with
 * {@code -XshowSettings:properties}; each forked process also self-reports its JDK and input arguments in its records.
 */
public record HostMetadata(Map<String, String> entries) {

    public static final String UNAVAILABLE = "unavailable: ";

    /** The keys every capture contains. */
    public static final List<String> REQUIRED_KEYS = List.of(
        "host.name", "host.model", "os.name", "os.version", "os.arch", "os.release", "cpu.model", "cpu.logical",
        "cpu.physical", "cpu.frequency_policy", "memory.bytes", "jdk.executable", "jdk.vendor", "jdk.version",
        "jdk.vm", "jdk.home", "jvm.flags", "power.source", "power.state", "power.settings", "background.load_average",
        "background.process_count", "background.top_cpu", "background.policy");

    private static final List<String> LINUX_MAINS = List.of("AC", "AC0", "AC1", "ADP0", "ADP1", "ACAD");
    private static final List<String> LINUX_BATTERIES = List.of("BAT0", "BAT1");

    public HostMetadata {
        entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public String get(String key) {
        return entries.get(key);
    }

    public static boolean isUnavailable(String value) {
        return value == null || value.startsWith(UNAVAILABLE);
    }

    /**
     * Captures the metadata of this host.
     *
     * @param javaExecutable the {@code java} the forked processes run
     * @param jvmFlags the JVM flags given identically to client and mock service
     * @param backgroundPolicy the declared background-service policy of the run
     */
    public static HostMetadata capture(SystemProbe probe, String javaExecutable, List<String> jvmFlags,
                                       String backgroundPolicy) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("host.name", first(probe.run(List.of("hostname")), "hostname failed"));
        m.put("os.name", System.getProperty("os.name"));
        m.put("os.version", System.getProperty("os.version"));
        m.put("os.arch", System.getProperty("os.arch"));
        if (probe.isMac()) {
            captureMac(probe, m);
        } else if (probe.isLinux()) {
            captureLinux(probe, m);
        } else {
            for (String key : REQUIRED_KEYS) {
                m.putIfAbsent(key, UNAVAILABLE + "unsupported OS " + probe.osName());
            }
        }
        m.put("cpu.logical", Integer.toString(Runtime.getRuntime().availableProcessors()));
        captureJdk(probe, javaExecutable, m);
        m.put("jvm.flags", String.join(" ", jvmFlags));
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        m.put("background.load_average", load < 0 ? UNAVAILABLE + "no load average on this OS"
                                                  : String.format(Locale.ROOT, "%.2f", load));
        m.put("background.process_count", probe.run(List.of("ps", "-A", "-o", "pid="))
                                               .map(o -> Long.toString(o.lines().filter(l -> !l.isBlank()).count()))
                                               .orElse(UNAVAILABLE + "ps failed"));
        List<String> topCommand = probe.isMac() ? List.of("ps", "-A", "-r", "-o", "%cpu=,comm=")
                                                : List.of("ps", "-A", "--sort=-%cpu", "-o", "%cpu=,comm=");
        m.put("background.top_cpu", probe.run(topCommand).map(HostMetadata::topProcesses)
                                         .orElse(UNAVAILABLE + "ps failed"));
        m.put("background.policy", backgroundPolicy);
        for (String key : REQUIRED_KEYS) {
            m.putIfAbsent(key, UNAVAILABLE + "not exposed by this host");
        }
        return new HostMetadata(m);
    }

    private static void captureMac(SystemProbe probe, Map<String, String> m) {
        m.put("os.release", probe.run(List.of("sw_vers", "-productVersion")).map(String::trim)
                                 .map(v -> "macOS " + v + probe.run(List.of("sw_vers", "-buildVersion"))
                                                              .map(b -> " (" + b.trim() + ")").orElse(""))
                                 .orElse(UNAVAILABLE + "sw_vers failed"));
        m.put("host.model", sysctl(probe, "hw.model"));
        m.put("cpu.model", sysctl(probe, "machdep.cpu.brand_string"));
        String physical = sysctl(probe, "hw.physicalcpu");
        Optional<String> perf = probe.run(List.of("sysctl", "-n", "hw.perflevel0.physicalcpu")).map(String::trim);
        Optional<String> eff = probe.run(List.of("sysctl", "-n", "hw.perflevel1.physicalcpu")).map(String::trim);
        m.put("cpu.physical", physical + (perf.isPresent() && eff.isPresent()
                                          ? " (performance=" + perf.get() + " efficiency=" + eff.get() + ")" : ""));
        m.put("cpu.frequency_policy", UNAVAILABLE + "macOS exposes no frequency governor");
        m.put("memory.bytes", sysctl(probe, "hw.memsize"));
        Optional<String> batt = probe.run(List.of("pmset", "-g", "batt"));
        m.put("power.source", batt.flatMap(HostMetadata::macPowerSource).orElse(UNAVAILABLE + "pmset -g batt failed"));
        m.put("power.state", batt.map(HostMetadata::macBatteryState).orElse(UNAVAILABLE + "pmset -g batt failed"));
        m.put("power.settings", probe.run(List.of("pmset", "-g")).map(HostMetadata::macPowerSettings)
                                     .orElse(UNAVAILABLE + "pmset -g failed"));
    }

    private static void captureLinux(SystemProbe probe, Map<String, String> m) {
        m.put("os.release", probe.readFile(Path.of("/etc/os-release"))
                                 .flatMap(s -> keyValue(s, "PRETTY_NAME"))
                                 .map(v -> v + probe.run(List.of("uname", "-r")).map(r -> " kernel " + r.trim())
                                                     .orElse(""))
                                 .orElse(UNAVAILABLE + "no /etc/os-release"));
        m.put("host.model", probe.readFile(Path.of("/sys/devices/virtual/dmi/id/product_name")).map(String::trim)
                                 .filter(s -> !s.isEmpty()).orElse(UNAVAILABLE + "no DMI product name"));
        Optional<String> cpuinfo = probe.readFile(Path.of("/proc/cpuinfo"));
        m.put("cpu.model", cpuinfo.flatMap(c -> c.lines().filter(l -> l.startsWith("model name")).findFirst())
                                  .map(l -> l.substring(l.indexOf(':') + 1).trim())
                                  .orElse(UNAVAILABLE + "no model name in /proc/cpuinfo"));
        m.put("cpu.physical", cpuinfo.map(HostMetadata::linuxPhysicalCores).filter(n -> n > 0).map(String::valueOf)
                                     .orElse(UNAVAILABLE + "no core ids in /proc/cpuinfo"));
        String governor = probe.readFile(Path.of("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"))
                               .map(String::trim).orElse(null);
        m.put("cpu.frequency_policy", governor != null ? "governor=" + governor
                                                       : UNAVAILABLE + "no cpufreq governor (common in VMs)");
        m.put("memory.bytes", probe.readFile(Path.of("/proc/meminfo"))
                                   .flatMap(s -> s.lines().filter(l -> l.startsWith("MemTotal:")).findFirst())
                                   .map(l -> Long.toString(Long.parseLong(l.replaceAll("[^0-9]", "")) * 1024))
                                   .orElse(UNAVAILABLE + "no /proc/meminfo"));
        String source = null;
        for (String name : LINUX_MAINS) {
            Optional<String> online = probe.readFile(Path.of("/sys/class/power_supply", name, "online"));
            if (online.isPresent()) {
                source = "1".equals(online.get().trim()) ? "AC Power" : "Battery Power";
                break;
            }
        }
        m.put("power.source", source != null ? source
                                             : UNAVAILABLE + "no mains power_supply in sysfs (typical of servers/VMs)");
        String battery = null;
        for (String name : LINUX_BATTERIES) {
            Optional<String> capacity = probe.readFile(Path.of("/sys/class/power_supply", name, "capacity"));
            if (capacity.isPresent()) {
                battery = name + " " + capacity.get().trim() + "% "
                          + probe.readFile(Path.of("/sys/class/power_supply", name, "status")).map(String::trim)
                                 .orElse("");
                break;
            }
        }
        m.put("power.state", battery != null ? battery.trim() : UNAVAILABLE + "no battery in sysfs");
        StringBuilder settings = new StringBuilder();
        probe.readFile(Path.of("/sys/devices/system/cpu/intel_pstate/no_turbo"))
             .ifPresent(v -> settings.append("intel_pstate.no_turbo=").append(v.trim()).append(' '));
        probe.readFile(Path.of("/sys/devices/system/cpu/cpufreq/boost"))
             .ifPresent(v -> settings.append("cpufreq.boost=").append(v.trim()).append(' '));
        probe.readFile(Path.of("/sys/firmware/acpi/platform_profile"))
             .ifPresent(v -> settings.append("platform_profile=").append(v.trim()).append(' '));
        m.put("power.settings", settings.length() > 0 ? settings.toString().trim()
                                                      : UNAVAILABLE + "no turbo/boost/platform profile in sysfs");
    }

    private static void captureJdk(SystemProbe probe, String javaExecutable, Map<String, String> m) {
        m.put("jdk.executable", javaExecutable);
        Optional<String> settings = probe.run(List.of(javaExecutable, "-XshowSettings:properties", "-version"));
        String why = UNAVAILABLE + javaExecutable + " -XshowSettings:properties failed";
        m.put("jdk.vendor", settings.flatMap(s -> javaProperty(s, "java.vendor")).orElse(why));
        m.put("jdk.version", settings.flatMap(s -> javaProperty(s, "java.runtime.version")
                                                       .or(() -> javaProperty(s, "java.version"))).orElse(why));
        m.put("jdk.vm", settings.flatMap(s -> javaProperty(s, "java.vm.name")
                                              .map(n -> n + javaProperty(s, "java.vm.version").map(v -> " " + v)
                                                                                        .orElse(""))).orElse(why));
        m.put("jdk.home", settings.flatMap(s -> javaProperty(s, "java.home")).orElse(why));
    }

    private static String sysctl(SystemProbe probe, String name) {
        return first(probe.run(List.of("sysctl", "-n", name)), "sysctl " + name + " failed");
    }

    private static String first(Optional<String> output, String reason) {
        return output.map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.lines().findFirst().orElse(s))
                     .orElse(UNAVAILABLE + reason);
    }

    /** {@code key = value} lines of {@code -XshowSettings:properties}. */
    static Optional<String> javaProperty(String settings, String key) {
        String prefix = key + " = ";
        return settings.lines().map(String::trim).filter(l -> l.startsWith(prefix)).findFirst()
                       .map(l -> l.substring(prefix.length()).trim());
    }

    static Optional<String> keyValue(String text, String key) {
        return text.lines().filter(l -> l.startsWith(key + "=")).findFirst()
                   .map(l -> l.substring(key.length() + 1).trim().replace("\"", ""));
    }

    /** {@code Now drawing from 'AC Power'} -> {@code AC Power}. */
    static Optional<String> macPowerSource(String pmsetBatt) {
        return pmsetBatt.lines().filter(l -> l.contains("drawing from")).findFirst()
                        .map(l -> l.substring(l.indexOf("drawing from") + "drawing from".length()).trim()
                                   .replace("'", ""));
    }

    /** The battery line of {@code pmset -g batt}, or "no battery". */
    static String macBatteryState(String pmsetBatt) {
        return pmsetBatt.lines().filter(l -> l.contains("%")).findFirst()
                        .map(l -> l.replaceAll("\\s*present: true", "").replaceAll("\\s+", " ").trim())
                        .orElse("no battery");
    }

    /** {@code lowpowermode}, {@code powermode}, {@code sleep} and {@code displaysleep} from {@code pmset -g}. */
    static String macPowerSettings(String pmset) {
        Set<String> wanted = Set.of("lowpowermode", "powermode", "sleep", "displaysleep", "highpowermode");
        String found = pmset.lines().map(String::trim).map(l -> l.split("\\s+"))
                            .filter(t -> t.length >= 2 && wanted.contains(t[0]))
                            .map(t -> t[0] + "=" + t[1]).collect(Collectors.joining(" "));
        return found.isEmpty() ? UNAVAILABLE + "no power mode settings in pmset -g" : found;
    }

    static long linuxPhysicalCores(String cpuinfo) {
        Set<String> cores = new TreeSet<>();
        String physicalId = "0";
        for (String line : cpuinfo.lines().toList()) {
            if (line.startsWith("physical id")) {
                physicalId = line.substring(line.indexOf(':') + 1).trim();
            } else if (line.startsWith("core id")) {
                cores.add(physicalId + ":" + line.substring(line.indexOf(':') + 1).trim());
            }
        }
        return cores.size();
    }

    /** The five busiest processes of a {@code %cpu comm} listing already sorted by CPU. */
    static String topProcesses(String ps) {
        return ps.lines().map(String::trim).filter(l -> !l.isEmpty()).limit(5).map(l -> {
            String[] t = l.split("\\s+", 2);
            String command = t.length > 1 ? t[1] : "?";
            command = command.substring(command.lastIndexOf('/') + 1);
            return command + "(" + t[0] + "%)";
        }).collect(Collectors.joining(" "));
    }
}
