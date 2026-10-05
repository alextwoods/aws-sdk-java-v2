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

/**
 * Schema-driven Smithy Java {@link software.amazon.smithy.java.core.serde.document.Document} conversion (Requirements
 * 6.1-6.4, 6.10 and 6.14).
 *
 * <p>V3 uses Smithy Java's {@code Document} interface directly and adds what Smithy Java 1.6.1 lacks:
 * {@link software.amazon.awssdk.smithy.runtime.document.ModeledDocument}, a document that keeps the exact modeled
 * kind and shape ID of every value, and {@link software.amazon.awssdk.smithy.runtime.document.SchemaDocuments}, which
 * converts generated values to modeled documents and any document (including protocol-decoded documents, whose
 * protocol context it keeps) back to generated values or modeled documents with exact, deterministic checks reported
 * as {@link software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException}.
 */
package software.amazon.awssdk.smithy.runtime.document;
