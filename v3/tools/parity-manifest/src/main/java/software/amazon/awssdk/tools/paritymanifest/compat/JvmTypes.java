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

package software.amazon.awssdk.tools.paritymanifest.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM descriptor parsing (JVMS 4.3) and the Java source spelling of erased types, for generated callers.
 *
 * <p>Source names replace every {@code $} of a binary name with {@code .}: SDK API types use {@code $} only as the
 * nesting separator, and local or anonymous classes are never API.
 */
final class JvmTypes {
    private JvmTypes() {
    }

    /** The field descriptors of a method descriptor's parameters. */
    static List<String> parameters(String methodDescriptor) {
        List<String> result = new ArrayList<>();
        int i = 1;
        while (methodDescriptor.charAt(i) != ')') {
            int end = end(methodDescriptor, i);
            result.add(methodDescriptor.substring(i, end));
            i = end;
        }
        return result;
    }

    /** The field descriptor of a method descriptor's return type ({@code V} for void). */
    static String returnType(String methodDescriptor) {
        return methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
    }

    /** The Java source spelling of the erased type a field descriptor (or {@code V}) denotes. */
    static String sourceType(String descriptor) {
        return switch (descriptor.charAt(0)) {
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'D' -> "double";
            case 'F' -> "float";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'S' -> "short";
            case 'Z' -> "boolean";
            case 'V' -> "void";
            case '[' -> sourceType(descriptor.substring(1)) + "[]";
            case 'L' -> sourceName(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
            default -> throw new IllegalArgumentException("Not a field descriptor: " + descriptor);
        };
    }

    /** A typed source expression of a field descriptor's type that a caller can pass as an argument. */
    static String argument(String descriptor) {
        return switch (descriptor.charAt(0)) {
            case 'Z' -> "false";
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S' -> "(" + sourceType(descriptor) + ") 0";
            default -> "(" + sourceType(descriptor) + ") null";
        };
    }

    /** The binary name of the class a field descriptor names after stripping array dimensions, or {@code null}. */
    static String className(String descriptor) {
        String element = descriptor.replaceFirst("^\\[+", "");
        return element.charAt(0) == 'L' ? element.substring(1, element.length() - 1).replace('/', '.') : null;
    }

    /** The Java source spelling of a binary class name. */
    static String sourceName(String binaryName) {
        return binaryName.replace('$', '.');
    }

    /** The simple names of a binary class name's nesting chain, outermost first ({@code A$B} gives A, B). */
    static List<String> simpleNames(String binaryName) {
        String simple = binaryName.substring(binaryName.lastIndexOf('.') + 1);
        return List.of(simple.split("\\$"));
    }

    /** The package of a binary class name, empty for the unnamed package. */
    static String packageName(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? "" : binaryName.substring(0, dot);
    }

    /** True for names of JDK platform types, whose hierarchy is read from the running JDK. */
    static boolean isJdk(String binaryName) {
        return binaryName.startsWith("java.") || binaryName.startsWith("javax.");
    }

    private static int end(String descriptor, int start) {
        int i = start;
        while (descriptor.charAt(i) == '[') {
            i++;
        }
        return descriptor.charAt(i) == 'L' ? descriptor.indexOf(';', i) + 1 : i + 1;
    }
}
