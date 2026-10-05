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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;

/**
 * The immutable empty {@link SdkAutoConstructMap}. Equal to every other empty map.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class DefaultSdkAutoConstructMap<K, V> extends AbstractMap<K, V> implements SdkAutoConstructMap<K, V> {

    private static final DefaultSdkAutoConstructMap<?, ?> INSTANCE = new DefaultSdkAutoConstructMap<>();

    private DefaultSdkAutoConstructMap() {
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DefaultSdkAutoConstructMap<K, V> getInstance() {
        return (DefaultSdkAutoConstructMap<K, V>) INSTANCE;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.emptySet();
    }
}
