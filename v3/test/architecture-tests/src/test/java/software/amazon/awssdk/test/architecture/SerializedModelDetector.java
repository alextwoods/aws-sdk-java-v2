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

package software.amazon.awssdk.test.architecture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Recognizes serialized whole-service semantic representations (Requirements 2.1, 3.3, 3.4): the forms in which a
 * model, or a restatement of one, could be handed to a downstream generator or loaded at run time. A file or string
 * is such a representation if it is, or (for JSON) contains at any depth:
 * <ul>
 *   <li>a Smithy IDL model: a {@code .smithy} file, or text with both a {@code $version: "..."} control statement and
 *       a {@code namespace} statement on their own lines;</li>
 *   <li>a Smithy JSON AST: an object with a {@code "smithy"} string and a {@code "shapes"} object;</li>
 *   <li>a C2J service model: an object with a {@code "metadata"} object and an {@code "operations"} or
 *       {@code "shapes"} object (this is also the shape of V2's serialized intermediate model);</li>
 *   <li>a C2J documentation model: an object with {@code "service"} and {@code "operations"} and {@code "shapes"}
 *       objects;</li>
 *   <li>a shape graph: a {@code "shapes"} object or array containing at least one shape definition, that is an object
 *       with a {@code "type"} and structure ({@code members}, {@code member}, {@code key}, {@code value},
 *       {@code input}, {@code output}, {@code errors}, {@code operations}, {@code resources}, {@code target} or
 *       {@code traits});</li>
 *   <li>a Java object-serialization stream (magic {@code 0xACED 0x0005}), whatever it contains;</li>
 *   <li>any of the above compressed with gzip.</li>
 * </ul>
 * Inventories are not representations: a provenance or generation manifest that lists shape IDs, file paths,
 * digests, or {@code {"id", "type"}} entries without member structure or traits restates no semantics and is
 * accepted. JSON is parsed with comments allowed; text that is not valid JSON is only checked for Smithy IDL.
 */
final class SerializedModelDetector {

    private static final Set<String> SHAPE_STRUCTURE = Set.of(
        "members", "member", "key", "value", "input", "output", "errors", "operations", "resources", "target",
        "traits");
    private static final Pattern IDL_VERSION = Pattern.compile("(?m)^\\s*\\$version\\s*:\\s*\"");
    private static final Pattern IDL_NAMESPACE = Pattern.compile("(?m)^\\s*namespace\\s+[A-Za-z_][\\w.]*\\s*$");
    /** Bound on decompressed gzip content, so that a hostile or corrupt archive cannot exhaust the test heap. */
    private static final int MAX_DECOMPRESSED_BYTES = 256 * 1024 * 1024;

    private SerializedModelDetector() {
    }

    /** The kind of serialized representation in the file {@code fileName} with {@code content}, if it is one. */
    static Optional<String> detectFile(String fileName, byte[] content) {
        if (fileName.endsWith(".smithy")) {
            return Optional.of("Smithy IDL model file");
        }
        if (content.length >= 4 && (content[0] & 0xFF) == 0xAC && (content[1] & 0xFF) == 0xED
            && content[2] == 0 && content[3] == 5) {
            return Optional.of("Java object-serialization stream");
        }
        if (content.length >= 2 && (content[0] & 0xFF) == 0x1F && (content[1] & 0xFF) == 0x8B) {
            String inner = fileName.endsWith(".gz") ? fileName.substring(0, fileName.length() - 3) : fileName;
            return gunzip(content).flatMap(bytes -> detectFile(inner, bytes)).map(kind -> "gzip-compressed " + kind);
        }
        return decodeUtf8(content).flatMap(SerializedModelDetector::detectText);
    }

    /** The kind of serialized representation in {@code text} (file content or a class's string constant). */
    static Optional<String> detectText(String text) {
        if (IDL_VERSION.matcher(text).find() && IDL_NAMESPACE.matcher(text).find()) {
            return Optional.of("Smithy IDL model");
        }
        String trimmed = text.strip();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return Optional.empty();
        }
        Node node;
        try {
            node = Node.parseJsonWithComments(trimmed);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        return detectNode(node);
    }

    private static Optional<String> detectNode(Node node) {
        if (node.isArrayNode()) {
            for (Node element : node.expectArrayNode().getElements()) {
                Optional<String> found = detectNode(element);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (!node.isObjectNode()) {
            return Optional.empty();
        }
        ObjectNode o = node.expectObjectNode();
        if (isString(o, "smithy") && isObject(o, "shapes")) {
            return Optional.of("Smithy JSON AST model");
        }
        if (isObject(o, "metadata") && (isObject(o, "operations") || isObject(o, "shapes"))) {
            return Optional.of("C2J service model (or V2 intermediate model)");
        }
        if (o.containsMember("service") && isObject(o, "operations") && isObject(o, "shapes")) {
            return Optional.of("C2J documentation model");
        }
        Optional<Node> shapes = o.getMember("shapes");
        if (shapes.isPresent() && containsShapeDefinition(shapes.get())) {
            return Optional.of("serialized shape graph");
        }
        for (Node value : o.getMembers().values()) {
            Optional<String> found = detectNode(value);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static boolean containsShapeDefinition(Node shapes) {
        Collection<Node> candidates = List.of();
        if (shapes.isObjectNode()) {
            candidates = shapes.expectObjectNode().getMembers().values();
        } else if (shapes.isArrayNode()) {
            candidates = shapes.expectArrayNode().getElements();
        }
        for (Node candidate : candidates) {
            if (candidate.isObjectNode()) {
                ObjectNode shape = candidate.expectObjectNode();
                if (shape.containsMember("type")
                    && shape.getMembers().keySet().stream().anyMatch(k -> SHAPE_STRUCTURE.contains(k.getValue()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isObject(ObjectNode o, String member) {
        return o.getMember(member).map(Node::isObjectNode).orElse(false);
    }

    private static boolean isString(ObjectNode o, String member) {
        return o.getMember(member).map(Node::isStringNode).orElse(false);
    }

    private static Optional<String> decodeUtf8(byte[] content) {
        try {
            return Optional.of(StandardCharsets.UTF_8.newDecoder()
                                                     .onMalformedInput(CodingErrorAction.REPORT)
                                                     .onUnmappableCharacter(CodingErrorAction.REPORT)
                                                     .decode(ByteBuffer.wrap(content)).toString());
        } catch (CharacterCodingException e) {
            return Optional.empty(); // binary content that is not one of the binary forms above
        }
    }

    private static Optional<byte[]> gunzip(byte[] content) {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(content))) {
            byte[] bytes = in.readNBytes(MAX_DECOMPRESSED_BYTES + 1);
            if (bytes.length > MAX_DECOMPRESSED_BYTES) {
                throw new IllegalStateException("gzip resource decompresses to more than " + MAX_DECOMPRESSED_BYTES
                                                + " bytes");
            }
            return Optional.of(bytes);
        } catch (IOException e) {
            return Optional.empty(); // not actually gzip
        }
    }
}
