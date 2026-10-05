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

package software.amazon.awssdk.core.util;

import java.util.Map;

/**
 * The empty map a generated accessor returns for an absent map member, as in V2: {@code hasXxx()} is {@code false}
 * exactly when {@code xxx()} returns an instance of this type. Setting a member to one of these (or {@code null})
 * leaves it absent.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface SdkAutoConstructMap<K, V> extends Map<K, V> {
}
