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

package software.amazon.awssdk.v2migration;

/** The syntactic form in which a migration item was encountered, and what its {@link SourceLocation} spans. */
public enum ItemKind {
    /** A {@code <dependency>} of a POM's {@code dependencies} section; spans the element. */
    DEPENDENCY,
    /** A non-import {@code <dependency>} of a POM's {@code dependencyManagement} section; spans the element. */
    MANAGED_DEPENDENCY,
    /** A {@code <scope>import</scope>} BOM in a POM's {@code dependencyManagement} section; spans the element. */
    BOM_IMPORT,
    /**
     * A V2 artifact of the project's effective dependency graph that this POM does not declare (transitive or
     * inherited); a position at the declaration it arrives through.
     */
    EFFECTIVE_DEPENDENCY,
    /** A single-type import; spans the whole {@code import ...;} declaration. */
    IMPORT,
    /** A single static import; spans the whole {@code import static ...;} declaration. */
    STATIC_IMPORT,
    /** A type named in source outside imports; spans the (possibly qualified) name. */
    TYPE_REFERENCE,
    /** A method invocation; spans the method name through the closing parenthesis (not the receiver). */
    METHOD_INVOCATION,
    /** A non-anonymous {@code new} expression; spans the whole expression. */
    CONSTRUCTOR_CALL,
    /** A field or enum constant read or written; spans the (possibly qualified) name. */
    FIELD_ACCESS,
    /**
     * A method declaration that overrides or implements a V2 method, such as an interceptor hook; spans the whole
     * declaration including its modifiers and body.
     */
    METHOD_OVERRIDE,
    /** A V2 configuration key passed as a string literal to the JVM's environment or system-property APIs. */
    CONFIGURATION_KEY,
    /**
     * Source that the compiler could not attribute against the project's class path; spans the reported range. Such a
     * construct may use V2 APIs that cannot be identified, so it is never left silently unexamined.
     */
    UNATTRIBUTED_SOURCE
}
