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

package software.amazon.awssdk.v2migration.recipes.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.v2migration.manifest.EntryKind;

/**
 * Parsing of Parity Manifest {@code api:} IDs ({@link EntryKind#PUBLIC_TYPE} and {@link EntryKind#PUBLIC_MEMBER}) and
 * of the JVM descriptors they embed. Names are binary names ({@code a.b.Outer$Inner}).
 */
final class ApiIds {

    private static final String API = "api:";

    private ApiIds() {
    }

    /**
     * A member of an {@code api:} member ID.
     *
     * @param owner      the declaring type's binary name
     * @param name       the member name ({@code <init>} for constructors)
     * @param parameters the parameter descriptors of a method, empty for a field
     * @param result     the return descriptor of a method, or the field descriptor
     * @param field      true for a field ID
     */
    record Member(String owner, String name, List<String> parameters, String result, boolean field) {
        Member {
            parameters = List.copyOf(parameters);
        }

        /** A short human-readable form for diagnostics, for example {@code Context.ModifyHttpRequest#httpRequest()}. */
        String describe() {
            String simple = owner.substring(owner.lastIndexOf('.') + 1).replace('$', '.');
            if (field) {
                return simple + "." + name;
            }
            List<String> types = parameters.stream().map(ApiIds::describeDescriptor).toList();
            return simple + "#" + name + "(" + String.join(", ", types) + ")";
        }
    }

    /** The binary name of a type ID, if {@code id} is one. */
    static Optional<String> type(String id) {
        return EntryKind.PUBLIC_TYPE.accepts(id) ? Optional.of(id.substring(API.length())) : Optional.empty();
    }

    /** The member of a member ID, if {@code id} is one with a well-formed descriptor. */
    static Optional<Member> member(String id) {
        if (!EntryKind.PUBLIC_MEMBER.accepts(id)) {
            return Optional.empty();
        }
        String rest = id.substring(API.length());
        int hash = rest.indexOf('#');
        String owner = rest.substring(0, hash);
        String signature = rest.substring(hash + 1);
        int paren = signature.indexOf('(');
        if (paren < 0) {
            int colon = signature.indexOf(':');
            String descriptor = signature.substring(colon + 1);
            return valid(descriptor)
                   ? Optional.of(new Member(owner, signature.substring(0, colon), List.of(), descriptor, true))
                   : Optional.empty();
        }
        int close = signature.indexOf(')', paren);
        Optional<List<String>> parameters = descriptors(signature.substring(paren + 1, close));
        String result = signature.substring(close + 1);
        if (parameters.isEmpty() || !(result.equals("V") || valid(result))) {
            return Optional.empty();
        }
        return Optional.of(new Member(owner, signature.substring(0, paren), parameters.get(), result, false));
    }

    /** The owner's binary name of any {@code api:} ID (the type itself for a type ID), if {@code id} is one. */
    static Optional<String> owner(String id) {
        return type(id).or(() -> member(id).map(Member::owner));
    }

    /** The binary name of the class a field descriptor ({@code Lx/y/Z;}) denotes; empty for primitives and arrays. */
    static Optional<String> className(String descriptor) {
        if (descriptor.length() > 2 && descriptor.charAt(0) == 'L' && descriptor.endsWith(";")) {
            return Optional.of(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
        }
        return Optional.empty();
    }

    /** The source form of a binary name: nested types are separated by dots. */
    static String sourceName(String binaryName) {
        return binaryName.replace('$', '.');
    }

    /** The parameter descriptors concatenated in {@code descriptors}, or empty if malformed. */
    static Optional<List<String>> descriptors(String descriptors) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < descriptors.length()) {
            int end = descriptorEnd(descriptors, i);
            if (end < 0) {
                return Optional.empty();
            }
            out.add(descriptors.substring(i, end));
            i = end;
        }
        return Optional.of(out);
    }

    /** A human-readable type for a descriptor, for diagnostics. */
    static String describeDescriptor(String descriptor) {
        if (descriptor.startsWith("[")) {
            return describeDescriptor(descriptor.substring(1)) + "[]";
        }
        return className(descriptor).map(ApiIds::sourceName).orElseGet(() -> primitive(descriptor.charAt(0)));
    }

    /** The Java keyword of a primitive (or {@code void}) descriptor character, or {@code null}. */
    static String primitive(char descriptor) {
        switch (descriptor) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'F': return "float";
            case 'D': return "double";
            case 'V': return "void";
            default: return null;
        }
    }

    private static boolean valid(String descriptor) {
        return !descriptor.isEmpty() && descriptorEnd(descriptor, 0) == descriptor.length();
    }

    private static int descriptorEnd(String s, int start) {
        int i = start;
        while (i < s.length() && s.charAt(i) == '[') {
            i++;
        }
        if (i >= s.length()) {
            return -1;
        }
        char c = s.charAt(i);
        if (c == 'L') {
            int semicolon = s.indexOf(';', i);
            return semicolon <= i + 1 ? -1 : semicolon + 1;
        }
        return c != 'V' && primitive(c) != null ? i + 1 : -1;
    }
}
