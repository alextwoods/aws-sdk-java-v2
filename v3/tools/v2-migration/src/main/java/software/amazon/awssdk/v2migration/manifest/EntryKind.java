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

package software.amazon.awssdk.v2migration.manifest;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The kinds of Parity Manifest entries and their stable ID schemes: the {@code kind} member of
 * {@code parity-manifest-1.schema.json}, whose {@code id} description lists the schemes. Java names are binary names
 * ({@code Outer$Inner}); member descriptors are JVM descriptors. The ID patterns are those the manifest's publisher
 * validates entries with, so the reader accepts exactly the IDs the publisher writes.
 */
public enum EntryKind {
    /** {@code artifact:<groupId>:<artifactId>}: a published V2 Maven artifact. */
    ARTIFACT("artifact:[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+"),
    /** {@code service:<module>}: a V2 AWS service client, keyed by its V2 service module name. */
    SERVICE("service:[A-Za-z0-9_.-]+"),
    /** {@code api:<binary name>}: a public or protected type. */
    PUBLIC_TYPE("api:" + Patterns.BINARY_NAME),
    /**
     * {@code api:<binary name>#<method>(<params>)<return>} or {@code api:<binary name>#<field>:<descriptor>}: a public
     * or protected method, constructor ({@code <init>}) or field.
     */
    PUBLIC_MEMBER("api:" + Patterns.BINARY_NAME + "#(?:(?:[^#():\\s]+|<init>)\\([^()\\s]*\\)[^()\\s]+"
                  + "|[^#():\\s]+:[^()\\s]+)"),
    /** {@code config:<source>:<key>}: a configuration setting, e.g. {@code config:env:AWS_REGION}. */
    CONFIGURATION("config:[a-z0-9-]+:\\S+"),
    /** {@code http-mode:<implementation>:<sync|async>}: one HTTP Implementation in one mode. */
    HTTP_MODE("http-mode:[a-z0-9-]+:(?:sync|async)"),
    /** {@code behavior:<dotted id>}: a documented customer-visible behavior. */
    BEHAVIOR("behavior:[a-z0-9][a-z0-9._-]*");

    /** The configuration source of environment variables in {@link #CONFIGURATION} IDs. */
    public static final String ENVIRONMENT_SOURCE = "env";
    /** The configuration source of JVM system properties in {@link #CONFIGURATION} IDs. */
    public static final String SYSTEM_PROPERTY_SOURCE = "sysprop";

    private final Pattern idPattern;

    EntryKind(String idPattern) {
        this.idPattern = Pattern.compile(idPattern);
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

    /** An {@link #ARTIFACT} ID. */
    public static String artifactId(String groupId, String artifactId) {
        return "artifact:" + groupId + ":" + artifactId;
    }

    /** A {@link #PUBLIC_TYPE} ID from a binary class name. */
    public static String typeId(String binaryName) {
        return "api:" + binaryName;
    }

    /** A {@link #PUBLIC_MEMBER} ID for a method or constructor ({@code <init>}) with a JVM method descriptor. */
    public static String methodId(String binaryName, String name, String methodDescriptor) {
        return "api:" + binaryName + "#" + name + methodDescriptor;
    }

    /** A {@link #PUBLIC_MEMBER} ID for a field with a JVM field descriptor. */
    public static String fieldId(String binaryName, String name, String fieldDescriptor) {
        return "api:" + binaryName + "#" + name + ":" + fieldDescriptor;
    }

    /** A {@link #CONFIGURATION} ID, e.g. {@code configurationId("env", "AWS_REGION")}. */
    public static String configurationId(String source, String key) {
        return "config:" + source + ":" + key;
    }

    private static final class Patterns {
        static final String BINARY_NAME = "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*";
    }
}
