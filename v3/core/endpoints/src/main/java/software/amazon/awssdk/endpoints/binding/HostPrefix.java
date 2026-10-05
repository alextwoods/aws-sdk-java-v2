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

package software.amazon.awssdk.endpoints.binding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.rules.internal.RulesFunctions;

/**
 * The {@code hostPrefix} of an operation's {@code smithy.api#endpoint} trait: a template of literal text and
 * {@code {Label}} placeholders bound to the operation input members with the {@code smithy.api#hostLabel} trait,
 * prepended to the host of the resolved endpoint (for example {@code {AccountId}.} before
 * {@code s3-control.us-west-2.amazonaws.com}).
 *
 * <p>Every label value must be set and non-empty, and the expanded prefix must consist of valid host labels
 * (letters, digits and hyphens, separated by dots), so a request member can never redirect a request to another
 * host. Instances are immutable and thread-safe.
 */
public final class HostPrefix {
    private final String template;
    private final List<Object> parts;
    private final List<String> labels;

    private HostPrefix(String template, List<Object> parts, List<String> labels) {
        this.template = template;
        this.parts = List.copyOf(parts);
        this.labels = List.copyOf(labels);
    }

    /**
     * Parses a {@code hostPrefix} template.
     *
     * @throws IllegalArgumentException if a placeholder is empty or unterminated
     */
    public static HostPrefix parse(String template) {
        Objects.requireNonNull(template, "template");
        List<Object> parts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        while (i < template.length()) {
            int open = template.indexOf('{', i);
            if (open < 0) {
                parts.add(template.substring(i));
                break;
            }
            if (open > i) {
                parts.add(template.substring(i, open));
            }
            int close = template.indexOf('}', open);
            if (close < 0 || close == open + 1) {
                throw new IllegalArgumentException("Invalid hostPrefix template \"" + template + "\".");
            }
            String label = template.substring(open + 1, close);
            parts.add(new Label(label));
            labels.add(label);
            i = close + 1;
        }
        return new HostPrefix(template, parts, labels);
    }

    /** The template as modeled. */
    public String template() {
        return template;
    }

    /** The label names in template order: the member names of the input's host-label members. Immutable. */
    public List<String> labels() {
        return labels;
    }

    /**
     * The template expanded with {@code labelValues} (by label name).
     *
     * @throws EndpointResolutionException if a label is unset or empty, or the prefix is not a valid host prefix
     */
    public String expand(Map<String, String> labelValues) {
        StringBuilder prefix = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Label label) {
                String value = labelValues.get(label.name());
                if (value == null || value.isEmpty()) {
                    throw EndpointResolutionException.create(
                        "The host label " + label.name() + " of host prefix \"" + template + "\" must be set to a "
                        + "non-empty value.");
                }
                if (value.length() > 63 || !value.chars().allMatch(HostPrefix::isLabelCharacter)) {
                    throw EndpointResolutionException.create(
                        "The host label " + label.name() + " of host prefix \"" + template + "\" must be 1-63 "
                        + "letters, digits or hyphens.");
                }
                prefix.append(value);
            } else {
                prefix.append((String) part);
            }
        }
        String expanded = prefix.toString();
        String host = expanded.endsWith(".") ? expanded.substring(0, expanded.length() - 1) : expanded;
        // A prefix may end in '-' (for example "query-"), so validate it as the start of a host label.
        String probe = host.endsWith("-") ? host + "x" : host;
        if (host.isEmpty() || !RulesFunctions.isValidHostLabel(probe, true)) {
            throw EndpointResolutionException.create(
                "The host prefix \"" + expanded + "\" (from \"" + template + "\") is not a valid host prefix: each "
                + "dot-separated label must be 1-63 letters, digits or hyphens and start with a letter or digit.");
        }
        return expanded;
    }

    /**
     * {@code endpoint} with the expanded prefix prepended to its host.
     *
     * @throws EndpointResolutionException if the prefix is invalid or the endpoint has no host
     */
    public URI apply(URI endpoint, Map<String, String> labelValues) {
        String prefix = expand(labelValues);
        String host = endpoint.getHost();
        String text = endpoint.toString();
        int authority = text.indexOf("//");
        String userInfo = endpoint.getRawUserInfo();
        int hostStart = authority + 2 + (userInfo == null ? 0 : userInfo.length() + 1);
        if (host == null || authority < 0 || !text.startsWith(host, hostStart)) {
            throw EndpointResolutionException.create("The endpoint " + endpoint + " has no host to apply host prefix \""
                                                     + template + "\" to.");
        }
        try {
            return new URI(text.substring(0, hostStart) + prefix + text.substring(hostStart));
        } catch (URISyntaxException e) {
            throw EndpointResolutionException.builder()
                                             .message("Unable to apply host prefix \"" + prefix + "\" to " + endpoint)
                                             .cause(e)
                                             .build();
        }
    }

    private static boolean isLabelCharacter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-';
    }

    @Override
    public String toString() {
        return template;
    }

    private record Label(String name) {
    }
}
