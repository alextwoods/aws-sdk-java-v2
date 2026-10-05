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
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line entry point of the service fleet.
 *
 * <pre>
 * java -cp &lt;service-fleet.jar&gt;:&lt;smithy-model.jar&gt;:... software.amazon.awssdk.tools.servicefleet.ServiceFleetCli \
 *     sync|check --repository-root &lt;V2 checkout&gt; --v3-root &lt;V2 checkout&gt;/v3 --canonical-models &lt;aws-models checkout&gt;
 * </pre>
 *
 * <ul>
 *   <li>{@code sync} plans the fleet from the V2 baseline and the canonical checkout at its current {@code HEAD}
 *       (which must be clean), then writes the lock, the service modules with their vendored model files, the
 *       aggregator's module list and pinned revision, and the BOM entries. It prints what changed and every baseline
 *       service without a canonical model, and exits 1 if there is one, because the fleet gate then fails.</li>
 *   <li>{@code check} runs {@link ServiceFleetGate} and exits 1 on any problem.</li>
 * </ul>
 */
public final class ServiceFleetCli {

    private ServiceFleetCli() {
    }

    public static void main(String[] args) throws IOException {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length == 0 || !List.of("sync", "check").contains(args[0])) {
            err.println("usage: ServiceFleetCli sync|check --repository-root <dir> --v3-root <dir> "
                        + "--canonical-models <dir>");
            return 2;
        }
        Map<String, Path> options = new LinkedHashMap<>();
        for (int i = 1; i + 1 < args.length; i += 2) {
            options.put(args[i], Path.of(args[i + 1]).toAbsolutePath().normalize());
        }
        Path repository = options.get("--repository-root");
        Path v3 = options.getOrDefault("--v3-root", repository == null ? null : repository.resolve("v3"));
        Path models = options.get("--canonical-models");
        if (repository == null || v3 == null || ("sync".equals(args[0]) && models == null)) {
            err.println("--repository-root" + ("sync".equals(args[0]) ? " and --canonical-models are" : " is")
                        + " required");
            return 2;
        }
        if ("check".equals(args[0])) {
            ServiceFleetGate.Result result = ServiceFleetGate.check(repository, v3, models);
            result.notes().forEach(n -> out.println("note: " + n));
            result.problems().forEach(err::println);
            out.println(result.problems().size() + " problem(s)");
            return result.problems().isEmpty() ? 0 : 1;
        }
        String status = ServiceFleetGate.git(models, "status", "--porcelain").trim();
        if (!status.isEmpty()) {
            err.println("The canonical checkout " + models + " has local changes; sync only from a clean revision:\n"
                        + status);
            return 1;
        }
        String revision = ServiceFleetGate.git(models, "rev-parse", "HEAD").trim();
        FleetPlan plan = ServiceFleetPlanner.plan(V2Service.scan(repository), CanonicalModel.scan(models), revision);
        List<String> changed = ServiceFleetWriter.write(plan, v3, models);
        changed.forEach(c -> out.println("updated " + c));
        out.println("Planned " + plan.services().size() + " service(s) in " + plan.modules().size()
                    + " module(s) from " + FleetPlan.CANONICAL_REPOSITORY + " revision " + revision + "; "
                    + changed.size() + " path(s) changed");
        plan.missing().forEach(m -> err.println(ServiceFleetGate.MISSING_CLIENT + ": " + m));
        return plan.missing().isEmpty() ? 0 : 1;
    }
}
