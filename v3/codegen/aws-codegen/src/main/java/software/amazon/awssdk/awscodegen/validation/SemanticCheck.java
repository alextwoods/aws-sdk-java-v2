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

package software.amazon.awssdk.awscodegen.validation;

import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * One required-AWS-behavior check over the assembled canonical model (Requirement 2.10). A check reads the Smithy
 * {@link Model} and its indexes directly and reports every service or operation whose required behavior has no
 * supported canonical representation. Checks never modify the model and never infer a missing behavior.
 */
public interface SemanticCheck {

    /** Stable identifier of the check, for example {@code aws.protocol}. */
    String id();

    /**
     * Returns one diagnostic per missing or unsupported element, in any order. {@code service} is a shape of
     * {@code model}.
     */
    List<Diagnostic> check(Model model, ServiceShape service);
}
