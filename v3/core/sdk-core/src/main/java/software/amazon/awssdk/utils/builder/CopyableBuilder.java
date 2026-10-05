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

package software.amazon.awssdk.utils.builder;

/**
 * A builder of {@code T} that can be copied.
 *
 * @param <B> the builder type
 * @param <T> the built type
 */
public interface CopyableBuilder<B extends CopyableBuilder<B, T>, T extends ToCopyableBuilder<B, T>>
    extends SdkBuilder<B, T> {

    /** A new builder with the state of this one; later changes to either do not affect the other. */
    default B copy() {
        return build().toBuilder();
    }
}
