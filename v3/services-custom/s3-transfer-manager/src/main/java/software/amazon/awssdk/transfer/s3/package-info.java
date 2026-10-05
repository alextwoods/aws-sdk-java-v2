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
 * Root package of the {@code software.amazon.awssdk:s3-transfer-manager} V3 artifact: the
 * {@link software.amazon.awssdk.transfer.s3.S3TransferManager}, with the V2 package layout ({@code config},
 * {@code model}, {@code progress}).
 *
 * <p>Owns: the S3 Transfer Manager high-level library, built only on public V3 APIs.
 *
 * <p>Allowed V3 module dependencies: {@code aws-core}, {@code sdk-core}, {@code smithy-runtime} and the generated
 * {@code s3} service. Forbidden dependencies: V2 and bridge internals, concrete HTTP implementations, logging
 * implementations. Classes under {@code software.amazon.awssdk.transfer.s3.internal} are not a public API.
 */
package software.amazon.awssdk.transfer.s3;
