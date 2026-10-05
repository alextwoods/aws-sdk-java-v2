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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import software.amazon.awssdk.tools.servicefleet.CanonicalModel.ModelFile;

/**
 * Writes the fleet of a {@link FleetPlan} into a V3 tree: the lock, every service module's {@code pom.xml} and
 * vendored canonical model files, the services aggregator's module list and pinned revision, and the BOM's service
 * entries. Directories under {@code services/} that an earlier run generated (their {@code pom.xml} carries
 * {@link ServiceFleetRenderer#GENERATED_MARKER}) and that the plan no longer lists are removed; no other directory is
 * touched.
 */
public final class ServiceFleetWriter {

    private ServiceFleetWriter() {
    }

    /**
     * Writes {@code plan} into the V3 tree at {@code v3Root}, copying model files from the canonical checkout at
     * {@code canonicalModels}.
     *
     * @return the paths written or deleted, relative to {@code v3Root}, sorted
     */
    public static List<String> write(FleetPlan plan, Path v3Root, Path canonicalModels) {
        List<String> changed = new ArrayList<>();
        Path services = v3Root.resolve("services");
        Map<String, List<FleetService>> modules = plan.modules();
        for (Map.Entry<String, List<FleetService>> module : modules.entrySet()) {
            Path directory = services.resolve(module.getKey());
            for (FleetService service : module.getValue()) {
                Path source = canonicalModels.resolve(service.canonicalDirectory()).resolve("smithy");
                Path target = directory.resolve(service.modelDirectory());
                Set<String> wanted = new TreeSet<>();
                for (ModelFile file : service.modelFiles()) {
                    wanted.add(file.path());
                    Path to = target.resolve(file.path());
                    byte[] content = CanonicalModel.readBytes(source.resolve(file.path()));
                    if (!CanonicalModel.sha256(content).equals(file.digest())) {
                        throw new IllegalStateException(source.resolve(file.path()) + " does not have the planned "
                                                        + "digest " + file.digest());
                    }
                    if (!Files.isRegularFile(to) || !CanonicalModel.sha256(CanonicalModel.readBytes(to))
                                                                   .equals(file.digest())) {
                        writeBytes(to, content);
                        changed.add(v3Root.relativize(to).toString());
                    }
                }
                for (ModelFile stale : CanonicalModel.files(target)) {
                    if (!wanted.contains(stale.path())) {
                        delete(target.resolve(stale.path()));
                        changed.add(v3Root.relativize(target.resolve(stale.path())).toString());
                    }
                }
            }
            if (writeIfChanged(directory.resolve("pom.xml"), ServiceFleetRenderer.modulePom(module.getValue()))) {
                changed.add(v3Root.relativize(directory.resolve("pom.xml")).toString());
            }
        }
        for (Path stale : generatedModules(services)) {
            if (!modules.containsKey(stale.getFileName().toString())) {
                deleteRecursively(stale);
                changed.add(v3Root.relativize(stale).toString());
            }
        }
        Path aggregator = services.resolve("pom.xml");
        String pom = V2Service.read(aggregator);
        pom = ServiceFleetRenderer.replaceRegion(pom, "services/pom.xml",
                                                 indent -> ServiceFleetRenderer.aggregatorModules(plan, indent));
        if (writeIfChanged(aggregator, ServiceFleetRenderer.pinRevision(pom, "services/pom.xml", plan.revision()))) {
            changed.add("services/pom.xml");
        }
        Path bom = v3Root.resolve("bom/pom.xml");
        if (writeIfChanged(bom, ServiceFleetRenderer.replaceRegion(
            V2Service.read(bom), "bom/pom.xml", indent -> ServiceFleetRenderer.bomEntries(plan, indent)))) {
            changed.add("bom/pom.xml");
        }
        if (writeIfChanged(v3Root.resolve(ServiceFleetLock.PATH), ServiceFleetLock.write(plan))) {
            changed.add(ServiceFleetLock.PATH);
        }
        changed.sort(Comparator.naturalOrder());
        return changed;
    }

    /** Directories directly under {@code services} whose {@code pom.xml} the writer generated, sorted. */
    static List<Path> generatedModules(Path services) {
        try (Stream<Path> list = Files.list(services)) {
            return list.filter(d -> Files.isRegularFile(d.resolve("pom.xml")))
                       .filter(d -> V2Service.read(d.resolve("pom.xml")).contains(ServiceFleetRenderer.GENERATED_MARKER))
                       .sorted()
                       .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean writeIfChanged(Path file, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (Files.isRegularFile(file) && Arrays.equals(CanonicalModel.readBytes(file), bytes)) {
            return false;
        }
        writeBytes(file, bytes);
        return true;
    }

    private static void writeBytes(Path file, byte[] content) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + file, e);
        }
    }

    private static void delete(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete " + file, e);
        }
    }

    private static void deleteRecursively(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(ServiceFleetWriter::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
