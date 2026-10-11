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

package software.amazon.awssdk.protocol.smithytests;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.Validate;

/**
 * The protocol test skip list, loaded from {@code protocol-test-skips.json}.
 *
 * <p>The file is a JSON array of objects:
 * <pre>{@code
 * [
 *   { "id": "RestJsonHttpPayloadWithUnion", "reason": "Unions in payloads are not supported yet" },
 *   { "id": "SomeSharedCaseId", "reason": "...", "protocol": "aws.protocols#awsJson1_0" }
 * ]
 * }</pre>
 *
 * <p>Each entry needs a non-blank {@code id} and {@code reason}; {@code protocol} is optional and limits the skip to one
 * protocol, and {@code service} is optional and limits it to the service with that absolute ShapeId, so that one file
 * can serve every service of a module. Loading fails with an {@link IllegalArgumentException} naming the entry if an
 * entry is malformed, has an unknown property, or repeats an earlier {@code (service, id, protocol)} triple. Use
 * {@link #validateAgainst} to reject entries whose case does not exist.
 */
public final class ProtocolTestSkips {
    private static final String FILE_NAME = "protocol-test-skips.json";
    private static final JsonNodeParser PARSER = JsonNodeParser.create();
    private static final Set<String> KNOWN_PROPERTIES = new HashSet<>(Arrays.asList("id", "reason", "protocol", "service"));
    private static final ProtocolTestSkips EMPTY = new ProtocolTestSkips(Collections.emptyList());

    private final List<ProtocolTestSkip> entries;

    private ProtocolTestSkips(List<ProtocolTestSkip> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * The conventional file name of the skip list, {@code protocol-test-skips.json}.
     */
    public static String fileName() {
        return FILE_NAME;
    }

    /**
     * A skip list with no entries.
     */
    public static ProtocolTestSkips empty() {
        return EMPTY;
    }

    /**
     * Load the skip list from a file.
     */
    public static ProtocolTestSkips load(Path file) {
        Validate.paramNotNull(file, "file");
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the protocol test skip list " + file, e);
        }
    }

    /**
     * Load the skip list from a stream. The stream is read to the end but not closed.
     *
     * @param source a description of the stream used in error messages, for example a file name
     */
    public static ProtocolTestSkips load(InputStream in, String source) {
        Validate.paramNotNull(in, "in");
        JsonNode root;
        try {
            root = PARSER.parse(in);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(source + " is not valid JSON: " + e.getMessage(), e);
        }
        return fromJson(root, source);
    }

    /**
     * Parse the skip list from a JSON string.
     */
    public static ProtocolTestSkips fromJson(String json) {
        Validate.paramNotNull(json, "json");
        JsonNode root;
        try {
            root = PARSER.parse(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The protocol test skip list is not valid JSON: " + e.getMessage(), e);
        }
        return fromJson(root, FILE_NAME);
    }

    private static ProtocolTestSkips fromJson(JsonNode root, String source) {
        if (!root.isArray()) {
            throw new IllegalArgumentException(source + " must contain a JSON array of skip entries");
        }
        List<ProtocolTestSkip> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<JsonNode> elements = root.asArray();
        for (int i = 0; i < elements.size(); i++) {
            ProtocolTestSkip entry = parseEntry(elements.get(i), i, source);
            String key = entry.service().orElse("") + "|" + entry.id() + "|" + entry.protocol().orElse("");
            if (!seen.add(key)) {
                throw new IllegalArgumentException(source + " entry " + i + " repeats the skip for id \"" + entry.id() + "\""
                                                   + entry.protocol().map(p -> " and protocol " + p).orElse("")
                                                   + entry.service().map(s -> " and service " + s).orElse(""));
            }
            entries.add(entry);
        }
        return new ProtocolTestSkips(entries);
    }

    private static ProtocolTestSkip parseEntry(JsonNode node, int index, String source) {
        String where = source + " entry " + index;
        if (!node.isObject()) {
            throw new IllegalArgumentException(where + " must be a JSON object");
        }
        Map<String, JsonNode> fields = node.asObject();
        for (String name : fields.keySet()) {
            if (!KNOWN_PROPERTIES.contains(name)) {
                throw new IllegalArgumentException(where + " has unknown property \"" + name
                                                   + "\"; allowed properties are id, reason, protocol and service");
            }
        }
        String id = requiredString(fields, "id", where);
        String reason = requiredString(fields, "reason", where + " (id \"" + id + "\")");
        String protocol = null;
        if (fields.containsKey("protocol")) {
            protocol = requiredString(fields, "protocol", where + " (id \"" + id + "\")");
        }
        String service = null;
        if (fields.containsKey("service")) {
            service = requiredString(fields, "service", where + " (id \"" + id + "\")");
        }
        return ProtocolTestSkip.create(id, reason, protocol, service);
    }

    private static String requiredString(Map<String, JsonNode> fields, String name, String where) {
        JsonNode value = fields.get(name);
        if (value == null || !value.isString()) {
            throw new IllegalArgumentException(where + " must have a string \"" + name + "\"");
        }
        String text = value.asString();
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException(where + " has an empty \"" + name + "\"");
        }
        return text;
    }

    /**
     * All entries, in file order.
     */
    public List<ProtocolTestSkip> entries() {
        return entries;
    }

    /**
     * The entries that apply to the service with absolute ShapeId {@code serviceId}: those limited to it and those with
     * no {@code service}. {@link #find} and {@link #validateAgainst} do not look at {@code service}, so call them on the
     * result of this method when the file serves several services.
     */
    public ProtocolTestSkips forService(String serviceId) {
        Validate.paramNotBlank(serviceId, "serviceId");
        List<ProtocolTestSkip> applicable = new ArrayList<>();
        for (ProtocolTestSkip entry : entries) {
            if (entry.service().map(serviceId::equals).orElse(true)) {
                applicable.add(entry);
            }
        }
        return new ProtocolTestSkips(applicable);
    }

    /**
     * The entry that skips case {@code caseId} under {@code protocol}. An entry limited to {@code protocol} wins over one
     * without a protocol.
     */
    public Optional<ProtocolTestSkip> find(String caseId, String protocol) {
        ProtocolTestSkip anyProtocol = null;
        for (ProtocolTestSkip entry : entries) {
            if (entry.appliesTo(caseId, protocol)) {
                if (entry.protocol().isPresent()) {
                    return Optional.of(entry);
                }
                anyProtocol = entry;
            }
        }
        return Optional.ofNullable(anyProtocol);
    }

    /**
     * Fail if an entry names a case that does not exist.
     *
     * @param knownCases every selected test case id mapped to the protocol ShapeIds it is defined for
     * @throws IllegalArgumentException naming every entry whose id is not a key of {@code knownCases}, or whose protocol is
     *                                  not one of that id's protocols
     */
    public void validateAgainst(Map<String, ? extends Collection<String>> knownCases) {
        Validate.paramNotNull(knownCases, "knownCases");
        List<String> unknown = new ArrayList<>();
        for (ProtocolTestSkip entry : entries) {
            Collection<String> protocols = knownCases.get(entry.id());
            boolean known = protocols != null && entry.protocol().map(protocols::contains).orElse(true);
            if (!known) {
                unknown.add(entry.toString());
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("The protocol test skip list names test cases that do not exist: "
                                               + String.join(", ", unknown));
        }
    }
}
