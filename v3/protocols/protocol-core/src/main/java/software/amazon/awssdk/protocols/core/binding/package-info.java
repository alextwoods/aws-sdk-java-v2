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
 * Immutable HTTP binding plans derived from the Smithy HTTP binding traits carried by runtime schemas:
 * {@link software.amazon.awssdk.protocols.core.binding.HttpOperationBindings} per operation and
 * {@link software.amazon.awssdk.protocols.core.binding.HttpMessageBindings} per bound structure. The HTTP-bound
 * protocol runtimes ({@code restJson1}, {@code restXml}) serialize and deserialize messages by walking these plans;
 * there are no generated per-operation marshallers (Requirements 5.3, 5.4, 5.7).
 *
 * <p>Plans describe locations only. They hold no request data and no HTTP message types; the HTTP message
 * representation belongs to {@code http-spi}.
 */
package software.amazon.awssdk.protocols.core.binding;
