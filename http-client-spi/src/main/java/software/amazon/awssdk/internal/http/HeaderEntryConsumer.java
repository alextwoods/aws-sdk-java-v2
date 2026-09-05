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

package software.amazon.awssdk.internal.http;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Callback for per-entry header iteration: one call per name/value pair rather than one call per name with a
 * materialized value list. A dedicated interface rather than {@code BiConsumer<String, String>} so a single class can
 * implement both this and {@code BiConsumer<String, List<String>>} (same erasure otherwise).
 */
@SdkInternalApi
@FunctionalInterface
public interface HeaderEntryConsumer {
    void accept(String name, String value);
}
