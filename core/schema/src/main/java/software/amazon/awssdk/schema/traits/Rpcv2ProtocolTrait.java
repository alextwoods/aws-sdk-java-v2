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

import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;

/**
 * Base class for the Smithy RPC v2 protocol definition traits, such as {@link Rpcv2CborTrait}.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.protocol.traits.Rpcv2ProtocolTrait}.
 */
@SdkPublicApi
public abstract class Rpcv2ProtocolTrait extends HttpProtocolTrait {

    Rpcv2ProtocolTrait(ShapeId traitId, List<String> http, List<String> eventStreamHttp) {
        super(traitId, http, eventStreamHttp);
    }
}
