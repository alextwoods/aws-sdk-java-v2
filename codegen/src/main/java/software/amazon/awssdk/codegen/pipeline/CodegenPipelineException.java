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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Thrown when a {@link CodegenPipeline} stage fails for a service. No generated sources are written for the service.
 */
@SdkInternalApi
public final class CodegenPipelineException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String service;
    private final CodegenPipeline.Stage stage;

    CodegenPipelineException(String service, CodegenPipeline.Stage stage, String detail, Throwable cause) {
        super("Code generation for service " + service + " failed in stage " + stage + ": " + detail, cause);
        this.service = Objects.requireNonNull(service, "service");
        this.stage = Objects.requireNonNull(stage, "stage");
    }

    /** The service whose generation failed. */
    public String service() {
        return service;
    }

    /** The stage that failed. */
    public CodegenPipeline.Stage stage() {
        return stage;
    }
}
