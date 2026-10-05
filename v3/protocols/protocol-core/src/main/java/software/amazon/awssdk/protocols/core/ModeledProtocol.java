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

package software.amazon.awssdk.protocols.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * One protocol trait applied to a service in its canonical model, with the trait's protocol options as runtime
 * values. AWS protocol trait classes are build-time only, so generated service metadata records these values
 * directly instead of the trait.
 *
 * <p>The options carried are the ones the launch protocol traits define:
 * <ul>
 *   <li>{@code http} and {@code eventStreamHttp}: the ALPN protocol IDs ({@code "h2"}, {@code "http/1.1"}) the
 *       service supports, in the model's preference order, for {@code awsJson1_0}, {@code awsJson1_1},
 *       {@code restJson1}, {@code restXml} and {@code rpcv2Cbor}. Empty when the trait has none (the
 *       protocol then uses HTTP/1.1).</li>
 *   <li>{@code noErrorWrapping} of {@code restXml}.</li>
 * </ul>
 * The AWS JSON version is part of the protocol's identity; see {@link #awsJsonVersion()}.
 *
 * @param id absolute shape ID of the protocol trait
 * @param http the trait's {@code http} list; empty if absent
 * @param eventStreamHttp the trait's {@code eventStreamHttp} list; empty if absent
 * @param noErrorWrapping the {@code restXml} {@code noErrorWrapping} flag; {@code false} for every other protocol
 */
public record ModeledProtocol(ShapeId id, List<String> http, List<String> eventStreamHttp, boolean noErrorWrapping) {

    public ModeledProtocol {
        Objects.requireNonNull(id, "id");
        if (id.hasMember()) {
            throw new IllegalArgumentException("A protocol is identified by a trait shape ID, not the member ID " + id);
        }
        http = List.copyOf(http);
        eventStreamHttp = List.copyOf(eventStreamHttp);
        if (noErrorWrapping && !id.equals(ProtocolIds.REST_XML)) {
            throw new IllegalArgumentException("noErrorWrapping is an option of " + ProtocolIds.REST_XML + ", not "
                                               + id);
        }
    }

    /** A protocol trait without options. */
    public static ModeledProtocol of(ShapeId id) {
        return new ModeledProtocol(id, List.of(), List.of(), false);
    }

    /**
     * {@code "1.0"} for {@code awsJson1_0}, {@code "1.1"} for {@code awsJson1_1}, empty for every other protocol. The
     * AWS JSON protocols differ on the wire only in this version ({@code application/x-amz-json-<version>}).
     */
    public Optional<String> awsJsonVersion() {
        if (id.equals(ProtocolIds.AWS_JSON_1_0)) {
            return Optional.of("1.0");
        }
        if (id.equals(ProtocolIds.AWS_JSON_1_1)) {
            return Optional.of("1.1");
        }
        return Optional.empty();
    }
}
