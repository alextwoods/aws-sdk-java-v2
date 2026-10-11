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

package software.amazon.awssdk.codegen.customization.processors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessorChain;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;

public final class DefaultCustomizationProcessor {

    private DefaultCustomizationProcessor() {
    }

    public static CodegenCustomizationProcessor getProcessorFor(
            CustomizationConfig config) {

        return new CodegenCustomizationProcessorChain(
            namedProcessorsFor(config).values().toArray(new CodegenCustomizationProcessor[0]));
    }

    /**
     * The processors of {@link #getProcessorFor(CustomizationConfig)}, in the order they run, keyed by customization
     * name. A processor driven by a {@code customization.config} entry is named after that entry; the others are named
     * after their class, without the {@code Processor} suffix. The Codegen_Pipeline uses these names for
     * {@code disabledCustomizations} and in its log messages.
     */
    public static Map<String, CodegenCustomizationProcessor> namedProcessorsFor(CustomizationConfig config) {
        Map<String, CodegenCustomizationProcessor> processors = new LinkedHashMap<>();
        processors.put("customServiceMetadata", new MetadataModifiersProcessor(config.getCustomServiceMetadata()));
        processors.put("renameShapes", new RenameShapesProcessor(config.getRenameShapes()));
        processors.put("shapeModifiers", new ShapeModifiersProcessor(config.getShapeModifiers()));
        processors.put("shapeSubstitutions", new ShapeSubstitutionsProcessor(config.getShapeSubstitutions()));
        processors.put("customSdkShapes", new CustomSdkShapesProcessor(config.getCustomSdkShapes()));
        processors.put("operationModifiers", new OperationModifiersProcessor(config.getOperationModifiers()));
        processors.put("smithyRpcV2CborProtocol", new SmithyRpcV2CborProtocolProcessor());
        processors.put("removeExceptionMessageProperty", new RemoveExceptionMessagePropertyProcessor());
        processors.put("useLegacyEventGenerationScheme", new UseLegacyEventGenerationSchemeProcessor());
        processors.put("newAndLegacyEventStream", new NewAndLegacyEventStreamProcessor());
        processors.put("duplicateAndRenameSharedEvents",
                       new EventStreamSharedEventProcessor(config.getDuplicateAndRenameSharedEvents()));
        processors.put("s3RemoveBucketFromUri", new S3RemoveBucketFromUriProcessor());
        processors.put("s3ControlRemoveAccountIdHostPrefix", new S3ControlRemoveAccountIdHostPrefixProcessor());
        processors.put("explicitStringPayloadQueryProtocol", new ExplicitStringPayloadQueryProtocolProcessor());
        processors.put("lowercaseShapeValidator", new LowercaseShapeValidatorProcessor());
        processors.put("longPollingOperation", new LongPollingOperationProcessor());
        return Collections.unmodifiableMap(processors);
    }
}
