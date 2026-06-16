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

package software.amazon.awssdk.c2j.smithy;

import java.io.StringWriter;
import java.nio.file.Path;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Parity check for the Smithy-seeded IR path. Builds v2's {@link IntermediateModel} two ways for the
 * same service and diffs them:
 *
 * <ol>
 *   <li><b>Direct C2J:</b> {@code service-2.json -> ServiceModel (Jackson) -> IntermediateModelBuilder}
 *       — exactly what v2 does today.</li>
 *   <li><b>Smithy-seeded:</b> {@code service-2.json -> Smithy Model (C2jToSmithyConverter) ->
 *       ServiceModel (SmithyToServiceModel) -> IntermediateModelBuilder} — the new on-ramp.</li>
 * </ol>
 *
 * <p>{@link IntermediateModelBuilder} is used <b>unchanged</b> by both paths; only the source of the
 * {@link ServiceModel} differs. The two IRs are serialized with v2's own object mapper settings and
 * compared line-by-line, so any divergence is reported precisely.
 *
 * <p>Usage: {@code SmithyIrParityRunner <service-2.json> [<service-2.json> ...]}
 */
public final class SmithyIrParityRunner {

    private SmithyIrParityRunner() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: SmithyIrParityRunner [--dump <dir>] <service-2.json> ...");
        }
        // --dump <dir>: write both IRs (<svc>.direct.json / <svc>.smithy.json) to <dir> for one service
        // and exit, instead of diffing. Lets the caller inspect full IR with shape-name context.
        if (args.length >= 3 && "--dump".equals(args[0])) {
            dump(Path.of(args[1]), Path.of(args[2]));
            return;
        }
        int failures = 0;
        for (String arg : args) {
            Path serviceJson = Path.of(arg);
            try {
                if (!compareOne(serviceJson)) {
                    failures++;
                }
            } catch (RuntimeException e) {
                System.out.println("[ERROR] " + serviceJson + ": " + e);
                e.printStackTrace(System.out);
                failures++;
            }
        }
        System.out.println(failures == 0 ? "\nPARITY OK (all services)" : "\nPARITY FAILURES: " + failures);
        if (failures != 0) {
            throw new IllegalStateException(failures + " service(s) diverged");
        }
    }

    // Load the per-service customization.config (empty if absent) — both paths must apply the same one.
    private static CustomizationConfig loadCustom(Path serviceJson) {
        return ModelLoaderUtils.loadOptionalModel(
                CustomizationConfig.class,
                serviceJson.resolveSibling("customization.config").toFile(), true)
                .orElseGet(CustomizationConfig::create);
    }

    // Build the IR from a ServiceModel, loading the SAME sibling models (waiters/paginators/endpoint
    // rule set + tests) the maven plugin loads — so services whose customization references endpoint
    // parameters (e.g. s3) build instead of NPEing on a null endpointRuleSet.
    private static IntermediateModel buildIr(ServiceModel serviceModel, Path serviceJson,
                                             CustomizationConfig custom) {
        Path dir = serviceJson.getParent();
        Waiters waiters = ModelLoaderUtils.loadOptionalModel(
                Waiters.class, dir.resolve("waiters-2.json").toFile()).orElse(Waiters.none());
        Paginators paginators = ModelLoaderUtils.loadOptionalModel(
                Paginators.class, dir.resolve("paginators-1.json").toFile()).orElse(Paginators.none());
        EndpointRuleSetModel ruleSet = ModelLoaderUtils.loadOptionalModel(
                EndpointRuleSetModel.class, dir.resolve("endpoint-rule-set.json").toFile()).orElse(null);
        EndpointTestSuiteModel tests = ModelLoaderUtils.loadOptionalModel(
                EndpointTestSuiteModel.class, dir.resolve("endpoint-tests.json").toFile()).orElse(null);
        return new IntermediateModelBuilder(
                C2jModels.builder()
                         .serviceModel(serviceModel)
                         .customizationConfig(custom)
                         .waitersModel(waiters)
                         .paginatorsModel(paginators)
                         .endpointRuleSetModel(ruleSet)
                         .endpointTestSuiteModel(tests)
                         .build()).build();
    }

    private static void dump(Path serviceJson, Path outDir) {
        CustomizationConfig custom = loadCustom(serviceJson);
        ServiceModel directModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceJson.toFile());
        IntermediateModel directIr = buildIr(directModel, serviceJson, custom);
        ServiceModel viaSmithy = SmithyToServiceModel.fromC2jViaSmithy(serviceJson);
        IntermediateModel smithyIr = buildIr(viaSmithy, serviceJson, custom);
        String svc = serviceJson.getParent().getParent().getParent().getParent().getParent()
                                .getFileName().toString();
        try {
            java.nio.file.Files.writeString(outDir.resolve(svc + ".direct.json"), toJson(directIr));
            java.nio.file.Files.writeString(outDir.resolve(svc + ".smithy.json"), toJson(smithyIr));
            System.out.println("dumped " + svc + " to " + outDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean compareOne(Path serviceJson) {
        // Load the REAL per-service customization.config so both paths apply the same customizations
        // (e.g. a shapeModifiers exclude of Content-Length on payload shapes) — exactly what the
        // product build does. Both IRs are then built with that same config + sibling models.
        CustomizationConfig custom = loadCustom(serviceJson);

        // Path 1: direct C2J -> IR (today's behavior).
        ServiceModel directModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceJson.toFile());
        IntermediateModel directIr = buildIr(directModel, serviceJson, custom);

        // Path 2: C2J -> Smithy -> ServiceModel -> IR (the new on-ramp), IntermediateModelBuilder unchanged.
        ServiceModel viaSmithy = SmithyToServiceModel.fromC2jViaSmithy(serviceJson);
        IntermediateModel smithyIr = buildIr(viaSmithy, serviceJson, custom);

        String directJson = toJson(directIr);
        String smithyJson = toJson(smithyIr);

        if (directJson.equals(smithyJson)) {
            System.out.println("[OK]   " + serviceJson.getParent().getFileName() + " — IR identical ("
                               + directIr.getShapes().size() + " shapes, "
                               + directIr.getOperations().size() + " operations)");
            return true;
        }
        System.out.println("[DIFF] " + serviceJson.getParent().getFileName() + " — IR differs:");
        printFirstDiffs(directJson, smithyJson, 40);
        return false;
    }

    private static String toJson(IntermediateModel ir) {
        try {
            StringWriter w = new StringWriter();
            Jackson.writeWithObjectMapper(ir, w);   // v2's own mapper (Jdk8Module + ordered keys)
            return w.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void printFirstDiffs(String a, String b, int maxLines) {
        String[] la = a.split("\n");
        String[] lb = b.split("\n");
        int shown = 0;
        for (int i = 0; i < Math.max(la.length, lb.length) && shown < maxLines; i++) {
            String x = i < la.length ? la[i] : "<EOF>";
            String y = i < lb.length ? lb[i] : "<EOF>";
            if (!x.equals(y)) {
                System.out.println("  L" + (i + 1) + " direct: " + x.trim());
                System.out.println("  L" + (i + 1) + " smithy: " + y.trim());
                shown++;
            }
        }
        System.out.println("  (direct " + la.length + " lines, smithy " + lb.length + " lines)");
    }
}
