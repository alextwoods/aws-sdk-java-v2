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

package software.amazon.awssdk.schema;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A container that provides member schemas by name.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.MemberLookup}.
 */
@SdkPublicApi
public interface MemberLookup {
    /**
     * Returns the member with the given name.
     *
     * @param memberName the Smithy member name
     * @return the member schema, or null if there is no member with that name
     */
    Schema member(String memberName);
}
