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

package software.amazon.awssdk.protocols.core.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;

/**
 * The immutable HTTP binding plan of one operation, shared by every HTTP-bound protocol and derived once per
 * operation (design: "resolved protocol/binding/codec strategy" of a pipeline plan) from its schemas' traits:
 * <ul>
 *   <li>the request line of the {@code smithy.api#http} trait: method, URI path segments, query literals and the
 *       success status code, if the operation has the trait;</li>
 *   <li>the {@code smithy.api#endpoint} host prefix and the input's {@code hostLabel} members;</li>
 *   <li>the request bindings of the input, the response bindings of the output and of every modeled error, and each
 *       error's HTTP status code ({@code httpError}, else 400 for client and 500 for server errors).</li>
 * </ul>
 * Construction verifies that the URI labels and the input's {@code httpLabel} members, and the host prefix labels and
 * the input's {@code hostLabel} members, correspond one to one, so a plan that exists can always bind its input.
 */
public final class HttpOperationBindings {
    /** The success status code of an operation without an {@code http} trait. */
    public static final int DEFAULT_SUCCESS_CODE = 200;

    private final OperationSchemas schemas;
    private final String method;
    private final List<UriSegment> path;
    private final Map<String, String> queryLiterals;
    private final int successCode;
    private final List<UriSegment> hostPrefix;
    private final List<Schema> hostLabels;
    private final HttpMessageBindings request;
    private final HttpMessageBindings response;
    private final Map<ShapeId, HttpMessageBindings> errors;
    private final Map<ShapeId, Integer> errorCodes;

    private HttpOperationBindings(OperationSchemas schemas) {
        this.schemas = schemas;
        HttpTrait http = schemas.operation().getTrait(TraitKey.HTTP_TRAIT);
        this.method = http == null ? null : http.getMethod();
        this.path = http == null ? List.of() : segments(http.getUri());
        this.queryLiterals = http == null ? Map.of()
                                          : Collections.unmodifiableMap(new LinkedHashMap<>(
                                              http.getUri().getQueryLiterals()));
        this.successCode = http == null ? DEFAULT_SUCCESS_CODE : http.getCode();
        EndpointTrait endpoint = schemas.operation().getTrait(TraitKey.ENDPOINT_TRAIT);
        this.hostPrefix = endpoint == null ? List.of() : segments(endpoint.getHostPrefix());
        this.request = HttpMessageBindings.of(schemas.input(), HttpMessageBindings.Direction.REQUEST);
        this.response = HttpMessageBindings.of(schemas.output(), HttpMessageBindings.Direction.RESPONSE);
        Map<ShapeId, HttpMessageBindings> errorBindings = new LinkedHashMap<>();
        Map<ShapeId, Integer> codes = new LinkedHashMap<>();
        for (Schema error : schemas.errors()) {
            errorBindings.put(error.id(), HttpMessageBindings.of(error, HttpMessageBindings.Direction.RESPONSE));
            codes.put(error.id(), errorCode(error));
        }
        this.errors = Collections.unmodifiableMap(errorBindings);
        this.errorCodes = Collections.unmodifiableMap(codes);
        List<Schema> hostLabelMembers = new ArrayList<>();
        for (Schema member : schemas.input().members()) {
            if (member.hasTrait(TraitKey.HOST_LABEL_TRAIT)) {
                hostLabelMembers.add(member);
            }
        }
        this.hostLabels = List.copyOf(hostLabelMembers);
        if (http != null) {
            requireCorrespondence("URI", path, request.members(HttpBindingLocation.LABEL).stream()
                                                      .map(HttpMemberBinding::memberName).toList());
        }
        requireCorrespondence("host prefix", hostPrefix, hostLabels.stream().map(Schema::memberName).toList());
    }

    /**
     * The binding plan of the operation described by {@code schemas}.
     *
     * @throws IllegalArgumentException if a structure's bindings cannot be represented, or the URI or host prefix
     *         labels do not correspond one to one with the input's label members
     */
    public static HttpOperationBindings of(OperationSchemas schemas) {
        return new HttpOperationBindings(Objects.requireNonNull(schemas, "schemas"));
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operationId() {
        return schemas.id();
    }

    /** The operation's schemas. */
    public OperationSchemas schemas() {
        return schemas;
    }

    /** True if the operation has an {@code smithy.api#http} trait. */
    public boolean hasHttpTrait() {
        return method != null;
    }

    /** The HTTP method of the {@code http} trait; empty if the operation has none. */
    public Optional<String> method() {
        return Optional.ofNullable(method);
    }

    /** The URI path segments of the {@code http} trait, in order; empty if the operation has none. */
    public List<UriSegment> path() {
        return path;
    }

    /** The literal query parameters of the {@code http} trait's URI, in modeled order (empty values allowed). */
    public Map<String, String> queryLiterals() {
        return queryLiterals;
    }

    /** The success status code: the {@code http} trait's {@code code}, else {@value #DEFAULT_SUCCESS_CODE}. */
    public int successCode() {
        return successCode;
    }

    /** The {@code endpoint} host prefix segments; empty if the operation has none. */
    public List<UriSegment> hostPrefix() {
        return hostPrefix;
    }

    /** The input members with {@code smithy.api#hostLabel}, in modeled member order. */
    public List<Schema> hostLabels() {
        return hostLabels;
    }

    /** The request bindings of the input. */
    public HttpMessageBindings request() {
        return request;
    }

    /** The response bindings of the output. */
    public HttpMessageBindings response() {
        return response;
    }

    /** The response bindings of every modeled error, by error shape ID, in the operation's error order. */
    public Map<ShapeId, HttpMessageBindings> errors() {
        return errors;
    }

    /** The HTTP status code of every modeled error, by error shape ID, in the operation's error order. */
    public Map<ShapeId, Integer> errorCodes() {
        return errorCodes;
    }

    @Override
    public String toString() {
        return "HttpOperationBindings[" + schemas.id() + "]";
    }

    private static int errorCode(Schema error) {
        HttpErrorTrait httpError = error.getTrait(TraitKey.HTTP_ERROR_TRAIT);
        if (httpError != null) {
            return httpError.getCode();
        }
        ErrorTrait kind = error.getTrait(TraitKey.ERROR_TRAIT);
        return kind.getDefaultHttpStatusCode();
    }

    private static List<UriSegment> segments(SmithyPattern pattern) {
        List<UriSegment> out = new ArrayList<>();
        for (SmithyPattern.Segment segment : pattern.getSegments()) {
            UriSegment.Kind kind = segment.isGreedyLabel() ? UriSegment.Kind.GREEDY_LABEL
                                                           : segment.isLabel() ? UriSegment.Kind.LABEL
                                                                               : UriSegment.Kind.LITERAL;
            out.add(new UriSegment(segment.getContent(), kind));
        }
        return List.copyOf(out);
    }

    private void requireCorrespondence(String where, List<UriSegment> segments, List<String> members) {
        Set<String> labels = new TreeSet<>();
        segments.stream().filter(UriSegment::isLabel).forEach(s -> labels.add(s.content()));
        Set<String> bound = new TreeSet<>(members);
        if (!labels.equals(bound)) {
            Set<String> unbound = new TreeSet<>(labels);
            unbound.removeAll(bound);
            Set<String> unused = new TreeSet<>(bound);
            unused.removeAll(labels);
            throw new IllegalArgumentException(
                "The " + where + " labels of " + schemas.id() + " do not match the label members of "
                + schemas.input().id() + ": labels without a member " + unbound + ", members without a label "
                + unused);
        }
    }
}
