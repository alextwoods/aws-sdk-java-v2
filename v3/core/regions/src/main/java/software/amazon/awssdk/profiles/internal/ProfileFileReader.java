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

package software.amazon.awssdk.profiles.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parser of the shared configuration and credentials file format, with the V2 {@code ProfileFileReader} rules: comment
 * lines, trailing comments, {@code [profile name]} vs {@code [name]} sections per file type, {@code [default]} vs
 * {@code [profile default]} precedence, reserved sections ({@code sso-session}, {@code services}), continuation lines,
 * sub-properties, invalid-identifier skipping and later-duplicate-wins. Not a public API.
 */
public final class ProfileFileReader {
    /** Title of the ordinary profile section. */
    public static final String PROFILES = "profile";
    /** Reserved section titles besides {@link #PROFILES}, in the configuration file only. */
    public static final List<String> RESERVED_SECTIONS = List.of("sso-session", "services");

    private static final Pattern EMPTY_LINE = Pattern.compile("^[\t ]*$");
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_\\-/.%@:+]*$");

    private final boolean configuration;
    private final Map<String, Map<String, Map<String, String>>> sections = new LinkedHashMap<>();
    private int lineNumber;
    private String section = PROFILES;
    private String currentProfile;
    private String currentProperty;
    private boolean ignoringProfile;
    private boolean ignoringProperty;
    private boolean subProperties;
    private boolean seenPrefixedDefault;

    private ProfileFileReader(boolean configuration) {
        this.configuration = configuration;
        sections.put(PROFILES, new LinkedHashMap<>());
        RESERVED_SECTIONS.forEach(title -> sections.put(title, new LinkedHashMap<>()));
    }

    /**
     * Parses {@code content}: section title → section name → properties, in file order.
     *
     * @param configuration true for the configuration file, false for the credentials file
     * @throws IllegalArgumentException naming the line of a syntax error
     */
    public static Map<String, Map<String, Map<String, String>>> parse(String content, boolean configuration) {
        ProfileFileReader reader = new ProfileFileReader(configuration);
        content.lines().forEach(reader::parseLine);
        return reader.sections;
    }

    private void parseLine(String line) {
        lineNumber++;
        if (EMPTY_LINE.matcher(line).matches() || line.startsWith("#") || line.startsWith(";")) {
            return;
        }
        if (line.startsWith("[")) {
            readSectionLine(line);
        } else if (line.startsWith(" ") || line.startsWith("\t")) {
            readContinuationLine(line);
        } else {
            readPropertyLine(line);
        }
    }

    private void readSectionLine(String line) {
        String trimmed = removeTrailingComments(line, "#", ";").strip();
        require(trimmed.endsWith("]"), "Profile definition must end with ']'");
        String raw = trimmed.substring(1, trimmed.length() - 1).strip();
        String reserved = RESERVED_SECTIONS.stream().filter(title -> hasPrefix(raw, title)).findFirst().orElse(null);
        String name;
        if (reserved != null) {
            section = reserved;
            name = configuration ? raw.substring(reserved.length()).strip() : null;
        } else {
            section = PROFILES;
            name = profileName(raw);
        }
        if (name == null || !VALID_IDENTIFIER.matcher(name).matches()) {
            ignoringProfile = true;
            return;
        }
        currentProfile = name;
        currentProperty = null;
        ignoringProfile = false;
        ignoringProperty = false;
        sections.get(section).computeIfAbsent(name, n -> new LinkedHashMap<>());
    }

    private String profileName(String raw) {
        if (!configuration) {
            return raw;
        }
        boolean prefixed = hasPrefix(raw, PROFILES);
        String name;
        if (prefixed) {
            name = raw.substring(PROFILES.length()).strip();
        } else if (raw.equals("default")) {
            name = "default";
        } else {
            return null;
        }
        if (name.equals("default")) {
            boolean seen = sections.get(PROFILES).containsKey("default");
            if (seen && !prefixed && seenPrefixedDefault) {
                return null;
            }
            if (seen && prefixed && !seenPrefixedDefault) {
                sections.get(PROFILES).remove("default");
            }
            if (prefixed) {
                seenPrefixedDefault = true;
            }
        }
        return name;
    }

    private void readPropertyLine(String line) {
        if (ignoringProfile) {
            return;
        }
        require(currentProfile != null, "Expected a profile definition");
        String trimmed = removeTrailingComments(line, " #", " ;", "\t#", "\t;").strip();
        String[] property = property(trimmed);
        if (property == null) {
            ignoringProperty = true;
            return;
        }
        currentProperty = property[0];
        ignoringProperty = false;
        subProperties = property[1].isEmpty();
        sections.get(section).get(currentProfile).put(property[0], property[1]);
    }

    private void readContinuationLine(String line) {
        if (ignoringProfile || ignoringProperty) {
            return;
        }
        require(currentProfile != null && currentProperty != null, "Expected a profile or property definition");
        String trimmed = line.strip();
        Map<String, String> properties = sections.get(section).get(currentProfile);
        if (subProperties) {
            String[] sub = property(trimmed);
            if (sub != null) {
                properties.put(currentProperty + "." + sub[0], sub[1]);
            }
        }
        properties.put(currentProperty, properties.get(currentProperty) + "\n" + trimmed);
    }

    /** {@code [key, value]}, or null if the key is not a valid identifier. */
    private String[] property(String line) {
        int equals = line.indexOf('=');
        require(equals != -1, "Expected an '=' sign defining a property");
        String key = line.substring(0, equals).strip();
        require(!key.isEmpty(), "Property did not have a name");
        if (!VALID_IDENTIFIER.matcher(key).matches()) {
            return null;
        }
        return new String[] {key, line.substring(equals + 1).strip()};
    }

    private static boolean hasPrefix(String raw, String title) {
        return raw.startsWith(title + " ") || raw.startsWith(title + "\t");
    }

    private static String removeTrailingComments(String line, String... markers) {
        int end = line.length();
        for (String marker : markers) {
            int index = line.indexOf(marker);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return line.substring(0, end);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message + " on line " + lineNumber);
        }
    }
}
