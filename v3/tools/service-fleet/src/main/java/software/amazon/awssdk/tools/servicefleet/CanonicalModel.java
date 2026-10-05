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

package software.amazon.awssdk.tools.servicefleet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * One service model of the canonical aws-models repository: {@code <directory>/smithy/*.json}, identified by the
 * service shape it defines and that shape's {@code aws.api#service} SDK ID.
 *
 * @param directory the model's top-level directory in the repository (for example {@code monitoring})
 * @param serviceShapeId the absolute shape ID of the service, or {@code null} if no file defines exactly one service
 * @param sdkId the service's {@code aws.api#service} {@code sdkId}, or {@code null}
 * @param protocolModules the V3 protocol modules of the service's modeled protocol traits, sorted
 * @param unknownProtocols modeled protocol-like traits without a V3 protocol module, sorted
 * @param files the model files, relative to {@code <directory>/smithy}, with their digests, sorted by path
 * @param modelDigest the location-independent digest of {@code files} that generation records
 */
public record CanonicalModel(String directory, String serviceShapeId, String sdkId, List<String> protocolModules,
                             List<String> unknownProtocols, List<ModelFile> files, String modelDigest) {

    /** The V3 protocol module of each launch protocol trait (Requirement 5.5). */
    public static final Map<String, String> PROTOCOL_MODULES = Map.of(
        "aws.protocols#awsJson1_0", "aws-json-protocol",
        "aws.protocols#awsJson1_1", "aws-json-protocol",
        "aws.protocols#restJson1", "rest-json-protocol",
        "aws.protocols#restXml", "rest-xml-protocol",
        "aws.protocols#awsQuery", "aws-query-protocol",
        "aws.protocols#ec2Query", "ec2-query-protocol",
        "smithy.protocols#rpcv2Cbor", "rpcv2-cbor-protocol");

    public CanonicalModel {
        Objects.requireNonNull(directory, "directory");
        protocolModules = List.copyOf(protocolModules);
        unknownProtocols = List.copyOf(unknownProtocols);
        files = List.copyOf(files);
    }

    /**
     * A model file and the {@code sha256:<hex>} digest of its content.
     *
     * @param path the path relative to the model directory, with {@code /} separators
     * @param digest the content digest
     */
    public record ModelFile(String path, String digest) {
        public ModelFile {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(digest, "digest");
        }
    }

    /** Every service model of the repository, sorted by directory. */
    public static List<CanonicalModel> scan(Path repository) {
        List<CanonicalModel> models = new ArrayList<>();
        try (Stream<Path> list = Files.list(repository)) {
            for (Path directory : list.filter(d -> Files.isDirectory(d.resolve("smithy"))).sorted().toList()) {
                models.add(read(directory.getFileName().toString(), directory.resolve("smithy")));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return models;
    }

    /** The service model in {@code smithyDirectory}. */
    static CanonicalModel read(String directory, Path smithyDirectory) {
        List<ModelFile> files = files(smithyDirectory);
        String serviceShapeId = null;
        String sdkId = null;
        int services = 0;
        TreeSet<String> protocols = new TreeSet<>();
        TreeSet<String> unknown = new TreeSet<>();
        for (ModelFile file : files) {
            if (!file.path().endsWith(".json")) {
                continue;
            }
            ObjectNode shapes = Node.parse(V2Service.read(smithyDirectory.resolve(file.path()))).expectObjectNode()
                                    .getObjectMember("shapes").orElse(Node.objectNode());
            for (Map.Entry<String, Node> shape : shapes.getStringMap().entrySet()) {
                ObjectNode definition = shape.getValue().expectObjectNode();
                if (!"service".equals(definition.expectStringMember("type").getValue())) {
                    continue;
                }
                services++;
                serviceShapeId = shape.getKey();
                ObjectNode traits = definition.getObjectMember("traits").orElse(Node.objectNode());
                sdkId = traits.getObjectMember("aws.api#service").flatMap(t -> t.getStringMember("sdkId"))
                              .map(s -> s.getValue()).orElse(null);
                for (String trait : traits.getStringMap().keySet()) {
                    String module = PROTOCOL_MODULES.get(trait);
                    if (module != null) {
                        protocols.add(module);
                    } else if (trait.startsWith("aws.protocols#") && !trait.equals("aws.protocols#awsQueryCompatible")) {
                        unknown.add(trait);
                    }
                }
            }
        }
        if (services != 1) {
            serviceShapeId = null;
            sdkId = null;
        }
        return new CanonicalModel(directory, serviceShapeId, sdkId, List.copyOf(protocols), List.copyOf(unknown),
                                  files, modelDigest(smithyDirectory, files));
    }

    /** The files of {@code directory} (recursively) with their digests, sorted by portable relative path. */
    static List<ModelFile> files(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(Files::isRegularFile)
                       .map(f -> new ModelFile(directory.relativize(f).toString().replace('\\', '/'),
                                               sha256(readBytes(f))))
                       .sorted((a, b) -> a.path().compareTo(b.path()))
                       .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The location-independent digest {@code aws-codegen} records for a model directory: SHA-256 over, for every
     * file in path order, the length-prefixed UTF-8 path and the length-prefixed content.
     */
    static String modelDigest(Path directory, List<ModelFile> files) {
        MessageDigest sha256 = sha256();
        for (ModelFile file : files) {
            byte[] name = file.path().getBytes(StandardCharsets.UTF_8);
            byte[] content = readBytes(directory.resolve(file.path()));
            sha256.update(ByteBuffer.allocate(Integer.BYTES).putInt(name.length).array());
            sha256.update(name);
            sha256.update(ByteBuffer.allocate(Long.BYTES).putLong(content.length).array());
            sha256.update(content);
        }
        return "sha256:" + HexFormat.of().formatHex(sha256.digest());
    }

    static String sha256(byte[] content) {
        return "sha256:" + HexFormat.of().formatHex(sha256().digest(content));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    static byte[] readBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
    }
}
