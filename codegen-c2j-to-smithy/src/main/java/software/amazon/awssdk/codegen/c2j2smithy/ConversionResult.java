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

package software.amazon.awssdk.codegen.c2j2smithy;

import java.util.Objects;
import software.amazon.smithy.model.Model;

/**
 * The output of {@link C2jToSmithyConverter#convert(C2jInput)}: the converted Smithy model and the gap report of
 * C2J properties that have no standard trait.
 */
public final class ConversionResult {
    private final Model model;
    private final ConversionGapReport gapReport;

    /**
     * @param model the converted, not yet validated, Smithy model
     * @param gapReport the gap report of the conversion
     */
    public ConversionResult(Model model, ConversionGapReport gapReport) {
        this.model = Objects.requireNonNull(model, "model");
        this.gapReport = Objects.requireNonNull(gapReport, "gapReport");
    }

    /**
     * The converted Smithy model. It is built with {@code Model.builder()} and is not validated; assemble it with the
     * {@code com.amazonaws.c2j} marker trait definitions on the class path to validate it.
     */
    public Model model() {
        return model;
    }

    /**
     * The gap report. Never null; empty when the conversion has no gaps.
     */
    public ConversionGapReport gapReport() {
        return gapReport;
    }
}
