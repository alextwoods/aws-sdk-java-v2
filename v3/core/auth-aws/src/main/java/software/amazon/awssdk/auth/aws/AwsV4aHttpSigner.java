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
import software.amazon.awssdk.auth.aws.internal.DefaultAwsV4aHttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * The AWS Signature Version 4a signer ({@code aws.auth#sigv4a}), with the V2 name and properties: a multi-region
 * ECDSA P-256/SHA-256 signature with a key derived from the credentials, a region-less credential scope and the
 * {@code X-Amz-Region-Set} header (or query parameter when presigning).
 *
 * <p>V2 required the AWS CRT for SigV4a; this implementation is pure JDK.
 */
public interface AwsV4aHttpSigner extends AwsV4FamilyHttpSigner<AwsCredentialsIdentity> {
    /** The regions the signature is valid in. Required. */
    SignerProperty<RegionSet> REGION_SET = SignerProperty.create(AwsV4aHttpSigner.class, "RegionSet");

    /** The signer. */
    static AwsV4aHttpSigner create() {
        return DefaultAwsV4aHttpSigner.INSTANCE;
    }
}
