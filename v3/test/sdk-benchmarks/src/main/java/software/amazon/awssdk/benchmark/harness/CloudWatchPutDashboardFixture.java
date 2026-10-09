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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The SDK-free wire half of the rpcv2Cbor fixture (CloudWatch {@code PutDashboard} on the rpcv2Cbor route
 * {@code /service/<service>/operation/<op>} with {@code smithy-protocol: rpc-v2-cbor}): the {@code DashboardBody} is
 * the pad; the success response echoes it as a dashboard validation message. Error cells get the modeled
 * {@code DashboardInvalidInputError} (400); retry cells the modeled, retryable {@code InternalServiceFault} (500).
 *
 * <p>Reference-specific request expectation: the Launch Parity Baseline (V2 2.54.0, whose CloudWatch client speaks
 * rpcv2Cbor through its Jackson CBOR generator) encodes the same request, with the same members in the same order, but
 * writes a text string longer than {@value #V2_BASELINE_TEXT_CHUNK} characters as an indefinite-length text string
 * (0x7F, definite chunks of at most {@value #V2_BASELINE_TEXT_CHUNK} characters, 0xFF) where V3 writes one definite
 * string. {@link #v2BaselineRequest(PaddedFixture)} is exactly that encoding (the V2 reference's wire test proves it
 * byte for byte for every payload class), so the V2 arm declares its own exact encoded bytes and the wire-bytes rule
 * applies to it unchanged; the mock service and every response are shared.
 */
public final class CloudWatchPutDashboardFixture {
    public static final String NAME = "cloudwatch-putdashboard";
    public static final String DASHBOARD = "benchmark-dashboard";
    public static final String CONTENT_TYPE = "application/cbor";
    public static final String PATH = "/service/GraniteServiceVersion20100801/operation/PutDashboard";
    public static final Map<String, String> HEADERS = Map.of("smithy-protocol", "rpc-v2-cbor");
    /** The Launch Parity Baseline's CBOR text chunk length (characters); see the class comment. */
    public static final int V2_BASELINE_TEXT_CHUNK = 3996;

    private CloudWatchPutDashboardFixture() {
    }

    public static PaddedFixture create(PayloadClass payload) {
        return new PaddedFixture(
            payload, new PaddedFixture.Route("POST", PATH, "smithy-protocol", "rpc-v2-cbor", null), 1,
            // The client encodes the input as an indefinite-length map (0xBF ... 0xFF).
            pad -> new Cbor().indefiniteMap().text("DashboardName").text(DASHBOARD).text("DashboardBody").text(pad)
                             .breakCode().bytes(),
            pad -> response(200, CONTENT_TYPE, new Cbor().map(1).text("DashboardValidationMessages").array(1).map(2)
                                                         .text("DataPath").text("$").text("Message").text(pad)
                                                         .bytes(), HEADERS),
            response(400, CONTENT_TYPE, error("DashboardInvalidInputError", "Invalid dashboard body"), HEADERS),
            response(500, CONTENT_TYPE, error("InternalServiceFault", "injected retry"), HEADERS));
    }

    /** The exact request body the Launch Parity Baseline (V2 2.54.0) sends for {@code fixture}'s pad. */
    public static byte[] v2BaselineRequest(PaddedFixture fixture) {
        return new Cbor().indefiniteMap().text("DashboardName").text(DASHBOARD).text("DashboardBody")
                         .chunkedText(fixture.pad(), V2_BASELINE_TEXT_CHUNK).breakCode().bytes();
    }

    /**
     * The Launch Parity Baseline's encoded bytes of one logical operation ({@link #v2BaselineRequest} plus the shared
     * success response), asserted to lie in the fixture's payload class.
     */
    public static long v2BaselineEncodedBytes(PaddedFixture fixture, PayloadClass payload) {
        long bytes = v2BaselineRequest(fixture).length + (long) fixture.success().body().length;
        if (!payload.contains(bytes)) {
            throw new IllegalStateException("V2 baseline fixture of " + bytes + " encoded bytes is not "
                                            + payload.id());
        }
        return bytes;
    }

    private static byte[] error(String name, String message) {
        return new Cbor().map(2).text("__type").text("com.amazonaws.cloudwatch#" + name).text("message").text(message)
                         .bytes();
    }

    /** A minimal CBOR writer (RFC 8949) for the fixture's maps, arrays and text strings. */
    static final class Cbor {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Cbor map(int entries) {
            return head(5, entries);
        }

        Cbor indefiniteMap() {
            out.write(0xBF);
            return this;
        }

        Cbor breakCode() {
            out.write(0xFF);
            return this;
        }

        /** {@code s} as one definite text string, or, if longer than {@code chunk}, as definite chunks of it. */
        Cbor chunkedText(String s, int chunk) {
            if (s.length() <= chunk) {
                return text(s);
            }
            out.write(0x7F);
            for (int i = 0; i < s.length(); i += chunk) {
                text(s.substring(i, Math.min(s.length(), i + chunk)));
            }
            return breakCode();
        }

        Cbor array(int elements) {
            return head(4, elements);
        }

        Cbor text(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            head(3, b.length);
            out.writeBytes(b);
            return this;
        }

        private Cbor head(int major, long n) {
            int m = major << 5;
            if (n < 24) {
                out.write(m | (int) n);
            } else if (n < 0x100) {
                out.write(m | 24);
                out.write((int) n);
            } else if (n < 0x10000) {
                out.write(m | 25);
                out.write((int) (n >>> 8));
                out.write((int) n);
            } else {
                out.write(m | 26);
                for (int shift = 24; shift >= 0; shift -= 8) {
                    out.write((int) (n >>> shift));
                }
            }
            return this;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }
}
