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

package software.amazon.awssdk.test.architecture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.test.architecture.ClassFile.Member;

/**
 * Bytecode checks over a module's compiled classes. Every diagnostic starts with the module's artifact ID and names
 * the referencing class.
 */
public final class BytecodeInspector {

    private BytecodeInspector() {
    }

    /**
     * References to classes of the artifact classes in {@code forbiddenClassIds} that have a class-name rule in
     * {@link ForbiddenClassNames} (Requirements 15.3-15.8).
     */
    public static List<String> forbiddenReferences(String module, Map<String, ClassFile> classes,
                                                   Set<String> forbiddenClassIds) {
        List<String> result = new ArrayList<>();
        for (ClassFile type : classes.values()) {
            for (String reference : type.referencedClasses()) {
                List<String> matched = ForbiddenClassNames.classify(reference, forbiddenClassIds);
                if (!matched.isEmpty()) {
                    result.add(module + ": " + type.name() + " references " + reference + " (forbidden artifact class "
                               + matched + ")");
                }
            }
        }
        return result;
    }

    /**
     * References that neither the JDK, the module itself nor {@code runtimeClassPath} (the module's resolved runtime
     * dependency tree) defines. Such a class can only come from outside the published runtime tree: a V2 runtime
     * artifact (V2 and V3 share the {@code software.amazon.awssdk} packages), or a provided, test-scoped or
     * undeclared dependency. Diagnostics are grouped by referenced class.
     */
    public static List<String> unresolvedReferences(String module, Map<String, ClassFile> classes,
                                                    ClassPath runtimeClassPath) {
        Map<String, Set<String>> unresolved = new TreeMap<>();
        for (ClassFile type : classes.values()) {
            for (String reference : type.referencedClasses()) {
                if (!classes.containsKey(reference) && runtimeClassPath.ownerOf(reference).isEmpty()) {
                    unresolved.computeIfAbsent(reference, r -> new TreeSet<>()).add(type.name());
                }
            }
        }
        List<String> result = new ArrayList<>();
        unresolved.forEach((reference, from) -> result.add(
            module + ": " + reference + " (referenced by " + String.join(", ", from) + ") is not defined by the JDK, "
            + "the module, or any artifact in its runtime dependency tree; it can only come from a V2 runtime "
            + "artifact or a provided, test-scoped or undeclared dependency"));
        return result;
    }

    /**
     * Third-party logging implementation types in the public or protected API of public classes (Requirement 15.8):
     * supertypes, the class signature, and the descriptors, generic signatures and declared exceptions of public and
     * protected fields and methods.
     */
    public static List<String> loggingImplementationApiTypes(String module, Map<String, ClassFile> classes) {
        List<String> result = new ArrayList<>();
        for (ClassFile type : classes.values()) {
            if (!type.isPublic()) {
                continue;
            }
            String prefix = module + ": " + type.name() + " ";
            Set<String> supertypes = new TreeSet<>(type.interfaces());
            if (!type.superName().isEmpty()) {
                supertypes.add(type.superName());
            }
            report(result, prefix + "supertype", supertypes);
            // Type-parameter bounds and type arguments of generic supertypes.
            report(result, prefix + "class signature", ClassFile.classNamesIn(type.signature()));
            for (Member field : type.fields()) {
                if (field.isApi()) {
                    report(result, prefix + "field " + field.name(), typesOf(field));
                }
            }
            for (Member method : type.methods()) {
                if (method.isApi()) {
                    report(result, prefix + "method " + method.name() + method.descriptor(), typesOf(method));
                }
            }
        }
        return result;
    }

    private static Set<String> typesOf(Member member) {
        Set<String> result = new TreeSet<>(ClassFile.classNamesIn(member.descriptor()));
        result.addAll(ClassFile.classNamesIn(member.signature()));
        result.addAll(member.exceptions());
        return result;
    }

    private static void report(List<String> result, String where, Set<String> types) {
        for (String type : types) {
            if (ForbiddenClassNames.isLoggingImplementation(type)) {
                result.add(where + " exposes logging implementation type " + type);
            }
        }
    }
}
