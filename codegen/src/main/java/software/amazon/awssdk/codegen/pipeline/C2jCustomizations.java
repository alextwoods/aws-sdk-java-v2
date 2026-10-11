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

package software.amazon.awssdk.codegen.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.customization.processors.DefaultCustomizationProcessor;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;

/**
 * Stage 2 of the Codegen_Pipeline: applies the C2J (source model) half of the customizations to the C2J service model.
 *
 * <p>Each customization runs on its own (Requirement 17.11). One listed in {@code disabledCustomizations} is not run,
 * and one that throws is undone, so that the model is as it was before the customization started. Both are reported as
 * {@code customization <name> skipped for <service>: <reason>} and the remaining customizations still run.
 * {@link ModelInvalidException} is not a customization failure but a model validation result, so it is propagated.
 */
@SdkInternalApi
final class C2jCustomizations {

    private C2jCustomizations() {
    }

    /**
     * Applies the customizations and returns the customized model, which may be a new instance.
     */
    static ServiceModel apply(String service, ServiceModel serviceModel, CustomizationConfig config,
                              CodegenPipelineListener listener) {
        Map<String, CodegenCustomizationProcessor> processors = DefaultCustomizationProcessor.namedProcessorsFor(config);
        Set<String> disabled = disabledCustomizations(config);
        for (String name : disabled) {
            if (!processors.containsKey(name)) {
                listener.warning(service, "disabledCustomizations entry " + name + " for " + service
                                          + " does not name a C2J customization; known names: " + processors.keySet());
            }
        }

        ServiceModel current = serviceModel;
        for (Map.Entry<String, CodegenCustomizationProcessor> entry : processors.entrySet()) {
            String name = entry.getKey();
            if (disabled.contains(name)) {
                listener.warning(service, skipped(name, service, "disabled by disabledCustomizations"));
                continue;
            }

            JsonNode before = C2jServiceModelSerializer.toTree(current);
            try {
                entry.getValue().preprocess(current);
            } catch (ModelInvalidException e) {
                throw e;
            } catch (RuntimeException e) {
                listener.warning(service, skipped(name, service, String.valueOf(e.getMessage())));
                current = C2jServiceModelSerializer.fromTree(before);
            }
        }
        return current;
    }

    static String skipped(String name, String service, String reason) {
        return "customization " + name + " skipped for " + service + ": " + reason;
    }

    private static Set<String> disabledCustomizations(CustomizationConfig config) {
        List<String> names = config.getDisabledCustomizations();
        return names == null ? new LinkedHashSet<>() : new LinkedHashSet<>(names);
    }
}
