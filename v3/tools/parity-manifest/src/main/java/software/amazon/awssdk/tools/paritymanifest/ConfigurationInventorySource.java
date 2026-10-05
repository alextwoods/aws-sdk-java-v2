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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Customer-facing V2 configuration keys: {@code config:env:<NAME>}, {@code config:sysprop:<name>} and
 * {@code config:profile:<key>}, read from the source of the public key holders in {@link #HOLDERS}:
 *
 * <ul>
 *   <li>{@code SystemSetting} enums whose environment variable is the constant name and whose system property is the
 *       first (string-literal) constructor argument: {@code SdkSystemSetting} (sdk-core) and
 *       {@code ProfileFileSystemSetting} (profiles);</li>
 *   <li>{@code ProxySystemSetting} (utils): system property only; {@code ProxyEnvironmentSetting} (utils):
 *       environment variable only, the first argument;</li>
 *   <li>{@code ProfileProperty} (profiles): {@code public static final String} profile keys.</li>
 * </ul>
 *
 * <p>Internal holders such as {@code AwsClientOption} or {@code internal} {@code SystemSetting}s are not
 * customer-facing configuration. Parsing strips comments, then reads enum constants up to the first {@code ;} of the
 * enum body; a constant (or {@code String} constant) is deprecated when annotated {@code @Deprecated}. A key reported
 * by more than one holder is kept once, from the first holder in {@link #HOLDERS} order.
 */
public final class ConfigurationInventorySource implements InventorySource {

    /** How a holder declares keys. */
    enum Style { ENV_NAME_AND_SYSPROP_ARG, SYSPROP_ARG, ENV_ARG, PROFILE_CONSTANTS }

    /** One source file declaring configuration keys. */
    record Holder(String artifact, String path, Style style) {
    }

    static final List<Holder> HOLDERS = List.of(
        new Holder("sdk-core", "core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkSystemSetting.java",
                   Style.ENV_NAME_AND_SYSPROP_ARG),
        new Holder("profiles",
                   "core/profiles/src/main/java/software/amazon/awssdk/profiles/ProfileFileSystemSetting.java",
                   Style.ENV_NAME_AND_SYSPROP_ARG),
        new Holder("profiles", "core/profiles/src/main/java/software/amazon/awssdk/profiles/ProfileProperty.java",
                   Style.PROFILE_CONSTANTS),
        new Holder("utils", "utils/src/main/java/software/amazon/awssdk/utils/ProxySystemSetting.java",
                   Style.SYSPROP_ARG),
        new Holder("utils", "utils/src/main/java/software/amazon/awssdk/utils/ProxyEnvironmentSetting.java",
                   Style.ENV_ARG));

    private static final Pattern ENUM_CONSTANT = Pattern.compile(
        "((?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*)\\b([A-Z][A-Z0-9_]*)\\s*\\(([^)]*)\\)");
    private static final Pattern STRING_CONSTANT = Pattern.compile(
        "((?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*)public\\s+static\\s+final\\s+String\\s+([A-Z][A-Z0-9_]*)\\s*=\\s*"
        + "\"([^\"]*)\"\\s*;");
    private static final Pattern ENUM_START = Pattern.compile("\\benum\\s+\\w+[^{]*\\{");

    private final List<Holder> holders;

    public ConfigurationInventorySource() {
        this(HOLDERS);
    }

    ConfigurationInventorySource(List<Holder> holders) {
        this.holders = List.copyOf(holders);
    }

    @Override
    public String name() {
        return "v2-configuration";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        Map<String, InventoryItem> items = new TreeMap<>();
        for (Holder holder : holders) {
            Path file = context.repositoryRoot().resolve(holder.path());
            String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
            String type = file.getFileName().toString().replace(".java", "");
            int before = items.size();
            if (holder.style() == Style.PROFILE_CONSTANTS) {
                Matcher m = STRING_CONSTANT.matcher(source);
                while (m.find()) {
                    add(items, holder, "profile", m.group(3), type + "." + m.group(2), deprecated(m.group(1)));
                }
            } else {
                Matcher start = ENUM_START.matcher(source);
                if (!start.find()) {
                    throw new IOException(file + " declares no enum");
                }
                int end = source.indexOf(';', start.end());
                String body = source.substring(start.end(), end < 0 ? source.length() : end);
                Matcher m = ENUM_CONSTANT.matcher(body);
                while (m.find()) {
                    String constant = m.group(2);
                    String first = m.group(3).split(",", -1)[0].strip();
                    String literal = first.startsWith("\"") && first.endsWith("\"") && first.length() >= 2
                                     ? first.substring(1, first.length() - 1) : null;
                    boolean deprecated = deprecated(m.group(1));
                    String surface = type + "." + constant;
                    switch (holder.style()) {
                        case ENV_NAME_AND_SYSPROP_ARG -> {
                            add(items, holder, "env", constant, surface, deprecated);
                            if (literal != null) {
                                add(items, holder, "sysprop", literal, surface, deprecated);
                            }
                        }
                        case SYSPROP_ARG -> {
                            if (literal != null) {
                                add(items, holder, "sysprop", literal, surface, deprecated);
                            }
                        }
                        case ENV_ARG -> {
                            if (literal != null) {
                                add(items, holder, "env", literal, surface, deprecated);
                            }
                        }
                        default -> throw new IllegalStateException(holder.style().name());
                    }
                }
            }
            if (items.size() == before) {
                throw new IOException(file + " yielded no configuration keys; has its declaration style changed?");
            }
        }
        return List.copyOf(new ArrayList<>(items.values()));
    }

    private static void add(Map<String, InventoryItem> items, Holder holder, String kind, String key, String surface,
                            boolean deprecated) {
        String id = EntryKind.configurationId(kind, key);
        items.putIfAbsent(id, new InventoryItem(id, EntryKind.CONFIGURATION, kind + " " + key + " (" + surface + ")",
                                                new SourceLocation(holder.artifact(), holder.path()), deprecated));
    }

    private static boolean deprecated(String annotations) {
        return annotations != null && annotations.matches("(?s).*@(?:java\\.lang\\.)?Deprecated\\b.*");
    }

    /** {@code source} with block and line comments replaced by a space, leaving string and char literals intact. */
    static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < n && source.charAt(j) != c) {
                    j += source.charAt(j) == '\\' ? 2 : 1;
                }
                j = Math.min(j + 1, n);
                out.append(source, i, j);
                i = j;
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                int close = source.indexOf("*/", i + 2);
                i = close < 0 ? n : close + 2;
                out.append(' ');
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                int eol = source.indexOf('\n', i);
                i = eol < 0 ? n : eol;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
