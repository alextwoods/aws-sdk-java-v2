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

package software.amazon.awssdk.awscodegen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.Symbol;

/**
 * Import table of one generated Java file.
 *
 * <p>The first symbol referenced under a simple name owns that name for the whole file: it is written by its simple
 * name (and imported unless it is in {@code java.lang} or the file's own package), while any later symbol with the
 * same simple name is written fully qualified. Imports are rendered sorted, so the output does not depend on the
 * order in which distinct names were first referenced.
 */
public final class JavaImports implements ImportContainer {

    private final String packageName;
    private final Set<String> packageTypeNames;
    private final Map<String, String> simpleNames = new HashMap<>();
    private final Set<String> imports = new TreeSet<>();

    JavaImports(String packageName) {
        this(packageName, Set.of());
    }

    /**
     * @param packageTypeNames simple names of the types generated into {@code packageName}. A same-package type
     *        shadows the {@code java.lang} type of the same simple name (for example a modeled
     *        {@code IllegalArgumentException} or {@code UnsupportedOperationException} error), so such a
     *        {@code java.lang} type never owns its simple name in this file and is written fully qualified.
     */
    JavaImports(String packageName, Set<String> packageTypeNames) {
        this.packageName = packageName;
        this.packageTypeNames = Set.copyOf(packageTypeNames);
    }

    /** Java has no import aliases, so {@code alias} is ignored. */
    @Override
    public void importSymbol(Symbol symbol, String alias) {
        String namespace = symbol.getNamespace();
        if (namespace.isEmpty()) {
            return;
        }
        if (namespace.equals("java.lang") && packageTypeNames.contains(symbol.getName())) {
            return;
        }
        String qualified = qualifiedName(symbol);
        if (simpleNames.putIfAbsent(symbol.getName(), qualified) == null
            && !namespace.equals(packageName) && !namespace.equals("java.lang")) {
            imports.add(qualified);
        }
    }

    /**
     * Claims {@code simpleName} for {@code qualifiedName} without importing it: for the file's own type and its nested
     * types, which templates write by simple name. Any other symbol with that simple name is then written fully
     * qualified. Has no effect if the name is already owned.
     */
    void reserve(String simpleName, String qualifiedName) {
        simpleNames.putIfAbsent(simpleName, qualifiedName);
    }

    /** How {@code symbol} is written in this file: its simple name if it owns that name, else fully qualified. */
    String reference(Symbol symbol) {
        if (symbol.getNamespace().isEmpty()) {
            return symbol.getName();
        }
        String qualified = qualifiedName(symbol);
        return qualified.equals(simpleNames.get(symbol.getName())) ? symbol.getName() : qualified;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (String qualified : imports) {
            result.append("import ").append(qualified).append(";\n");
        }
        return result.toString();
    }

    private static String qualifiedName(Symbol symbol) {
        return symbol.getNamespace() + "." + symbol.getName();
    }
}
