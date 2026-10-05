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

import java.util.Objects;

/**
 * One {@code /}-separated segment of an {@code smithy.api#http} URI path or an {@code smithy.api#endpoint} host
 * prefix.
 *
 * @param content the literal text, or the label name without braces or {@code +}
 * @param kind whether the segment is literal or a label
 */
public record UriSegment(String content, Kind kind) {

    /** The kind of a segment. */
    public enum Kind {
        /** Literal text. */
        LITERAL,
        /** A label replaced by one percent-encoded member value. */
        LABEL,
        /** A greedy label ({@code {name+}}) whose value may contain {@code /}. */
        GREEDY_LABEL
    }

    public UriSegment {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(kind, "kind");
    }

    /** True for {@link Kind#LABEL} and {@link Kind#GREEDY_LABEL}. */
    public boolean isLabel() {
        return kind != Kind.LITERAL;
    }
}
