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

package software.amazon.awssdk.dynamicclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The reference model of the Dynamic Client SEP's Testing section ({@code sprockets.smithy}), with a DC case's "Given"
 * traits applied and its protocol trait set to one or more Supported_Protocols.
 *
 * <p>The reference model is an awsJson1_0 service. Each other protocol needs some traits for the model to be valid:
 * <ul>
 *     <li>restJson1 and restXml: {@code @http} on every operation and {@code @httpPayload} on every streaming member
 *     (Smithy's {@code StreamingTrait} validation). Smithy then rejects {@code UploadSprocket}'s unbound
 *     {@code checksum: Blob} ({@code HttpPayload} validation), and no HTTP binding trait accepts a blob, so the member
 *     becomes {@code @httpHeader("x-amz-sprocket-checksum") checksum: String};</li>
 *     <li>awsQuery and ec2Query: {@code @xmlNamespace} on the service.</li>
 * </ul>
 * These are applied with {@code apply} statements, except the {@code checksum} change, which edits the text.
 */
final class SprocketsModels {
    static final ShapeId SERVICE = ShapeId.from("smithy.example#Sprockets");

    private static final String REFERENCE_TEXT = read("sprockets.smithy");
    private static final String REFERENCE_PROTOCOL = "@aws.protocols#awsJson1_0";
    private static final String UNBOUND_CHECKSUM = "        checksum: Blob";
    private static final String HEADER_CHECKSUM = "        @httpHeader(\"x-amz-sprocket-checksum\")\n"
                                                  + "        checksum: String";
    private static final List<String> HTTP_BINDINGS = Arrays.asList(
        "apply CreateSprocket @http(method: \"POST\", uri: \"/sprockets\")",
        "apply GetSprocket @http(method: \"POST\", uri: \"/get-sprocket\")",
        "apply TagSprocket @http(method: \"POST\", uri: \"/tag-sprocket\")",
        "apply UploadSprocket @http(method: \"PUT\", uri: \"/upload-sprocket\")",
        "apply DownloadSprocket @http(method: \"POST\", uri: \"/download-sprocket\")",
        "apply WatchSprockets @http(method: \"POST\", uri: \"/watch-sprockets\")",
        "apply SendSprocketCommands @http(method: \"POST\", uri: \"/send-sprocket-commands\")",
        "apply SyncSprockets @http(method: \"POST\", uri: \"/sync-sprockets\")",
        "apply UploadSprocketInput$data @httpPayload",
        "apply DownloadSprocketOutput$data @httpPayload",
        "apply WatchSprocketsOutput$events @httpPayload",
        "apply SendSprocketCommandsInput$commands @httpPayload",
        "apply SyncSprocketsInput$commands @httpPayload",
        "apply SyncSprocketsOutput$events @httpPayload");
    private static final String XML_NAMESPACE =
        "apply Sprockets @xmlNamespace(uri: \"" + SprocketsProtocol.XML_NAMESPACE + "\")";

    private static final Map<SprocketsProtocol, Model> BY_PROTOCOL = new ConcurrentHashMap<>();

    private SprocketsModels() {
    }

    /**
     * The reference model as the SEP states it (awsJson1_0).
     */
    static Model reference() {
        return forProtocol(SprocketsProtocol.AWS_JSON_1_0);
    }

    /**
     * The reference model with the protocol trait of {@code protocol} in place of awsJson1_0, and no other Given
     * traits. Assembled once per protocol.
     */
    static Model forProtocol(SprocketsProtocol protocol) {
        return BY_PROTOCOL.computeIfAbsent(protocol, p -> builder().protocols(p).build());
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a variant of the reference model.
     */
    static final class Builder {
        private final List<SprocketsProtocol> protocols = new ArrayList<>();
        private final List<String> given = new ArrayList<>();
        private final List<String> models = new ArrayList<>();
        private String protocolTraits;
        private String text = REFERENCE_TEXT;

        private Builder() {
        }

        /**
         * Sets the service's protocol traits, in this textual order, with the traits each protocol needs.
         */
        Builder protocols(SprocketsProtocol... values) {
            protocols.addAll(Arrays.asList(values));
            return this;
        }

        /**
         * Replaces the reference model's protocol trait with this IDL text, and applies nothing else for it.
         */
        Builder protocolTraits(String idl) {
            protocolTraits = idl;
            return this;
        }

        /**
         * Adds IDL statements in the reference model's namespace, typically {@code apply} statements.
         */
        Builder given(String... statements) {
            given.addAll(Arrays.asList(statements));
            return this;
        }

        /**
         * Adds a complete IDL model file, for shapes of another namespace.
         */
        Builder model(String idl) {
            models.add(idl);
            return this;
        }

        /**
         * Replaces text of the reference model that cannot be changed with traits, such as a member.
         */
        Builder edit(String target, String replacement) {
            if (!text.contains(target)) {
                throw new IllegalArgumentException("The reference model does not contain: " + target);
            }
            text = text.replace(target, replacement);
            return this;
        }

        Model build() {
            List<String> statements = new ArrayList<>();
            String model = text;
            if (protocolTraits != null) {
                model = model.replace(REFERENCE_PROTOCOL, protocolTraits);
            } else if (!protocols.isEmpty()) {
                model = model.replace(REFERENCE_PROTOCOL, protocols.stream()
                                                                   .map(p -> "@" + p.traitId())
                                                                   .collect(Collectors.joining("\n")));
                if (protocols.stream().anyMatch(SprocketsProtocol::httpBindings)) {
                    model = model.replace(UNBOUND_CHECKSUM, HEADER_CHECKSUM);
                    statements.addAll(HTTP_BINDINGS);
                }
                if (protocols.stream().anyMatch(SprocketsProtocol::query)) {
                    statements.add(XML_NAMESPACE);
                }
            }
            statements.addAll(given);
            ModelAssembler assembler = Model.assembler()
                                            .discoverModels(Model.class.getClassLoader())
                                            .addUnparsedModel("sprockets.smithy", model);
            if (!statements.isEmpty()) {
                assembler.addUnparsedModel("given.smithy", "$version: \"2\"\nnamespace smithy.example\n"
                                                           + String.join("\n", statements) + "\n");
            }
            for (int i = 0; i < models.size(); i++) {
                assembler.addUnparsedModel("given-" + i + ".smithy", models.get(i));
            }
            return assembler.assemble().unwrap();
        }
    }

    private static String read(String resource) {
        try (InputStream in = SprocketsModels.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Test model " + resource + " not found");
            }
            return new String(IoUtils.toByteArray(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
