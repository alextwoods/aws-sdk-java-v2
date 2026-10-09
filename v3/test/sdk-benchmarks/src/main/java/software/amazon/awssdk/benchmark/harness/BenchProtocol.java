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

package software.amazon.awssdk.benchmark.harness;

/**
 * The seven V3 launch protocols (Req 20.1), with the streaming capabilities V3's protocol runtime gives each one
 * ({@code StreamingPayloads.streamingRejection}): streaming blob payloads only on the HTTP payload binding protocols,
 * Event Streams on every protocol except the query protocols.
 */
public enum BenchProtocol {
    AWS_JSON_1_0("awsJson1_0", "aws.protocols#awsJson1_0", false, true),
    AWS_JSON_1_1("awsJson1_1", "aws.protocols#awsJson1_1", false, true),
    REST_JSON_1("restJson1", "aws.protocols#restJson1", true, true),
    REST_XML("restXml", "aws.protocols#restXml", true, true),
    AWS_QUERY("awsQuery", "aws.protocols#awsQuery", false, false),
    EC2_QUERY("ec2Query", "aws.protocols#ec2Query", false, false),
    RPC_V2_CBOR("rpcv2Cbor", "smithy.protocols#rpcv2Cbor", false, true);

    private final String id;
    private final String shapeId;
    private final boolean streamingBlobs;
    private final boolean eventStreams;

    BenchProtocol(String id, String shapeId, boolean streamingBlobs, boolean eventStreams) {
        this.id = id;
        this.shapeId = shapeId;
        this.streamingBlobs = streamingBlobs;
        this.eventStreams = eventStreams;
    }

    /** The cell-id token, e.g. {@code awsJson1_0}. */
    public String id() {
        return id;
    }

    /** The Smithy protocol trait shape ID. */
    public String shapeId() {
        return shapeId;
    }

    /** True if the protocol binds streaming blob payloads, so streaming cells apply. */
    public boolean supportsStreamingBlobs() {
        return streamingBlobs;
    }

    /** True if the protocol supports Event Streams, so Event Stream cells apply. */
    public boolean supportsEventStreams() {
        return eventStreams;
    }

    public static BenchProtocol fromId(String id) {
        for (BenchProtocol p : values()) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        throw new IllegalArgumentException("unknown protocol: " + id);
    }
}
