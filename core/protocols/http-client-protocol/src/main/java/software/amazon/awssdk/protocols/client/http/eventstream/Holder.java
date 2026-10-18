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

package software.amazon.awssdk.protocols.client.http.eventstream;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A mutable reference used while encoding one event.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.Holder}.
 */
@SdkInternalApi
final class Holder<T> {
    private T value;

    Holder() {
    }

    Holder(T value) {
        this.value = value;
    }

    void set(T value) {
        this.value = value;
    }

    boolean setIfAbsent(T value) {
        if (this.value == null) {
            this.value = value;
            return true;
        }
        return false;
    }

    T get() {
        return value;
    }
}
