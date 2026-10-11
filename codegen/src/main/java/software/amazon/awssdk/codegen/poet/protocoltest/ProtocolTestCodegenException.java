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

package software.amazon.awssdk.codegen.poet.protocoltest;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Protocol test generation cannot proceed: the skip list is invalid or names an unknown test case, or a selected test
 * case that is not skipped cannot be generated. Thrown before any test source is written.
 */
@SdkInternalApi
public final class ProtocolTestCodegenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProtocolTestCodegenException(String message) {
        super(message);
    }

    public ProtocolTestCodegenException(String message, Throwable cause) {
        super(message, cause);
    }
}
