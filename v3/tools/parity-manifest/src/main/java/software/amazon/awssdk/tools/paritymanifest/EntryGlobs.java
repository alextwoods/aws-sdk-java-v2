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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Entry ID globs and test scopes: a glob matches an entry ID literally except that {@code *} matches any characters;
 * a scope is a list of globs, where an element {@code !<glob>} excludes the entries it matches.
 */
public final class EntryGlobs {
    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    private EntryGlobs() {
    }

    /** The pattern of {@code glob}. */
    public static Pattern pattern(String glob) {
        return PATTERNS.computeIfAbsent(glob, g -> Pattern.compile(Pattern.quote(g).replace("*", "\\E.*\\Q")));
    }

    /** True if {@code glob} matches {@code id}. */
    public static boolean matches(String glob, String id) {
        return pattern(glob).matcher(id).matches();
    }

    /** True if {@code scopeElement} is an exclusion ({@code !<glob>}). */
    public static boolean isExclusion(String scopeElement) {
        return scopeElement.startsWith("!");
    }

    /** True if {@code id} is matched by an inclusion of {@code scope} and by none of its exclusions. */
    public static boolean inScope(String id, List<String> scope) {
        boolean included = false;
        for (String element : scope) {
            if (isExclusion(element)) {
                if (matches(element.substring(1), id)) {
                    return false;
                }
            } else if (!included && matches(element, id)) {
                included = true;
            }
        }
        return included;
    }
}
