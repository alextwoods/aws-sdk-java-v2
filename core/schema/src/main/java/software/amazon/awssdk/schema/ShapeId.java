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

package software.amazon.awssdk.schema;

import java.io.Serializable;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An immutable, absolute Smithy shape ID of the form {@code namespace#Name} or {@code namespace#Name$member}.
 *
 * <p>This is the SDK runtime port of the subset of {@code software.amazon.smithy.model.shapes.ShapeId} that
 * schema-based serialization uses. It has no dependency on {@code smithy-model}, so generated clients can use it
 * at runtime.
 *
 * <p>Each segment follows the Smithy IDL 2.0 identifier grammar: an identifier starts with an ASCII letter, or with
 * one or more underscores followed by an ASCII letter or digit, and continues with ASCII letters, digits and
 * underscores. A namespace is one or more identifiers separated by {@code .}.
 *
 * <p>Instances are thread-safe. {@link #toString()} and {@link #hashCode()} are computed once at construction.
 */
@SdkPublicApi
public final class ShapeId implements Comparable<ShapeId>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String namespace;
    private final String name;
    private final String member;

    // Derived from the fields above; rebuilt by readResolve after deserialization.
    private final transient String absoluteId;
    private final transient int hash;

    private ShapeId(String namespace, String name, String member) {
        this.namespace = namespace;
        this.name = name;
        this.member = member;
        this.absoluteId = format(namespace, name, member);
        this.hash = absoluteId.hashCode();
    }

    /**
     * Parses an absolute shape ID.
     *
     * @param absoluteShapeId a shape ID of the form {@code namespace#Name} or {@code namespace#Name$member}
     * @return the parsed shape ID
     * @throws ShapeIdSyntaxException if the string is not a valid absolute shape ID
     * @throws NullPointerException if {@code absoluteShapeId} is null
     */
    public static ShapeId from(String absoluteShapeId) {
        Validate.paramNotNull(absoluteShapeId, "absoluteShapeId");

        int hashIndex = absoluteShapeId.indexOf('#');
        if (hashIndex < 0) {
            throw syntaxError(absoluteShapeId, "an absolute shape ID must contain '#'");
        }
        String namespace = absoluteShapeId.substring(0, hashIndex);
        String rest = absoluteShapeId.substring(hashIndex + 1);

        int dollarIndex = rest.indexOf('$');
        String name = dollarIndex < 0 ? rest : rest.substring(0, dollarIndex);
        String member = dollarIndex < 0 ? null : rest.substring(dollarIndex + 1);

        validate(absoluteShapeId, namespace, name, member);
        return new ShapeId(namespace, name, member);
    }

    /**
     * Parses a shape ID that may be relative. A relative ID ({@code Name} or {@code Name$member}) is resolved against
     * {@code defaultNamespace}. An absolute ID (one that contains {@code #}) is parsed as-is and
     * {@code defaultNamespace} is ignored.
     *
     * @param defaultNamespace the namespace used when {@code shapeId} is relative; may be null, in which case
     *                         {@code shapeId} must be absolute
     * @param shapeId the absolute or relative shape ID
     * @return the parsed shape ID
     * @throws ShapeIdSyntaxException if the resulting shape ID is not valid
     * @throws NullPointerException if {@code shapeId} is null
     */
    public static ShapeId fromOptionalNamespace(String defaultNamespace, String shapeId) {
        Validate.paramNotNull(shapeId, "shapeId");
        if (defaultNamespace == null || shapeId.indexOf('#') >= 0) {
            return from(shapeId);
        }
        return from(defaultNamespace + "#" + shapeId);
    }

    /**
     * Creates a shape ID from a namespace and a shape name, without parsing a combined string.
     *
     * @param namespace the namespace, for example {@code com.amazonaws.sqs}
     * @param name the shape name, for example {@code GetQueueUrlRequest}
     * @return the shape ID {@code namespace#name}
     * @throws ShapeIdSyntaxException if the namespace or name is not valid
     * @throws NullPointerException if any argument is null
     */
    public static ShapeId fromParts(String namespace, String name) {
        Validate.paramNotNull(namespace, "namespace");
        Validate.paramNotNull(name, "name");
        validate(format(namespace, name, null), namespace, name, null);
        return new ShapeId(namespace, name, null);
    }

    /**
     * Creates a member shape ID from a namespace, a shape name and a member name, without parsing a combined string.
     *
     * @param namespace the namespace
     * @param name the shape name
     * @param member the member name
     * @return the shape ID {@code namespace#name$member}
     * @throws ShapeIdSyntaxException if any part is not valid
     * @throws NullPointerException if any argument is null
     */
    public static ShapeId fromParts(String namespace, String name, String member) {
        Validate.paramNotNull(namespace, "namespace");
        Validate.paramNotNull(name, "name");
        Validate.paramNotNull(member, "member");
        validate(format(namespace, name, member), namespace, name, member);
        return new ShapeId(namespace, name, member);
    }

    /**
     * Returns the namespace, for example {@code com.amazonaws.sqs}.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the shape name, for example {@code GetQueueUrlRequest}. For a member shape ID this is the name of the
     * containing shape.
     *
     * @return the shape name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the member name, or an empty optional when this is not a member shape ID.
     *
     * @return the member name, if present
     */
    public Optional<String> getMember() {
        return Optional.ofNullable(member);
    }

    /**
     * Returns whether this is a member shape ID ({@code namespace#Name$member}).
     *
     * @return true if a member name is present
     */
    public boolean hasMember() {
        return member != null;
    }

    /**
     * Returns a shape ID with the same namespace and name and the given member name.
     *
     * @param member the member name
     * @return the member shape ID
     * @throws ShapeIdSyntaxException if the member name is not a valid identifier
     * @throws NullPointerException if {@code member} is null
     */
    public ShapeId withMember(String member) {
        return fromParts(namespace, name, member);
    }

    /**
     * Returns this shape ID without its member name. Returns this instance when there is no member name.
     *
     * @return the non-member shape ID
     */
    public ShapeId withoutMember() {
        return member == null ? this : new ShapeId(namespace, name, null);
    }

    /**
     * Returns the absolute shape ID, {@code namespace#Name} or {@code namespace#Name$member}.
     *
     * @return the absolute shape ID string
     */
    @Override
    public String toString() {
        return absoluteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShapeId)) {
            return false;
        }
        ShapeId other = (ShapeId) o;
        return hash == other.hash && absoluteId.equals(other.absoluteId);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Orders shape IDs by their absolute string form, case-insensitively, using a case-sensitive comparison to break
     * ties. This ordering is consistent with {@link #equals(Object)}.
     *
     * @param other the shape ID to compare with
     * @return a negative number, zero, or a positive number as this ID sorts before, equal to, or after {@code other}
     */
    @Override
    public int compareTo(ShapeId other) {
        int outcome = absoluteId.compareToIgnoreCase(other.absoluteId);
        return outcome != 0 ? outcome : absoluteId.compareTo(other.absoluteId);
    }

    /**
     * Re-validates the deserialized parts and rebuilds the derived fields.
     */
    private Object readResolve() {
        return member == null ? fromParts(namespace, name) : fromParts(namespace, name, member);
    }

    private static String format(String namespace, String name, String member) {
        StringBuilder result = new StringBuilder(namespace.length() + name.length() + 1
                                                 + (member == null ? 0 : member.length() + 1));
        result.append(namespace).append('#').append(name);
        if (member != null) {
            result.append('$').append(member);
        }
        return result.toString();
    }

    private static void validate(String input, String namespace, String name, String member) {
        if (namespace.isEmpty()) {
            throw syntaxError(input, "the namespace is empty");
        }
        if (!isValidNamespace(namespace)) {
            throw syntaxError(input, "the namespace is not a valid Smithy namespace");
        }
        if (name.isEmpty()) {
            throw syntaxError(input, "the shape name is empty");
        }
        if (!isValidIdentifier(name, 0, name.length())) {
            throw syntaxError(input, "the shape name is not a valid Smithy identifier");
        }
        if (member != null) {
            if (member.isEmpty()) {
                throw syntaxError(input, "the member name is empty");
            }
            if (!isValidIdentifier(member, 0, member.length())) {
                throw syntaxError(input, "the member name is not a valid Smithy identifier");
            }
        }
    }

    private static boolean isValidNamespace(String namespace) {
        int start = 0;
        for (int i = 0; i <= namespace.length(); i++) {
            if (i == namespace.length() || namespace.charAt(i) == '.') {
                if (!isValidIdentifier(namespace, start, i)) {
                    return false;
                }
                start = i + 1;
            }
        }
        return true;
    }

    /**
     * Validates {@code value[start, end)} against the Smithy IDL 2.0 identifier grammar:
     * {@code (1*"_" (ALPHA / DIGIT) / ALPHA) *(ALPHA / DIGIT / "_")}.
     */
    private static boolean isValidIdentifier(String value, int start, int end) {
        if (start >= end) {
            return false;
        }
        int i = start;
        if (value.charAt(i) == '_') {
            while (i < end && value.charAt(i) == '_') {
                i++;
            }
            if (i == end || !isAlphaOrDigit(value.charAt(i))) {
                return false;
            }
        } else if (!isAlpha(value.charAt(i))) {
            return false;
        }
        for (i++; i < end; i++) {
            char c = value.charAt(i);
            if (c != '_' && !isAlphaOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isAlphaOrDigit(char c) {
        return isAlpha(c) || (c >= '0' && c <= '9');
    }

    private static ShapeIdSyntaxException syntaxError(String input, String reason) {
        return new ShapeIdSyntaxException("Invalid shape ID '" + input + "': " + reason);
    }
}
