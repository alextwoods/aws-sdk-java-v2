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

package software.amazon.awssdk.profiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.profiles.internal.ProfileFileReader;
import software.amazon.awssdk.utils.ToString;

/**
 * The parsed shared configuration ({@code ~/.aws/config}) and credentials ({@code ~/.aws/credentials}) files, with the
 * V2 name, parsing rules and members ({@code profile(String)}, {@code profiles()}, {@code getSection(String)},
 * {@code defaultProfileFile()}, {@code builder()}).
 *
 * <p>A profile file holds sections: ordinary {@linkplain #profiles() profiles}, and in the configuration file the
 * reserved {@value #SSO_SESSION_SECTION} and {@value #SERVICES_SECTION} sections. An {@link #aggregate aggregate} of
 * several files merges same-named sections property by property, later files winning; the default file is the
 * configuration file followed by the credentials file, so credentials-file values win. Immutable.
 */
public final class ProfileFile {
    /** Title of the ordinary profile section ({@code [profile name]} in the configuration file). */
    public static final String PROFILES_SECTION_TITLE = ProfileFileReader.PROFILES;
    /** Title of the {@code [sso-session name]} sections. */
    public static final String SSO_SESSION_SECTION = "sso-session";
    /** Title of the {@code [services name]} sections. */
    public static final String SERVICES_SECTION = "services";

    private static final ProfileFile EMPTY = new ProfileFile(Map.of());

    private final Map<String, Map<String, Profile>> sections;

    private ProfileFile(Map<String, Map<String, Profile>> sections) {
        Map<String, Map<String, Profile>> copy = new LinkedHashMap<>();
        sections.forEach((title, profiles) -> copy.put(title, Collections.unmodifiableMap(new LinkedHashMap<>(profiles))));
        this.sections = Collections.unmodifiableMap(copy);
    }

    /** Which of the two shared files a content is. */
    public enum Type {
        /** The configuration file: profiles are {@code [profile name]} (or {@code [default]}). */
        CONFIGURATION,
        /** The credentials file: profiles are {@code [name]}. */
        CREDENTIALS
    }

    /** A file with no sections. */
    public static ProfileFile empty() {
        return EMPTY;
    }

    /**
     * Parses {@code content} as a file of {@code type}.
     *
     * @throws IllegalArgumentException naming the line of a syntax error
     */
    public static ProfileFile parse(String content, Type type) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(type, "type");
        Map<String, Map<String, Profile>> sections = new LinkedHashMap<>();
        ProfileFileReader.parse(content, type == Type.CONFIGURATION).forEach((title, entries) -> {
            Map<String, Profile> profiles = new LinkedHashMap<>();
            entries.forEach((name, properties) -> profiles.put(name, Profile.create(name, properties)));
            sections.put(title, profiles);
        });
        return new ProfileFile(sections);
    }

    /**
     * Reads and parses {@code path} as a file of {@code type}.
     *
     * @throws UncheckedIOException if the file cannot be read
     * @throws IllegalArgumentException naming the file and line of a syntax error
     */
    public static ProfileFile read(Path path, Type type) {
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read profile file " + path, e);
        }
        try {
            return parse(content, type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid profile file " + path + ": " + e.getMessage(), e);
        }
    }

    /** {@code files} merged in order; for each section, properties of later files win. */
    public static ProfileFile aggregate(List<ProfileFile> files) {
        Map<String, Map<String, Map<String, String>>> merged = new LinkedHashMap<>();
        for (ProfileFile file : files) {
            file.sections.forEach((title, profiles) -> profiles.forEach((name, profile) -> {
                merged.computeIfAbsent(title, t -> new LinkedHashMap<>())
                      .computeIfAbsent(name, n -> new LinkedHashMap<>())
                      .putAll(profile.properties());
            }));
        }
        Map<String, Map<String, Profile>> sections = new LinkedHashMap<>();
        merged.forEach((title, entries) -> {
            Map<String, Profile> profiles = new LinkedHashMap<>();
            entries.forEach((name, properties) -> profiles.put(name, Profile.create(name, properties)));
            sections.put(title, profiles);
        });
        return new ProfileFile(sections);
    }

    /** The default profile file of the process; see {@link #defaultProfileFile(SystemSettings)}. */
    public static ProfileFile defaultProfileFile() {
        return defaultProfileFile(SystemSettings.system());
    }

    /**
     * The configuration file followed by the credentials file at their {@linkplain ProfileFileLocation locations};
     * a missing file contributes nothing.
     */
    public static ProfileFile defaultProfileFile(SystemSettings settings) {
        Path config = ProfileFileLocation.configurationFilePath(settings);
        Path credentials = ProfileFileLocation.credentialsFilePath(settings);
        return aggregate(List.of(Files.isRegularFile(config) ? read(config, Type.CONFIGURATION) : EMPTY,
                                 Files.isRegularFile(credentials) ? read(credentials, Type.CREDENTIALS) : EMPTY));
    }

    /** The profile named {@code profileName}, if present. */
    public Optional<Profile> profile(String profileName) {
        return section(PROFILES_SECTION_TITLE, profileName);
    }

    /** Every profile, in file order. Immutable. */
    public Map<String, Profile> profiles() {
        return getSection(PROFILES_SECTION_TITLE);
    }

    /** The {@code sectionTitle} section named {@code name} (for example an {@code sso-session}), if present. */
    public Optional<Profile> section(String sectionTitle, String name) {
        return Optional.ofNullable(getSection(sectionTitle).get(Objects.requireNonNull(name, "name")));
    }

    /** Every section with title {@code sectionTitle}, in file order. Immutable. */
    public Map<String, Profile> getSection(String sectionTitle) {
        return sections.getOrDefault(Objects.requireNonNull(sectionTitle, "sectionTitle"), Map.of());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProfileFile that && sections.equals(that.sections);
    }

    @Override
    public int hashCode() {
        return sections.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileFile").add("profiles", profiles().keySet()).build();
    }
}
