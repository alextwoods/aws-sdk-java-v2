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

package software.amazon.awssdk.test.protocoltests.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The checked-in list of approved omissions of a conformance suite (Requirement 19.15): applicable cases that are
 * expected not to pass, each with its reason and the Parity Manifest entry or requirement that sanctions it.
 *
 * <p>Format, one entry per line: {@code <case key> | <reason> | <reference>}; {@code #} starts a comment line. An
 * entry must name a discovered applicable case, must have a non-blank reason and reference, and must still fail: an
 * approved case that passes fails the suite until its entry is removed, so the list cannot go stale.
 */
public final class ApprovedOmissions {
    /** One approved omission. */
    public record Entry(String key, String reason, String reference, int line) {
    }

    private final String resource;
    private final Map<String, Entry> entries;
    private final List<String> problems;

    private ApprovedOmissions(String resource, Map<String, Entry> entries, List<String> problems) {
        this.resource = resource;
        this.entries = Collections.unmodifiableMap(entries);
        this.problems = List.copyOf(problems);
    }

    /** Reads the list from class-path resource {@code resource}. */
    public static ApprovedOmissions load(String resource) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        InputStream in = ApprovedOmissions.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("The approved-omission list " + resource + " is missing");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                String text = line.strip();
                if (text.isEmpty() || text.startsWith("#")) {
                    continue;
                }
                String[] fields = text.split("\\s+\\|\\s+", -1);
                if (fields.length != 3 || fields[0].isBlank() || fields[1].isBlank() || fields[2].isBlank()) {
                    problems.add(resource + ":" + number + ": expected '<case key> | <reason> | <reference>'");
                    continue;
                }
                Entry entry = new Entry(fields[0].strip(), fields[1].strip(), fields[2].strip(), number);
                if (entries.putIfAbsent(entry.key(), entry) != null) {
                    problems.add(resource + ":" + number + ": duplicate entry " + entry.key());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ApprovedOmissions(resource, entries, problems);
    }

    public String resource() {
        return resource;
    }

    public Optional<Entry> entry(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    public Map<String, Entry> entries() {
        return entries;
    }

    /** Malformed or duplicate lines. */
    public List<String> problems() {
        return problems;
    }
}
