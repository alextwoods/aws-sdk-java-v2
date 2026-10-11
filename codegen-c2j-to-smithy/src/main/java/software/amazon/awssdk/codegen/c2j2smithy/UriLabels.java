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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes C2J request URIs into Smithy URI patterns.
 *
 * <p>A C2J URI label names the member's {@code locationName} (for example {@code {Id}} bound to member
 * {@code HostedZoneId}, or {@code {resource-arn}}, which is not a valid Smithy identifier). Smithy's {@code @httpLabel}
 * binds a label by member name, so each label is renamed to the name of the input member bound to it. The wire is
 * unchanged, because a label name never appears on the wire.
 */
final class UriLabels {
    private static final Pattern LABEL = Pattern.compile("\\{([^}]*)}");

    private UriLabels() {
    }

    /**
     * Rename every label of {@code requestUri} to the name of the input member it binds.
     *
     * @param requestUri the C2J requestUri, which may include a query string
     * @param input the C2J input structure, or a missing node when the operation has no input
     * @return the normalized URI
     * @throws IllegalArgumentException if a label is bound to no {@code location: uri} input member
     */
    static String normalize(String requestUri, JsonNode input) {
        Matcher m = LABEL.matcher(requestUri);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String label = m.group(1);
            boolean greedy = label.endsWith("+");
            String name = greedy ? label.substring(0, label.length() - 1) : label;
            String member = boundMember(name, input);
            if (member == null) {
                throw new IllegalArgumentException("URI label {" + label + "} of requestUri " + requestUri
                                                   + " is not bound to an input member with location uri");
            }
            m.appendReplacement(out, Matcher.quoteReplacement("{" + member + (greedy ? "+" : "") + "}"));
        }
        m.appendTail(out);
        return out.toString();
    }

    // The uri-located input member whose wire name (locationName, else member name) is the label. An exact match wins
    // over a case-insensitive one.
    private static String boundMember(String label, JsonNode input) {
        String caseInsensitive = null;
        for (Map.Entry<String, JsonNode> e : input.path("members").properties()) {
            JsonNode m = e.getValue();
            if (!"uri".equals(m.path("location").asText(null))) {
                continue;
            }
            String wire = m.path("locationName").asText(e.getKey());
            if (wire.equals(label)) {
                return e.getKey();
            }
            if (caseInsensitive == null && wire.equalsIgnoreCase(label)) {
                caseInsensitive = e.getKey();
            }
        }
        return caseInsensitive;
    }
}
