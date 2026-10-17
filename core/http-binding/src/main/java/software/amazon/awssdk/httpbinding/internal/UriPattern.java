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

package software.amazon.awssdk.httpbinding.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A parsed {@code @http} URI pattern, such as {@code /{Bucket}/{Key+}?x-id=GetObject&tagging}.
 *
 * <p>SDK addition: smithy-java reads the parsed {@code UriPattern} from smithy-model's {@code HttpTrait}; the SDK's
 * runtime {@code HttpTrait} keeps the unparsed string. This class parses the parts the HTTP binding serde needs: the
 * path segments (literals, labels and the greedy label) and the query literals.
 */
@SdkInternalApi
final class UriPattern {
    private final List<Segment> segments;
    private final Map<String, String> queryLiterals;

    private UriPattern(List<Segment> segments, Map<String, String> queryLiterals) {
        this.segments = segments;
        this.queryLiterals = queryLiterals;
    }

    /**
     * Parses a URI pattern.
     *
     * @param pattern the pattern; must start with {@code /}
     * @return the parsed pattern
     * @throws IllegalArgumentException if the pattern is malformed
     */
    static UriPattern parse(String pattern) {
        if (pattern == null || !pattern.startsWith("/")) {
            throw new IllegalArgumentException("URI pattern must start with '/': " + pattern);
        }
        int queryStart = pattern.indexOf('?');
        String path = queryStart == -1 ? pattern : pattern.substring(0, queryStart);
        String query = queryStart == -1 ? "" : pattern.substring(queryStart + 1);
        return new UriPattern(parseSegments(pattern, path), parseQueryLiterals(pattern, query));
    }

    private static List<Segment> parseSegments(String pattern, String path) {
        List<Segment> result = new ArrayList<>();
        if (path.length() == 1) {
            // "/" has no segments.
            return Collections.emptyList();
        }
        boolean sawGreedy = false;
        int start = 1;
        while (start <= path.length()) {
            int end = path.indexOf('/', start);
            if (end == -1) {
                end = path.length();
            }
            String content = path.substring(start, end);
            Segment segment = parseSegment(pattern, content);
            if (segment.isGreedyLabel()) {
                if (sawGreedy) {
                    throw new IllegalArgumentException("URI pattern has more than one greedy label: " + pattern);
                }
                sawGreedy = true;
            }
            result.add(segment);
            start = end + 1;
        }
        return Collections.unmodifiableList(result);
    }

    private static Segment parseSegment(String pattern, String content) {
        boolean opens = content.startsWith("{");
        boolean closes = content.endsWith("}");
        if (!opens && !closes) {
            if (content.indexOf('{') != -1 || content.indexOf('}') != -1) {
                throw new IllegalArgumentException("URI label must be a whole path segment: " + pattern);
            }
            return new Segment(content, SegmentType.LITERAL);
        }
        if (!opens || !closes || content.length() < 3) {
            throw new IllegalArgumentException("Malformed URI label `" + content + "` in " + pattern);
        }
        String name = content.substring(1, content.length() - 1);
        SegmentType type = SegmentType.LABEL;
        if (name.endsWith("+")) {
            name = name.substring(0, name.length() - 1);
            type = SegmentType.GREEDY_LABEL;
        }
        if (name.isEmpty() || name.indexOf('{') != -1 || name.indexOf('}') != -1) {
            throw new IllegalArgumentException("Malformed URI label `" + content + "` in " + pattern);
        }
        return new Segment(name, type);
    }

    private static Map<String, String> parseQueryLiterals(String pattern, String query) {
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String key = eq == -1 ? part : part.substring(0, eq);
            String value = eq == -1 ? null : part.substring(eq + 1);
            if (key.isEmpty() || result.containsKey(key)) {
                throw new IllegalArgumentException("Invalid query literal `" + part + "` in " + pattern);
            }
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the path segments, in order. A pattern of {@code /} has none.
     */
    List<Segment> segments() {
        return segments;
    }

    /**
     * Returns the query literals in declaration order. A literal without {@code =} (for example {@code ?tagging}) maps
     * to null; {@code ?a=} maps to the empty string.
     */
    Map<String, String> queryLiterals() {
        return queryLiterals;
    }

    enum SegmentType {
        LITERAL,
        LABEL,
        GREEDY_LABEL
    }

    /**
     * One path segment: literal text, or the member name of a label.
     */
    static final class Segment {
        private final String content;
        private final SegmentType type;

        Segment(String content, SegmentType type) {
            this.content = content;
            this.type = type;
        }

        String content() {
            return content;
        }

        boolean isLabel() {
            return type == SegmentType.LABEL;
        }

        boolean isGreedyLabel() {
            return type == SegmentType.GREEDY_LABEL;
        }
    }
}
