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

package software.amazon.awssdk.endpoints.rules.internal;

/**
 * An invalid rule set detected during evaluation (a value of the wrong type, or a tree rule none of whose rules
 * matched). Rule sets are type-checked when they are built, so this never happens for canonical rule sets; the
 * public API reports it as an {@code EndpointResolutionException}.
 */
public final class RuleEvaluationError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RuleEvaluationError(String message) {
        super(message, null, false, false);
    }
}
