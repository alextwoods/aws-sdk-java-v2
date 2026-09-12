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

package software.amazon.awssdk.awscore.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.ClientOption;

/**
 * Client options that are an implementation detail of the AWS request pipeline: computed once when the client is
 * built, read on every call, and not part of any public or protected configuration surface.
 */
@SdkInternalApi
public final class AwsInternalClientOption<T> extends ClientOption<T> {

    /**
     * The {@link SelectedAuthScheme} placeholder that the legacy {@code SERVICE_SIGNING_NAME} and
     * {@code SIGNING_REGION} execution-attribute writes would produce for this client.
     *
     * <p>Those two attributes are mapped: writing them constructs an {@code "unset"} auth scheme carrying the signing
     * name and region as signer properties, for the benefit of interceptors and old-style signers that read them back.
     * Both values are client constants, so the object graph they produce is the same on every call — but it was being
     * rebuilt per call: an option build, then a copy of that option to add the second property, plus a completed
     * future and two sentinel objects. This option holds that graph, built once, for the common case where no
     * request-level override has already put an auth scheme in place.
     */
    public static final AwsInternalClientOption<SelectedAuthScheme<?>> PLACEHOLDER_AUTH_SCHEME =
        new AwsInternalClientOption<>(new UnsafeValueType(SelectedAuthScheme.class));

    private AwsInternalClientOption(UnsafeValueType unsafeValueType) {
        super(unsafeValueType);
    }
}
