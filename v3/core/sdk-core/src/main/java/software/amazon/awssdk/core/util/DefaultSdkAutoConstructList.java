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

import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * The immutable empty {@link SdkAutoConstructList}. Equal to every other empty list.
 *
 * @param <T> element type
 */
public final class DefaultSdkAutoConstructList<T> extends AbstractList<T>
    implements SdkAutoConstructList<T>, RandomAccess {

    private static final DefaultSdkAutoConstructList<?> INSTANCE = new DefaultSdkAutoConstructList<>();

    private DefaultSdkAutoConstructList() {
    }

    @SuppressWarnings("unchecked")
    public static <T> DefaultSdkAutoConstructList<T> getInstance() {
        return (DefaultSdkAutoConstructList<T>) INSTANCE;
    }

    @Override
    public T get(int index) {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    @Override
    public int size() {
        return 0;
    }
}
