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
 * The IAM Policy Builder: build, read ({@link software.amazon.awssdk.policybuilder.iam.IamPolicyReader}) and write
 * ({@link software.amazon.awssdk.policybuilder.iam.IamPolicyWriter}) IAM policy documents.
 *
 * <p>Root package of the {@code software.amazon.awssdk:iam-policy-builder} V3 artifact. Its public API is the V2
 * policy builder API, classified {@code PRESERVED} in the Parity Manifest: the same fully qualified types, members,
 * JVM descriptors and builder hierarchy, the same validation, equality, hash and string rendering, the same accepted
 * documents and byte-identical JSON output.
 *
 * <p>Owns: The IAM Policy Builder high-level library.
 *
 * <p>Allowed V3 module dependencies: {@code aws-core}, {@code sdk-core}. Forbidden dependencies: V2 and bridge
 * internals, and JSON libraries (JSON reading and writing is internal).
 */
package software.amazon.awssdk.policybuilder.iam;
