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

import static software.amazon.awssdk.benchmark.harness.PaddedFixture.response;
import static software.amazon.awssdk.benchmark.harness.PaddedFixture.utf8;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The SDK-free wire half of the awsJson1.1 fixture (Kinesis {@code PutRecord}): the record's {@code Data} blob (base64
 * on the wire) is the pad; the success response echoes it as the {@code SequenceNumber}. Error cells get the modeled
 * {@code ResourceNotFoundException}; retry cells the modeled, retryable {@code InternalFailureException} (HTTP 500).
 */
public final class KinesisPutRecordFixture {
    public static final String NAME = "kinesis-putrecord";
    public static final String STREAM = "benchmark-stream";
    public static final String KEY = "benchmark-key";
    public static final String TARGET = "Kinesis_20131202.PutRecord";
    public static final String CONTENT_TYPE = "application/x-amz-json-1.1";

    private KinesisPutRecordFixture() {
    }

    public static PaddedFixture create(PayloadClass payload) {
        return new PaddedFixture(
            payload, new PaddedFixture.Route("POST", "/", "X-Amz-Target", TARGET, null), 3,
            pad -> utf8("{\"StreamName\":\"" + STREAM + "\",\"Data\":\""
                        + Base64.getEncoder().encodeToString(pad.getBytes(StandardCharsets.UTF_8))
                        + "\",\"PartitionKey\":\"" + KEY + "\"}"),
            pad -> response(200, CONTENT_TYPE, "{\"ShardId\":\"shardId-000000000000\",\"SequenceNumber\":\"" + pad
                                               + "\"}"),
            response(400, CONTENT_TYPE, "{\"__type\":\"ResourceNotFoundException\",\"message\":\"Stream not found\"}"),
            response(500, CONTENT_TYPE, "{\"__type\":\"InternalFailureException\",\"message\":\"injected retry\"}"));
    }
}
