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

package software.amazon.awssdk.protocols.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Wire input violated the selected protocol or a schema (Requirements 5.9, 5.10). Every protocol runtime reports
 * such input with this exception, and returns no modeled value (partial or otherwise) for it.
 *
 * <p>It locates the failure exactly: the protocol, the operation, the shape being read or written when the violation
 * was found, and the member path from the operation's input, output or error structure to the failing member. The
 * path is a list of member names, list indexes ({@code [3]}) and map keys ({@code {key}}); it is empty when the
 * failure is at the root, for example an unparseable body. {@link #wireLocation()} optionally names where on the
 * wire the failure was found ({@code "header x-amz-meta"}, {@code "body"}, {@code "status code"}); it never contains
 * wire values, which may be sensitive.
 *
 * <p>Not retryable: a truncated transport body is a transport failure, classified separately.
 */
public final class SdkProtocolException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final ShapeId protocolId;
    private final ShapeId operationId;
    private final ShapeId shapeId;
    private final List<String> memberPath;
    private final String wireLocation;

    private SdkProtocolException(BuilderImpl builder) {
        super(builder);
        this.protocolId = builder.protocolId;
        this.operationId = builder.operationId;
        this.shapeId = builder.shapeId;
        this.memberPath = builder.memberPath;
        this.wireLocation = builder.wireLocation;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Absolute shape ID of the protocol trait. */
    public ShapeId protocolId() {
        return protocolId;
    }

    /** Absolute shape ID of the operation. */
    public ShapeId operationId() {
        return operationId;
    }

    /** Absolute shape ID of the shape being read or written when the violation was found. */
    public ShapeId shapeId() {
        return shapeId;
    }

    /** The path from the operation's input, output or error structure to the failing member; empty at the root. */
    public List<String> memberPath() {
        return memberPath;
    }

    /** {@link #memberPath()} rendered as {@code a.b[2].c{key}}, or {@code "<root>"} when empty. */
    public String renderedMemberPath() {
        return render(memberPath);
    }

    /** Where on the wire the violation was found, without any wire value; empty if unknown. */
    public Optional<String> wireLocation() {
        return Optional.ofNullable(wireLocation);
    }

    @Override
    public boolean retryable() {
        return false;
    }

    static String render(List<String> path) {
        if (path.isEmpty()) {
            return "<root>";
        }
        StringBuilder out = new StringBuilder();
        for (String segment : path) {
            if (!out.isEmpty() && !segment.startsWith("[") && !segment.startsWith("{")) {
                out.append('.');
            }
            out.append(segment);
        }
        return out.toString();
    }

    /**
     * Builder of an {@link SdkProtocolException}. {@code protocolId}, {@code operationId}, {@code shapeId} and a
     * {@code message} describing the violation are required; the built exception's message is prefixed with the
     * location.
     */
    public interface Builder extends SdkClientException.Builder {
        Builder protocolId(ShapeId protocolId);

        Builder operationId(ShapeId operationId);

        Builder shapeId(ShapeId shapeId);

        /** Member names, {@code [index]} and {@code {key}} segments from the root structure. */
        Builder memberPath(List<String> memberPath);

        Builder wireLocation(String wireLocation);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        SdkProtocolException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private ShapeId protocolId;
        private ShapeId operationId;
        private ShapeId shapeId;
        private List<String> memberPath = List.of();
        private String wireLocation;
        private boolean located;

        private BuilderImpl() {
        }

        private BuilderImpl(SdkProtocolException ex) {
            super(ex);
            this.protocolId = ex.protocolId;
            this.operationId = ex.operationId;
            this.shapeId = ex.shapeId;
            this.memberPath = ex.memberPath;
            this.wireLocation = ex.wireLocation;
            this.located = true;
        }

        @Override
        public Builder protocolId(ShapeId protocolId) {
            this.protocolId = protocolId;
            return this;
        }

        @Override
        public Builder operationId(ShapeId operationId) {
            this.operationId = operationId;
            return this;
        }

        @Override
        public Builder shapeId(ShapeId shapeId) {
            this.shapeId = shapeId;
            return this;
        }

        @Override
        public Builder memberPath(List<String> memberPath) {
            this.memberPath = List.copyOf(memberPath);
            return this;
        }

        @Override
        public Builder wireLocation(String wireLocation) {
            this.wireLocation = wireLocation;
            return this;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            this.located = false;
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            this.writableStackTrace = writableStackTrace;
            return this;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            this.numAttempts = numAttempts;
            return this;
        }

        @Override
        public SdkProtocolException build() {
            Objects.requireNonNull(protocolId, "protocolId");
            Objects.requireNonNull(operationId, "operationId");
            Objects.requireNonNull(shapeId, "shapeId");
            Objects.requireNonNull(message, "message");
            if (!located) {
                message = "Invalid " + protocolId + " wire input for operation " + operationId + ", shape " + shapeId
                          + ", member " + render(memberPath)
                          + (wireLocation == null ? "" : " (" + wireLocation + ")") + ": " + message;
                located = true;
            }
            return new SdkProtocolException(this);
        }
    }
}
