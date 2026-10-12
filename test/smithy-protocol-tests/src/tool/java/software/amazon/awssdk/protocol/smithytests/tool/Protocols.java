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

package software.amazon.awssdk.protocol.smithytests.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** The Supported_Protocols by ShapeId (Requirement 32.1). */
final class Protocols {

    static final String AWS_JSON_1_0 = "aws.protocols#awsJson1_0";
    static final String AWS_JSON_1_1 = "aws.protocols#awsJson1_1";
    static final String REST_JSON = "aws.protocols#restJson1";
    static final String REST_XML = "aws.protocols#restXml";
    static final String AWS_QUERY = "aws.protocols#awsQuery";
    static final String EC2_QUERY = "aws.protocols#ec2Query";
    static final String RPCV2_CBOR = "smithy.protocols#rpcv2Cbor";

    static final Set<String> SUPPORTED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        AWS_JSON_1_0, AWS_JSON_1_1, REST_JSON, REST_XML, AWS_QUERY, EC2_QUERY, RPCV2_CBOR)));

    private Protocols() {
    }

    static boolean hasHttpBindings(String protocol) {
        return REST_JSON.equals(protocol) || REST_XML.equals(protocol);
    }
}
