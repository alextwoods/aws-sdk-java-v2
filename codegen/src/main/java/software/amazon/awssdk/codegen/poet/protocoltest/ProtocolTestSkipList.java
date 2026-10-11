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

package software.amazon.awssdk.codegen.poet.protocoltest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * The protocol test skip list, read at codegen time: a JSON array of
 * {@code {"id": ..., "reason": ..., "protocol": ..., "service": ...}} entries, where {@code protocol} is optional and
 * limits the entry to cases of that protocol, and {@code service} is optional and limits the entry to the service with
 * that absolute ShapeId, so that one file can serve every service of a module.
 *
 * <p>The format and the validation rules are those of the runtime loader
 * {@code software.amazon.awssdk.protocol.smithytests.ProtocolTestSkips} in {@code smithy-protocol-tests-core}, which
 * codegen cannot depend on. Both reject a missing or blank {@code id} or {@code reason}, unknown properties and repeated
 * {@code (service, id, protocol)} triples; this class parses with the Smithy {@link Node} parser that codegen already uses.
 */
@SdkInternalApi
public final class ProtocolTestSkipList {
    private static final List<String> PROPERTIES = Arrays.asList("id", "reason", "protocol", "service");
    private static final ProtocolTestSkipList EMPTY = new ProtocolTestSkipList(Collections.emptyList());

    private final List<Entry> entries;

    private ProtocolTestSkipList(List<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public static ProtocolTestSkipList empty() {
        return EMPTY;
    }

    /**
     * Reads the skip list from a file.
     *
     * @throws ProtocolTestCodegenException if the file cannot be read or is not a valid skip list
     */
    public static ProtocolTestSkipList load(Path file) {
        String json;
        try {
            json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ProtocolTestCodegenException("Unable to read the protocol test skip list " + file, e);
        }
        return parse(json, file.toString());
    }

    /**
     * Parses a skip list.
     *
     * @param source a description of the input for error messages, for example its file name
     * @throws ProtocolTestCodegenException if the input is not a valid skip list
     */
    public static ProtocolTestSkipList parse(String json, String source) {
        Node root;
        try {
            root = Node.parse(json, source);
        } catch (RuntimeException e) {
            throw new ProtocolTestCodegenException(source + " is not valid JSON: " + e.getMessage(), e);
        }
        Optional<ArrayNode> array = root.asArrayNode();
        if (!array.isPresent()) {
            throw new ProtocolTestCodegenException(source + " must contain a JSON array of skip entries");
        }
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Node> elements = array.get().getElements();
        for (int i = 0; i < elements.size(); i++) {
            Entry entry = parseEntry(elements.get(i), source + " entry " + i);
            if (!seen.add(nullToEmpty(entry.service) + "|" + entry.id + "|" + nullToEmpty(entry.protocol))) {
                throw new ProtocolTestCodegenException(source + " entry " + i + " repeats the skip for id \"" + entry.id
                                                       + "\"" + (entry.protocol == null ? "" : " and protocol "
                                                                                              + entry.protocol)
                                                       + (entry.service == null ? "" : " and service " + entry.service));
            }
            entries.add(entry);
        }
        return new ProtocolTestSkipList(entries);
    }

    private static Entry parseEntry(Node node, String where) {
        Optional<ObjectNode> object = node.asObjectNode();
        if (!object.isPresent()) {
            throw new ProtocolTestCodegenException(where + " must be a JSON object");
        }
        for (StringNode key : object.get().getMembers().keySet()) {
            String name = key.getValue();
            if (!PROPERTIES.contains(name)) {
                throw new ProtocolTestCodegenException(where + " has unknown property \"" + name
                                                       + "\"; allowed properties are id, reason, protocol and service");
            }
        }
        String id = requiredString(object.get(), "id", where);
        String reason = requiredString(object.get(), "reason", where + " (id \"" + id + "\")");
        String protocol = object.get().getMember("protocol").isPresent()
                          ? requiredString(object.get(), "protocol", where + " (id \"" + id + "\")")
                          : null;
        String service = object.get().getMember("service").isPresent()
                         ? requiredString(object.get(), "service", where + " (id \"" + id + "\")")
                         : null;
        return new Entry(id, reason, protocol, service);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String requiredString(ObjectNode object, String name, String where) {
        Optional<Node> value = object.getMember(name);
        if (!value.isPresent() || !value.get().isStringNode()) {
            throw new ProtocolTestCodegenException(where + " must have a string \"" + name + "\"");
        }
        String text = value.get().expectStringNode().getValue();
        if (text.trim().isEmpty()) {
            throw new ProtocolTestCodegenException(where + " has an empty \"" + name + "\"");
        }
        return text;
    }

    public List<Entry> entries() {
        return entries;
    }

    /**
     * The entries that apply to the service with absolute ShapeId {@code serviceId}: those limited to it and those with
     * no {@code service}.
     */
    public ProtocolTestSkipList forService(String serviceId) {
        List<Entry> applicable = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.service == null || entry.service.equals(serviceId)) {
                applicable.add(entry);
            }
        }
        return applicable.size() == entries.size() ? this : new ProtocolTestSkipList(applicable);
    }

    /**
     * The entry that skips case {@code caseId} under {@code protocol}. An entry limited to {@code protocol} wins over one
     * without a protocol.
     */
    public Optional<Entry> find(String caseId, String protocol) {
        Entry anyProtocol = null;
        for (Entry entry : entries) {
            if (entry.id.equals(caseId)) {
                if (entry.protocol == null) {
                    anyProtocol = entry;
                } else if (entry.protocol.equals(protocol)) {
                    return Optional.of(entry);
                }
            }
        }
        return Optional.ofNullable(anyProtocol);
    }

    /**
     * Fails naming every entry whose id is not a selected test case, or whose protocol is not one the case is defined
     * for.
     *
     * @param knownCases every selected test case id, mapped to the protocol ShapeIds it is defined for
     */
    public void validateAgainst(Map<String, ? extends Collection<String>> knownCases) {
        List<String> unknown = new ArrayList<>();
        for (Entry entry : entries) {
            Collection<String> protocols = knownCases.get(entry.id);
            boolean known = protocols != null && (entry.protocol == null || protocols.contains(entry.protocol));
            if (!known) {
                unknown.add(entry.toString());
            }
        }
        if (!unknown.isEmpty()) {
            throw new ProtocolTestCodegenException("The protocol test skip list names test cases that do not exist: "
                                                   + String.join(", ", unknown));
        }
    }

    /**
     * One skip list entry.
     */
    public static final class Entry {
        private final String id;
        private final String reason;
        private final String protocol;
        private final String service;

        private Entry(String id, String reason, String protocol, String service) {
            this.id = id;
            this.reason = reason;
            this.protocol = protocol;
            this.service = service;
        }

        public String id() {
            return id;
        }

        public String reason() {
            return reason;
        }

        /**
         * The protocol ShapeId this entry is limited to, or null when it applies to every protocol.
         */
        public String protocol() {
            return protocol;
        }

        /**
         * The absolute service ShapeId this entry is limited to, or null when it applies to every service.
         */
        public String service() {
            return service;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry that = (Entry) o;
            return id.equals(that.id) && reason.equals(that.reason) && Objects.equals(protocol, that.protocol)
                   && Objects.equals(service, that.service);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + reason.hashCode();
            result = 31 * result + Objects.hashCode(protocol);
            return 31 * result + Objects.hashCode(service);
        }

        @Override
        public String toString() {
            return "{id=" + id + (protocol == null ? "" : ", protocol=" + protocol)
                   + (service == null ? "" : ", service=" + service) + ", reason=" + reason + "}";
        }
    }
}
