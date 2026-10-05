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
 * Typed client configuration keys ({@code ClientOption}), configuration values ({@code TimeoutConfiguration}), and
 * resolution with provenance: {@code ClientConfigurationResolver} applies the precedence of {@code ConfigurationSource}
 * (request override, builder, JVM property, environment, profile, default) to {@code ClientSetting}s read through
 * injectable {@code SettingSource}s, and records each value as a {@code ResolvedOption}.
 */
package software.amazon.awssdk.core.client.config;
