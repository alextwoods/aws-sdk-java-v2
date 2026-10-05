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

import software.amazon.awssdk.auth.api.SignerProperty;
import software.amazon.awssdk.auth.aws.internal.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * The AWS Signature Version 4 signer ({@code aws.auth#sigv4}), with the V2 name and properties. Session credentials add
 * {@code X-Amz-Security-Token}; every header-signed request carries {@code x-amz-content-sha256}.
 */
public interface AwsV4HttpSigner extends AwsV4FamilyHttpSigner<AwsCredentialsIdentity> {
    /** The signing region. Required. */
    SignerProperty<String> REGION_NAME = SignerProperty.create(AwsV4HttpSigner.class, "RegionName");

    /** The signer. */
    static AwsV4HttpSigner create() {
        return DefaultAwsV4HttpSigner.INSTANCE;
    }
}
