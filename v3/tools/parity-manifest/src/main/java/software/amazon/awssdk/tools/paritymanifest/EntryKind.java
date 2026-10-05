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

package software.amazon.awssdk.tools.paritymanifest;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The kinds of Parity Manifest entries (requirements glossary "Parity Manifest": public APIs, behaviors, services,
 * high-level libraries, HTTP Implementations, configuration sources and extension points; Requirement 16.2: every
 * public artifact, public API and documented customer behavior). High-level libraries are {@link #ARTIFACT}s and
 * extension points are {@link #PUBLIC_TYPE}s and {@link #PUBLIC_MEMBER}s.
 *
 * <p>Each kind has a stable, prefixed ID scheme so candidate evidence can join by entry ID (design "Parity Manifest
 * schema"). Java names are binary names ({@code Outer$Inner}); member descriptors are JVM descriptors.
 */
public enum EntryKind {
    /** {@code artifact:<groupId>:<artifactId>}: a published V2 Maven artifact. */
    ARTIFACT("artifact:", "artifact:[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+"),
    /** {@code service:<module>}: a V2 AWS service client (Requirement 16.4), keyed by its V2 service module name. */
    SERVICE("service:", "service:[A-Za-z0-9_.-]+"),
    /** {@code api:<binary name>}: a public or protected type. */
    PUBLIC_TYPE("api:", "api:" + Patterns.BINARY_NAME),
    /**
     * {@code api:<binary name>#<method>(<params>)<return>} or {@code api:<binary name>#<field>:<descriptor>}: a public
     * or protected method, constructor ({@code <init>}) or field.
     */
    PUBLIC_MEMBER("api:", "api:" + Patterns.BINARY_NAME + "#(?:(?:[^#():\\s]+|<init>)\\([^()\\s]*\\)[^()\\s]+"
                          + "|[^#():\\s]+:[^()\\s]+)"),
    /** {@code config:<source>:<key>}: a configuration setting, e.g. {@code config:env:AWS_REGION}. */
    CONFIGURATION("config:", "config:[a-z0-9-]+:\\S+"),
    /**
     * {@code http-mode:<implementation>:<sync|async>}: one HTTP Implementation in one mode (Requirement 12.10:
     * Apache 4, Apache 5, URLConnection, Netty and CRT are classified separately per supported mode).
     */
    HTTP_MODE("http-mode:", "http-mode:[a-z0-9-]+:(?:sync|async)"),
    /** {@code behavior:<dotted id>}: a documented customer-visible behavior. */
    BEHAVIOR("behavior:", "behavior:[a-z0-9][a-z0-9._-]*");

    private final String prefix;
    private final Pattern idPattern;

    EntryKind(String prefix, String idPattern) {
        this.prefix = prefix;
        this.idPattern = Pattern.compile(idPattern);
    }

    /** The ID prefix of this kind ({@link #PUBLIC_TYPE} and {@link #PUBLIC_MEMBER} share {@code api:}). */
    public String prefix() {
        return prefix;
    }

    /** True if {@code id} follows this kind's ID scheme. */
    public boolean accepts(String id) {
        return id != null && idPattern.matcher(id).matches();
    }

    /** The kind whose ID scheme {@code id} follows, if any. */
    public static Optional<EntryKind> ofId(String id) {
        for (EntryKind kind : values()) {
            if (kind.accepts(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    /** Builds an {@link #ARTIFACT} ID. */
    public static String artifactId(String groupId, String artifactId) {
        return "artifact:" + groupId + ":" + artifactId;
    }

    /** Builds a {@link #PUBLIC_TYPE} ID from a binary class name. */
    public static String typeId(String binaryName) {
        return "api:" + binaryName;
    }

    /** Builds a {@link #PUBLIC_MEMBER} ID for a method or constructor ({@code <init>}) with a JVM descriptor. */
    public static String methodId(String binaryName, String name, String methodDescriptor) {
        return "api:" + binaryName + "#" + name + methodDescriptor;
    }

    /** Builds a {@link #PUBLIC_MEMBER} ID for a field with a JVM field descriptor. */
    public static String fieldId(String binaryName, String name, String fieldDescriptor) {
        return "api:" + binaryName + "#" + name + ":" + fieldDescriptor;
    }

    /** Builds a {@link #CONFIGURATION} ID, e.g. {@code configurationId("env", "AWS_REGION")}. */
    public static String configurationId(String source, String key) {
        return "config:" + source + ":" + key;
    }

    /** Builds an {@link #HTTP_MODE} ID; {@code async} selects the asynchronous mode. */
    public static String httpModeId(String implementation, boolean async) {
        return "http-mode:" + implementation + ":" + (async ? "async" : "sync");
    }

    private static final class Patterns {
        static final String BINARY_NAME = "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*";
    }
}
