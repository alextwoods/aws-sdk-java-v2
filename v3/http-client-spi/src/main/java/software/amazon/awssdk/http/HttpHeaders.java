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

package software.amazon.awssdk.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable, insertion-ordered multimap of HTTP header fields (Requirement 12.4).
 *
 * <p>Every field keeps the exact name case and position it was added with, and repeated fields are never collapsed,
 * so a transport or signer iterating {@link #size()}/{@link #name(int)}/{@link #value(int)} sees exactly what the
 * protocol produced. Lookup by name is case-insensitive ({@code RFC 9110} field names are case-insensitive).
 *
 * <p>Storage is a flat array of alternating names and values, without per-field objects, so an iteration by index
 * allocates nothing.
 */
public final class HttpHeaders {
    private static final HttpHeaders EMPTY = new HttpHeaders(new String[0], 0);

    /** Alternating name, value; only the first {@code 2 * size} slots are used. */
    private final String[] fields;
    private final int size;

    private HttpHeaders(String[] fields, int size) {
        this.fields = fields;
        this.size = size;
    }

    /** No header fields. */
    public static HttpHeaders empty() {
        return EMPTY;
    }

    /** A new builder of header fields. */
    public static Builder builder() {
        return new Builder();
    }

    /** The number of fields, counting each repeated field separately. */
    public int size() {
        return size;
    }

    /** True if there are no fields. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** The name of field {@code index}, with its original case. */
    public String name(int index) {
        Objects.checkIndex(index, size);
        return fields[2 * index];
    }

    /** The value of field {@code index}. */
    public String value(int index) {
        Objects.checkIndex(index, size);
        return fields[2 * index + 1];
    }

    /** The value of the first field named {@code name} (case-insensitive). */
    public Optional<String> firstValue(String name) {
        for (int i = 0; i < size; i++) {
            if (fields[2 * i].equalsIgnoreCase(name)) {
                return Optional.of(fields[2 * i + 1]);
            }
        }
        return Optional.empty();
    }

    /** The values of every field named {@code name} (case-insensitive), in insertion order. */
    public List<String> allValues(String name) {
        List<String> out = null;
        for (int i = 0; i < size; i++) {
            if (fields[2 * i].equalsIgnoreCase(name)) {
                if (out == null) {
                    out = new ArrayList<>(2);
                }
                out.add(fields[2 * i + 1]);
            }
        }
        return out == null ? List.of() : Collections.unmodifiableList(out);
    }

    /** True if a field named {@code name} (case-insensitive) is present. */
    public boolean contains(String name) {
        for (int i = 0; i < size; i++) {
            if (fields[2 * i].equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** A builder holding these fields, for copy-on-write modification. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.fields = Arrays.copyOf(fields, Math.max(8, 2 * size));
        builder.size = size;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpHeaders other) || other.size != size) {
            return false;
        }
        return Arrays.equals(fields, 0, 2 * size, other.fields, 0, 2 * size);
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (int i = 0; i < 2 * size; i++) {
            h = 31 * h + fields[i].hashCode();
        }
        return h;
    }

    /** Renders the field names only; values may be sensitive. */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("HttpHeaders[");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(fields[2 * i]);
        }
        return out.append(']').toString();
    }

    /** Mutable builder of {@link HttpHeaders}; not thread safe. */
    public static final class Builder {
        private String[] fields = new String[16];
        private int size;

        private Builder() {
        }

        /**
         * Appends a field, after every existing field of any name.
         *
         * @throws IllegalArgumentException if {@code name} is empty or has a character other than visible ASCII or
         *     a {@code :} (HTTP/2 pseudo-headers are derived by {@link SdkHttpRequest#pseudoHeaders()}), or
         *     {@code value} contains CR, LF or NUL
         */
        public Builder add(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            RawTargets.requireFieldName(name);
            RawTargets.requireFieldValue(name, value);
            if (2 * size + 2 > fields.length) {
                fields = Arrays.copyOf(fields, fields.length * 2);
            }
            fields[2 * size] = name;
            fields[2 * size + 1] = value;
            size++;
            return this;
        }

        /**
         * Removes every field named {@code name} (case-insensitive), then appends {@code name: value}. An invalid field
         * is rejected as by {@link #add} before any field is removed.
         */
        public Builder set(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            RawTargets.requireFieldName(name);
            RawTargets.requireFieldValue(name, value);
            remove(name);
            return add(name, value);
        }

        /** Removes every field named {@code name} (case-insensitive), keeping the order of the others. */
        public Builder remove(String name) {
            int kept = 0;
            for (int i = 0; i < size; i++) {
                if (!fields[2 * i].equalsIgnoreCase(name)) {
                    fields[2 * kept] = fields[2 * i];
                    fields[2 * kept + 1] = fields[2 * i + 1];
                    kept++;
                }
            }
            Arrays.fill(fields, 2 * kept, 2 * size, null);
            size = kept;
            return this;
        }

        /** True if a field named {@code name} (case-insensitive) has been added. */
        public boolean contains(String name) {
            for (int i = 0; i < size; i++) {
                if (fields[2 * i].equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }

        public HttpHeaders build() {
            return size == 0 ? EMPTY : new HttpHeaders(Arrays.copyOf(fields, 2 * size), size);
        }
    }
}
