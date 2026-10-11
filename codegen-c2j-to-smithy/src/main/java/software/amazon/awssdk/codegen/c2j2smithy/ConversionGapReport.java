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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The C2J properties a conversion could not map to a standard Smithy or AWS trait (Requirement 17.10).
 *
 * <p>A report is produced on every conversion. It has zero entries when no gaps exist. Each entry names the C2J
 * property, the services it affects, the generated-code impact, and how many times the property occurred. Entries are
 * sorted by property, so the serialized form is deterministic.
 */
public final class ConversionGapReport {
    /**
     * The directory, relative to a build output directory such as {@code target}, that reports are written to.
     */
    public static final String REPORT_DIRECTORY = "c2j-conversion-gaps";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String service;
    private final List<Entry> entries;

    private ConversionGapReport(String service, List<Entry> entries) {
        this.service = service;
        this.entries = Collections.unmodifiableList(entries);
    }

    /**
     * Create a builder for the report of one service.
     */
    public static Builder builder(String service) {
        return new Builder(service);
    }

    /**
     * Parse a report previously written by {@link #toJson()}.
     *
     * @throws IllegalArgumentException if the JSON is not a gap report
     */
    public static ConversionGapReport fromJson(String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid C2J conversion gap report JSON", e);
        }
        if (root == null || !root.path("service").isTextual() || !root.path("entries").isArray()) {
            throw new IllegalArgumentException("A C2J conversion gap report needs a \"service\" string and an "
                                               + "\"entries\" array");
        }
        List<Entry> entries = new ArrayList<>();
        for (JsonNode e : root.get("entries")) {
            Set<String> services = new TreeSet<>();
            for (JsonNode s : e.path("services")) {
                services.add(s.asText());
            }
            entries.add(new Entry(e.path("property").asText(), services, e.path("impact").asText(),
                                  e.path("occurrences").asInt(0)));
        }
        return new ConversionGapReport(root.get("service").asText(), entries);
    }

    /**
     * The name of the service this report was produced for.
     */
    public String service() {
        return service;
    }

    /**
     * The gap entries, sorted by property. Empty when the conversion has no gaps.
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Whether the conversion had no gaps.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Serialize the report as pretty-printed JSON:
     * {@code {"service": ..., "entries": [{"property", "services", "impact", "occurrences"}, ...]}}.
     */
    public String toJson() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("service", service);
        ArrayNode array = root.putArray("entries");
        for (Entry entry : entries) {
            ObjectNode node = array.addObject();
            node.put("property", entry.property());
            ArrayNode services = node.putArray("services");
            entry.services().forEach(services::add);
            node.put("impact", entry.impact());
            node.put("occurrences", entry.occurrences());
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write the report to {@code <buildDirectory>/c2j-conversion-gaps/<service>.json}, replacing any earlier report,
     * and return the written file.
     *
     * @param buildDirectory the build output directory, for example the module's {@code target} directory
     */
    public Path writeTo(Path buildDirectory) {
        Path directory = buildDirectory.resolve(REPORT_DIRECTORY);
        Path file = directory.resolve(service + ".json");
        try {
            Files.createDirectories(directory);
            Files.write(file, toJson().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write the C2J conversion gap report to " + file, e);
        }
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConversionGapReport that = (ConversionGapReport) o;
        return service.equals(that.service) && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return 31 * service.hashCode() + entries.hashCode();
    }

    @Override
    public String toString() {
        return "ConversionGapReport(service=" + service + ", entries=" + entries + ")";
    }

    /**
     * One C2J property with no standard Smithy or AWS trait.
     */
    public static final class Entry {
        private final String property;
        private final Set<String> services;
        private final String impact;
        private final int occurrences;

        private Entry(String property, Set<String> services, String impact, int occurrences) {
            this.property = property;
            this.services = Collections.unmodifiableSet(new TreeSet<>(services));
            this.impact = impact;
            this.occurrences = occurrences;
        }

        /**
         * The C2J property, for example {@code operation.httpChecksum}.
         */
        public String property() {
            return property;
        }

        /**
         * The services the property affects, sorted.
         */
        public Set<String> services() {
            return services;
        }

        /**
         * The generated-code impact of the property having no standard trait.
         */
        public String impact() {
            return impact;
        }

        /**
         * How many times the property occurred in the converted models.
         */
        public int occurrences() {
            return occurrences;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            return occurrences == entry.occurrences
                   && property.equals(entry.property)
                   && services.equals(entry.services)
                   && impact.equals(entry.impact);
        }

        @Override
        public int hashCode() {
            int result = property.hashCode();
            result = 31 * result + services.hashCode();
            result = 31 * result + impact.hashCode();
            result = 31 * result + occurrences;
            return result;
        }

        @Override
        public String toString() {
            return "Entry(property=" + property + ", services=" + services + ", impact=" + impact
                   + ", occurrences=" + occurrences + ")";
        }
    }

    /**
     * Accumulates gaps while a service is converted.
     */
    public static final class Builder {
        private final String service;
        private final Map<String, String> impacts = new TreeMap<>();
        private final Map<String, Integer> occurrences = new TreeMap<>();

        private Builder(String service) {
            this.service = Objects.requireNonNull(service, "service");
        }

        /**
         * Record one occurrence of a C2J property with no standard trait. The first impact recorded for a property is
         * kept.
         */
        public Builder record(String property, String impact) {
            Objects.requireNonNull(property, "property");
            Objects.requireNonNull(impact, "impact");
            impacts.putIfAbsent(property, impact);
            occurrences.merge(property, 1, Integer::sum);
            return this;
        }

        /**
         * Record one occurrence of the property carried by a marker trait.
         */
        public Builder record(C2jMarkerTrait marker) {
            return record(marker.c2jProperty(), marker.impact());
        }

        /**
         * Build the report. Entries are sorted by property.
         */
        public ConversionGapReport build() {
            List<Entry> entries = new ArrayList<>(impacts.size());
            Set<String> services = Collections.singleton(service);
            for (Map.Entry<String, String> e : impacts.entrySet()) {
                entries.add(new Entry(e.getKey(), services, e.getValue(), occurrences.get(e.getKey())));
            }
            return new ConversionGapReport(service, entries);
        }
    }
}
