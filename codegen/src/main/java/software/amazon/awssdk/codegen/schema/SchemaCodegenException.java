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

package software.amazon.awssdk.codegen.schema;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Thrown when schema code generation cannot proceed: a misused trait inclusion setting (Requirements 9.6, 9.10, 9.11),
 * conflicting trait initializers (Requirement 9.8), or a trait value an initializer cannot render. It is raised while
 * the trait inclusion policy or the initializer registry is built, or while a trait is rendered, which all happen before
 * any generated source is written.
 */
@SdkProtectedApi
public final class SchemaCodegenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SchemaCodegenException(String message) {
        super(message);
    }

    public SchemaCodegenException(String message, Throwable cause) {
        super(message, cause);
    }
}
