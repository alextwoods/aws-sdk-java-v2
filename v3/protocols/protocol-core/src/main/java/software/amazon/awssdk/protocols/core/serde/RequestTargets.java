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

package software.amazon.awssdk.protocols.core.serde;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.protocols.core.binding.UriSegment;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.traits.EndpointTrait;

/**
 * Request-target helpers shared by the protocol runtimes: starting a request at an endpoint, joining the operation's
 * path to the endpoint's base path, and applying the {@code smithy.api#endpoint} host prefix.
 */
public final class RequestTargets {
    private RequestTargets() {
    }

    /**
     * A request builder addressed to {@code endpoint} with {@code method} and the endpoint's base path joined to
     * {@code encodedOperationPath}.
     *
     * @param endpoint an absolute {@code http}/{@code https} URI without query or fragment
     * @param encodedOperationPath the operation's percent-encoded path, starting with {@code /}
     * @throws IllegalArgumentException if the endpoint is not such a URI
     */
    public static SdkHttpRequest.Builder start(URI endpoint, String method, String encodedOperationPath) {
        Objects.requireNonNull(endpoint, "endpoint");
        if (!endpoint.isAbsolute() || endpoint.getHost() == null) {
            throw new IllegalArgumentException("The endpoint is not an absolute URI with a host");
        }
        if (endpoint.getRawQuery() != null || endpoint.getRawFragment() != null) {
            throw new IllegalArgumentException("The endpoint has a query or fragment");
        }
        return SdkHttpRequest.builder()
                             .method(method)
                             .scheme(endpoint.getScheme())
                             .host(endpoint.getHost())
                             .port(endpoint.getPort())
                             .encodedPath(joinPath(endpoint.getRawPath(), encodedOperationPath));
    }

    /**
     * Joins an endpoint's base path and an operation path with exactly one {@code /} between them.
     */
    public static String joinPath(String basePath, String operationPath) {
        String base = basePath == null ? "" : basePath;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (operationPath.isEmpty() || operationPath.equals("/")) {
            return base.isEmpty() ? "/" : base + (operationPath.isEmpty() ? "" : "/");
        }
        return base + (operationPath.startsWith("/") ? operationPath : "/" + operationPath);
    }

    /**
     * The operation's {@code smithy.api#endpoint} host prefix segments, or an empty list if it has none.
     */
    public static List<UriSegment> hostPrefix(Schema operation) {
        EndpointTrait endpoint = operation.getTrait(TraitKey.ENDPOINT_TRAIT);
        if (endpoint == null) {
            return List.of();
        }
        List<UriSegment> out = new ArrayList<>();
        for (SmithyPattern.Segment segment : endpoint.getHostPrefix().getSegments()) {
            out.add(new UriSegment(segment.getContent(), segment.isLabel() ? UriSegment.Kind.LABEL
                                                                           : UriSegment.Kind.LITERAL));
        }
        return List.copyOf(out);
    }

    /**
     * Prepends the host prefix to the request's host, replacing each label with the value of the input member of
     * that name ({@code smithy.api#hostLabel}).
     *
     * @throws IllegalArgumentException if a host label member is absent, empty or not a valid DNS label sequence
     *         (the value is not included in the message)
     */
    public static void applyHostPrefix(SdkHttpRequest.Builder request, String host, List<UriSegment> prefix,
                                       SerializableStruct input) {
        if (prefix.isEmpty()) {
            return;
        }
        StringBuilder out = new StringBuilder();
        for (UriSegment segment : prefix) {
            if (!segment.isLabel()) {
                out.append(segment.content());
                continue;
            }
            Schema member = input.schema().member(segment.content());
            Object value = member == null ? null : input.getMemberValue(member);
            if (!(value instanceof String text) || text.isEmpty()) {
                throw new IllegalArgumentException("The host label member " + segment.content() + " of "
                                                   + input.schema().id() + " is absent or empty");
            }
            if (!isHostLabelText(text)) {
                throw new IllegalArgumentException("The host label member " + segment.content() + " of "
                                                   + input.schema().id() + " is not a valid host name part");
            }
            out.append(text);
        }
        request.host(out.append(host).toString());
    }

    private static boolean isHostLabelText(String text) {
        for (String label : text.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
            for (int i = 0; i < label.length(); i++) {
                char c = label.charAt(i);
                if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-')) {
                    return false;
                }
            }
        }
        return true;
    }
}
