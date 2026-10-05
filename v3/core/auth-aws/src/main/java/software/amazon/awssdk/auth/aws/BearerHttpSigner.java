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

package software.amazon.awssdk.auth.aws;

import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/** The {@code smithy.api#httpBearerAuth} signer, with the V2 name: sets {@code Authorization: Bearer <token>}. */
public final class BearerHttpSigner implements HttpSigner<TokenIdentity> {
    private static final BearerHttpSigner INSTANCE = new BearerHttpSigner();

    private BearerHttpSigner() {
    }

    public static BearerHttpSigner create() {
        return INSTANCE;
    }

    @Override
    public SignedRequest sign(SignRequest<? extends TokenIdentity> request) {
        return SignedRequest.unsigned(request.request().toBuilder()
                                             .putHeader("Authorization", "Bearer " + request.identity().token())
                                             .build());
    }

    @Override
    public String toString() {
        return "BearerHttpSigner";
    }
}
