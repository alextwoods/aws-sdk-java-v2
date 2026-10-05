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
 * Type registries of generated services (Requirements 6.5-6.9).
 *
 * <p>{@link software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry} implements Smithy Java's
 * {@code TypeRegistry} and maps each exact shape ID to a
 * {@link software.amazon.awssdk.smithy.runtime.registry.RegisteredType}: the type's schema, Java class and builder
 * factory. Every generated service has one primary registry, built from lazily loaded partitions by generated code.
 * Registries compose with {@code SchemaTypeRegistry.compose}, which preserves entry identity and rejects conflicting
 * entries deterministically (Requirements 6.8 and 6.13), and typed construction from documents reports absent and
 * unknown discriminators precisely (Requirements 6.11 and 6.12). No type in this package loads classes by name, uses
 * reflection or reads a model.
 */
package software.amazon.awssdk.smithy.runtime.registry;
