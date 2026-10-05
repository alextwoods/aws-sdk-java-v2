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

/**
 * Root package of the {@code software.amazon.awssdk:auth-aws} V3 artifact.
 *
 * <p>Owns: AWS auth scheme implementations such as SigV4 and SigV4a.
 *
 * <p>The launch baseline's Modern Auth schemes (Requirement 13.2): SigV4 ({@link
 * software.amazon.awssdk.auth.aws.AwsV4AuthScheme}), SigV4a ({@link software.amazon.awssdk.auth.aws.AwsV4aAuthScheme},
 * pure JDK), S3 Express ({@code s3express} subpackage), bearer ({@link software.amazon.awssdk.auth.aws.BearerAuthScheme})
 * and the options derived from modeled auth traits ({@link software.amazon.awssdk.auth.aws.AwsAuthSchemeOptions}).
 *
 * <p>Allowed V3 module dependencies: {@code auth-api}, {@code identity-spi}, {@code http-spi}. Forbidden dependencies:
 * generated service artifacts.
 */
package software.amazon.awssdk.auth.aws;
