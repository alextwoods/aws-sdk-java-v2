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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.c2j2smithy.ConversionGapReport;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;

/**
 * A service that has been through stages 1 to 5 of {@link CodegenPipeline} and is ready for code generation.
 */
@SdkInternalApi
public final class PreparedService {
    private final ServiceInputs inputs;
    private final Model smithyModel;
    private final ConversionGapReport gapReport;
    private final IntermediateModel intermediateModel;
    private final List<String> supplementedPaginators;
    private final List<String> supplementedWaiters;

    PreparedService(ServiceInputs inputs, Model smithyModel, ConversionGapReport gapReport,
                    IntermediateModel intermediateModel, List<String> supplementedPaginators,
                    List<String> supplementedWaiters) {
        this.inputs = Objects.requireNonNull(inputs, "inputs");
        this.smithyModel = Objects.requireNonNull(smithyModel, "smithyModel");
        this.gapReport = gapReport;
        this.intermediateModel = Objects.requireNonNull(intermediateModel, "intermediateModel");
        this.supplementedPaginators = Collections.unmodifiableList(new ArrayList<>(supplementedPaginators));
        this.supplementedWaiters = Collections.unmodifiableList(new ArrayList<>(supplementedWaiters));
    }

    /** The inputs the service was prepared from. */
    public ServiceInputs inputs() {
        return inputs;
    }

    /** The assembled, validated Smithy model (stage 4). */
    public Model smithyModel() {
        return smithyModel;
    }

    /** The C2J conversion gap report (stage 3), or null when the service has a native Smithy model. */
    public ConversionGapReport gapReport() {
        return gapReport;
    }

    /** The intermediate model (stage 5). */
    public IntermediateModel intermediateModel() {
        return intermediateModel;
    }

    /**
     * The operations whose paginator was taken from {@code paginators-1.json} because the C2J_Converter could not
     * express it in {@code @paginated} (stage 5). Empty for a native Smithy model.
     */
    public List<String> supplementedPaginators() {
        return supplementedPaginators;
    }

    /**
     * The waiters taken from {@code waiters-2.json} because the C2J_Converter could not express them in
     * {@code smithy.waiters#waitable} (stage 5). Empty for a native Smithy model.
     */
    public List<String> supplementedWaiters() {
        return supplementedWaiters;
    }
}
