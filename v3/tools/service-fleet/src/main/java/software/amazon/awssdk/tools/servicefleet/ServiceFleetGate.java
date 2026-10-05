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

package software.amazon.awssdk.tools.servicefleet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.awssdk.tools.servicefleet.CanonicalModel.ModelFile;

/**
 * The fleet gate (Requirements 2.1, 2.10, 16.4, 16.5): checks a V3 tree against the V2 Launch Parity Baseline and the
 * pinned {@link FleetPlan}. Every problem starts with its category:
 *
 * <ul>
 *   <li>{@value #MISSING_CLIENT}: a baseline service has no generated V3 service (no canonical model at the pinned
 *       revision, an ambiguous model, or a model without a launch protocol).</li>
 *   <li>{@value #STALE_FLEET}: the lock disagrees with the baseline (package, required V2 client types, SDK ID) or,
 *       when the canonical checkout is available, with a fresh plan of it.</li>
 *   <li>{@value #STALE_BUILD}: a service module {@code pom.xml}, the services aggregator's module list or the BOM's
 *       service entries are not exactly what the lock renders, or a module directory is missing or left over.</li>
 *   <li>{@value #PROVENANCE_MISMATCH}: the aggregator's pinned revision is not the lock's; a vendored model file is
 *       missing, extra or has another digest; or the canonical checkout is at another revision, has local changes
 *       in a used model directory, or holds other model files.</li>
 * </ul>
 *
 * <p>Missing AWS semantics are not checked here: they fail {@code aws-codegen:generate} for the affected module.
 */
public final class ServiceFleetGate {

    public static final String MISSING_CLIENT = "MISSING_CLIENT";
    public static final String STALE_FLEET = "STALE_FLEET";
    public static final String STALE_BUILD = "STALE_BUILD";
    public static final String PROVENANCE_MISMATCH = "PROVENANCE_MISMATCH";

    private ServiceFleetGate() {
    }

    /**
     * The outcome of a gate run.
     *
     * @param problems the gate failures, sorted; the fleet build fails unless this is empty
     * @param notes what the run could not check (for example the canonical checkout is absent)
     */
    public record Result(List<String> problems, List<String> notes) {
        public Result {
            problems = List.copyOf(problems);
            notes = List.copyOf(notes);
        }
    }

    /**
     * Checks the V3 tree at {@code v3Root} against the V2 baseline at {@code repositoryRoot} and, when it is not
     * {@code null} and exists, the canonical aws-models checkout at {@code canonicalModels}.
     */
    public static Result check(Path repositoryRoot, Path v3Root, Path canonicalModels) {
        List<String> problems = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Path lockFile = v3Root.resolve(ServiceFleetLock.PATH);
        if (!Files.isRegularFile(lockFile)) {
            return new Result(List.of(STALE_FLEET + ": " + ServiceFleetLock.PATH + " does not exist"), notes);
        }
        FleetPlan lock = ServiceFleetLock.read(V2Service.read(lockFile));
        // A V3-only checkout (for example the clean-checkout reactor test's copy of v3/) has no V2 baseline to compare
        // with; the repository build, which has it, always does.
        List<V2Service> baseline = Files.isRegularFile(repositoryRoot.resolve("services/pom.xml"))
                                   ? V2Service.scan(repositoryRoot) : null;
        if (baseline != null) {
            checkBaseline(baseline, lock, problems);
        } else {
            notes.add("The V2 baseline (" + repositoryRoot.resolve("services/pom.xml") + ") is not in this checkout: "
                      + "the fleet was not compared with it");
        }
        checkBuild(lock, v3Root, problems);
        checkVendoredModels(lock, v3Root, problems);
        if (baseline != null && canonicalModels != null && Files.isDirectory(canonicalModels)) {
            checkCanonicalCheckout(lock, baseline, canonicalModels, problems);
        } else {
            notes.add("The canonical aws-models checkout " + canonicalModels + " is not available: the vendored model "
                      + "files were checked against the lock's digests only, not against revision "
                      + lock.revision() + " of " + FleetPlan.CANONICAL_REPOSITORY);
        }
        problems.sort(String::compareTo);
        return new Result(problems, notes);
    }

    private static void checkBaseline(List<V2Service> baseline, FleetPlan lock, List<String> problems) {
        Map<String, FleetService> fleet = lock.services().stream()
                                              .collect(Collectors.toMap(FleetService::name, s -> s, (a, b) -> a,
                                                                        LinkedHashMap::new));
        Map<String, String> reasons = new LinkedHashMap<>();
        lock.missing().forEach(m -> reasons.put(m.substring(0, m.indexOf(':')), m.substring(m.indexOf(':') + 2)));
        Set<String> names = new TreeSet<>();
        for (V2Service v2 : baseline) {
            names.add(v2.name());
            FleetService service = fleet.get(v2.name());
            if (service == null) {
                problems.add(MISSING_CLIENT + ": V2 baseline service " + v2.name() + " (serviceId " + v2.serviceId()
                             + ") has no generated V3 service module: "
                             + reasons.getOrDefault(v2.name(), "it is not in " + ServiceFleetLock.PATH));
                continue;
            }
            if (!service.module().equals(v2.module()) || !service.packageName().equals(v2.packageName())) {
                problems.add(STALE_FLEET + ": " + v2.name() + " is planned as module " + service.module()
                             + ", package " + service.packageName() + "; the baseline has module " + v2.module()
                             + ", package " + v2.packageName());
            }
            if (!Objects.equals(service.sdkId(), v2.serviceId())) {
                problems.add(STALE_FLEET + ": " + v2.name() + " is planned from sdkId " + service.sdkId()
                             + ", but its V2 serviceId is " + v2.serviceId());
            } else if (!service.requiredTypes().equals(v2.requiredTypes())) {
                problems.add(STALE_FLEET + ": " + v2.name() + " requires " + service.requiredTypes()
                             + ", but the V2 baseline generates " + v2.requiredTypes());
            }
        }
        for (FleetService service : lock.services()) {
            if (!names.contains(service.name())) {
                problems.add(STALE_FLEET + ": " + service.name() + " is not a service of the V2 baseline");
            }
        }
    }

    private static void checkBuild(FleetPlan lock, Path v3Root, List<String> problems) {
        Path services = v3Root.resolve("services");
        Map<String, List<FleetService>> modules = lock.modules();
        for (Map.Entry<String, List<FleetService>> module : modules.entrySet()) {
            Path pom = services.resolve(module.getKey()).resolve("pom.xml");
            if (!Files.isRegularFile(pom)) {
                problems.add(STALE_BUILD + ": services/" + module.getKey() + "/pom.xml does not exist");
            } else if (!V2Service.read(pom).equals(ServiceFleetRenderer.modulePom(module.getValue()))) {
                problems.add(STALE_BUILD + ": services/" + module.getKey() + "/pom.xml is not the POM "
                             + ServiceFleetLock.PATH + " renders");
            }
        }
        for (Path generated : ServiceFleetWriter.generatedModules(services)) {
            if (!modules.containsKey(generated.getFileName().toString())) {
                problems.add(STALE_BUILD + ": services/" + generated.getFileName() + " is a generated service module "
                             + "that " + ServiceFleetLock.PATH + " does not list");
            }
        }
        String aggregator = V2Service.read(services.resolve("pom.xml"));
        if (!expected(aggregator, "services/pom.xml",
                      indent -> ServiceFleetRenderer.aggregatorModules(lock, indent), problems)) {
            problems.add(STALE_BUILD + ": the module list of services/pom.xml is not the one " + ServiceFleetLock.PATH
                         + " renders");
        }
        String revision = ServiceFleetRenderer.revision(aggregator);
        if (!lock.revision().equals(revision)) {
            problems.add(PROVENANCE_MISMATCH + ": services/pom.xml pins canonical model revision " + revision
                         + ", but " + ServiceFleetLock.PATH + " was planned from revision " + lock.revision());
        }
        if (!expected(V2Service.read(v3Root.resolve("bom/pom.xml")), "bom/pom.xml",
                      indent -> ServiceFleetRenderer.bomEntries(lock, indent), problems)) {
            problems.add(STALE_BUILD + ": the service entries of bom/pom.xml are not the ones " + ServiceFleetLock.PATH
                         + " renders");
        }
    }

    private static boolean expected(String pom, String file, java.util.function.Function<String, String> rendering,
                                    List<String> problems) {
        try {
            return ServiceFleetRenderer.replaceRegion(pom, file, rendering).equals(pom);
        } catch (IllegalStateException e) {
            problems.add(STALE_BUILD + ": " + e.getMessage());
            return true;
        }
    }

    private static void checkVendoredModels(FleetPlan lock, Path v3Root, List<String> problems) {
        for (FleetService service : lock.services()) {
            Path directory = v3Root.resolve("services").resolve(service.module()).resolve(service.modelDirectory());
            List<ModelFile> actual = CanonicalModel.files(directory);
            if (!actual.equals(service.modelFiles())) {
                problems.add(PROVENANCE_MISMATCH + ": " + service.name() + ": the model files under services/"
                             + service.module() + "/" + service.modelDirectory() + " are " + describe(actual)
                             + ", but revision " + lock.revision() + " of " + service.canonicalDirectory()
                             + " pins " + describe(service.modelFiles()));
            } else if (!CanonicalModel.modelDigest(directory, actual).equals(service.modelDigest())) {
                problems.add(PROVENANCE_MISMATCH + ": " + service.name() + ": the model digest of services/"
                             + service.module() + "/" + service.modelDirectory() + " is not "
                             + service.modelDigest());
            }
        }
    }

    private static void checkCanonicalCheckout(FleetPlan lock, List<V2Service> baseline, Path canonicalModels,
                                               List<String> problems) {
        String head;
        try {
            head = git(canonicalModels, "rev-parse", "HEAD").trim();
        } catch (IOException e) {
            problems.add(PROVENANCE_MISMATCH + ": the revision of the canonical checkout " + canonicalModels
                         + " cannot be determined: " + e.getMessage());
            return;
        }
        if (!head.equals(lock.revision())) {
            problems.add(PROVENANCE_MISMATCH + ": the canonical checkout " + canonicalModels + " is at revision " + head
                         + ", but the fleet is pinned to revision " + lock.revision() + "; check out the pinned "
                         + "revision or re-plan the fleet with ServiceFleetCli sync");
            return;
        }
        Set<String> used = lock.services().stream().map(FleetService::canonicalDirectory)
                               .collect(Collectors.toCollection(TreeSet::new));
        try {
            // Porcelain lines are "XY <path>"; X is a space for unstaged changes, so only trailing space is stripped.
            String status = git(canonicalModels, "status", "--porcelain").stripTrailing();
            for (String line : status.lines().filter(l -> !l.isBlank()).toList()) {
                String path = line.length() > 3 ? line.substring(3) : line;
                String top = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;
                if (used.contains(top)) {
                    problems.add(PROVENANCE_MISMATCH + ": the canonical checkout has local changes in " + path
                                 + ", which revision " + lock.revision() + " does not contain");
                }
            }
        } catch (IOException e) {
            problems.add(PROVENANCE_MISMATCH + ": the status of the canonical checkout cannot be read: "
                         + e.getMessage());
        }
        FleetPlan fresh = ServiceFleetPlanner.plan(baseline, CanonicalModel.scan(canonicalModels), lock.revision());
        Map<String, FleetService> planned = fresh.services().stream()
                                                 .collect(Collectors.toMap(FleetService::name, s -> s));
        for (FleetService service : lock.services()) {
            FleetService expected = planned.get(service.name());
            if (!service.equals(expected)) {
                problems.add(PROVENANCE_MISMATCH + ": " + service.name() + " is pinned as " + summary(service)
                             + ", but revision " + lock.revision() + " of the canonical checkout plans "
                             + (expected == null ? "no service" : summary(expected)));
            }
        }
        if (!fresh.missing().equals(lock.missing())
            || fresh.services().size() != lock.services().size()) {
            problems.add(STALE_FLEET + ": " + ServiceFleetLock.PATH + " is not the plan of the canonical checkout "
                         + "(missing " + lock.missing() + " pinned, " + fresh.missing() + " planned); re-plan the fleet "
                         + "with ServiceFleetCli sync");
        }
    }

    private static String summary(FleetService service) {
        return service.service() + " from " + service.canonicalDirectory() + " (" + service.modelDigest() + ", "
               + service.protocolModules() + ")";
    }

    private static String describe(List<ModelFile> files) {
        return files.isEmpty() ? "absent" : files.stream().map(f -> f.path() + " " + f.digest()).toList().toString();
    }

    static String git(Path directory, String... args) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "-C", directory.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            if (process.waitFor() != 0) {
                throw new IOException("git " + String.join(" ", args) + " failed: " + output.trim());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
        return output;
    }
}
