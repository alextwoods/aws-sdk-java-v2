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
 * Root package of the {@code software.amazon.awssdk:aws-core} V3 artifact.
 *
 * <p>Owns: AwsClient, AwsClientConfiguration, region/partition/configuration providers, identity APIs, and AWS
 * metadata.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}, {@code smithy-runtime}, {@code identity-spi},
 * {@code regions}. Forbidden dependencies: concrete HTTP
 * engines.
 */
package software.amazon.awssdk.awscore;
