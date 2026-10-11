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

package software.amazon.awssdk.schema.traits;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;

/**
 * The {@code aws.protocols#ec2QueryName} trait. The {@code ec2Query} serialization name of a structure member.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait}.
 */
@SdkPublicApi
public final class Ec2QueryNameTrait extends StringTrait {

    /**
     * The trait's shape ID, {@code aws.protocols#ec2QueryName}.
     */
    public static final ShapeId ID = ShapeId.fromParts("aws.protocols", "ec2QueryName");

    /**
     * Creates the trait.
     *
     * @param value the query key
     * @throws NullPointerException if {@code value} is null
     */
    public Ec2QueryNameTrait(String value) {
        super(ID, value);
    }
}
