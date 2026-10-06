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

package software.amazon.awssdk.v2migration.recipes.modern;

import java.util.Optional;
import software.amazon.awssdk.v2migration.manifest.EntryKind;

/** Binary and source names of the Parity Manifest IDs these recipes read. */
final class Names {
    private static final String API = "api:";

    private Names() {
    }

    /** The binary name of a {@link EntryKind#PUBLIC_TYPE} ID. */
    static Optional<String> type(String id) {
        return id != null && EntryKind.PUBLIC_TYPE.accepts(id) ? Optional.of(id.substring(API.length()))
                                                                : Optional.empty();
    }

    /** The source (canonical) form of a binary name: nested types separated by dots. */
    static String canonical(String binaryName) {
        return binaryName.replace('$', '.');
    }

    /** The package of a binary name. */
    static String packageName(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? "" : binaryName.substring(0, dot);
    }

    /** The name of a binary name within its package, for example {@code RetryStrategy$Builder}. */
    static String nestedName(String binaryName) {
        return binaryName.substring(binaryName.lastIndexOf('.') + 1);
    }

    /** The enclosing type of a nested binary name, if it is nested. */
    static Optional<String> enclosing(String binaryName) {
        int dollar = binaryName.lastIndexOf('$');
        return dollar < 0 || dollar < binaryName.lastIndexOf('.') ? Optional.empty()
                                                                  : Optional.of(binaryName.substring(0, dollar));
    }

    /**
     * A replacement ID for diagnostics: the source name of a type, or the {@code Type#member} form of a member ID
     * without its descriptor.
     */
    static String display(String id) {
        if (!id.startsWith(API)) {
            return id;
        }
        String body = id.substring(API.length());
        int hash = body.indexOf('#');
        if (hash < 0) {
            return canonical(body);
        }
        String member = body.substring(hash + 1).split("[(:]", 2)[0];
        return canonical(body.substring(0, hash)) + "#" + member;
    }
}
