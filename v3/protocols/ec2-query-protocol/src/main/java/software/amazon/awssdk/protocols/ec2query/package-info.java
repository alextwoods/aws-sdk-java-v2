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
 * Root package of the {@code software.amazon.awssdk:ec2-query-protocol} V3 artifact.
 *
 * <p>Owns: The schema-driven EC2 Query protocol implementation
 * ({@link software.amazon.awssdk.protocols.ec2query.Ec2QueryProtocolProvider}, registered as a
 * {@code ProtocolProvider} service entry): the {@code ec2Query} form-key rules
 * ({@link software.amazon.awssdk.protocols.ec2query.Ec2QueryFormRules}) over the query runtime of
 * {@code aws-query-protocol}. Streaming operations are not supported by the query protocols.
 *
 * <p>Allowed V3 module dependencies: {@code protocol-core}, {@code aws-query-protocol}, {@code smithy-runtime},
 * {@code http-spi}, {@code aws-core}. Forbidden dependencies: per-service generated marshallers.
 */
package software.amazon.awssdk.protocols.ec2query;
