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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Where the shared files and the active profile name come from, with the V2 {@code ProfileFileLocation} and
 * {@code ProfileFileSystemSetting} settings:
 *
 * <ul>
 *   <li>configuration file: {@code aws.configFile} / {@code AWS_CONFIG_FILE}, else {@code ~/.aws/config};</li>
 *   <li>credentials file: {@code aws.sharedCredentialsFile} / {@code AWS_SHARED_CREDENTIALS_FILE}, else
 *       {@code ~/.aws/credentials};</li>
 *   <li>profile name: {@code aws.profile} / {@code AWS_PROFILE}, else {@code default}.</li>
 * </ul>
 *
 * <p>A leading {@code ~} is the user home directory: {@code HOME}, else {@code USERPROFILE}, else
 * {@code HOMEDRIVE}+{@code HOMEPATH}, else the {@code user.home} system property.
 */
public final class ProfileFileLocation {
    /** The JVM property naming the configuration file. */
    public static final String CONFIG_FILE_PROPERTY = "aws.configFile";
    /** The environment variable naming the configuration file. */
    public static final String CONFIG_FILE_ENVIRONMENT = "AWS_CONFIG_FILE";
    /** The JVM property naming the credentials file. */
    public static final String CREDENTIALS_FILE_PROPERTY = "aws.sharedCredentialsFile";
    /** The environment variable naming the credentials file. */
    public static final String CREDENTIALS_FILE_ENVIRONMENT = "AWS_SHARED_CREDENTIALS_FILE";
    /** The JVM property naming the active profile. */
    public static final String PROFILE_PROPERTY = "aws.profile";
    /** The environment variable naming the active profile. */
    public static final String PROFILE_ENVIRONMENT = "AWS_PROFILE";
    /** The profile used when none is named. */
    public static final String DEFAULT_PROFILE_NAME = "default";

    private ProfileFileLocation() {
    }

    /** The configuration file path of the process's settings. */
    public static Path configurationFilePath() {
        return configurationFilePath(SystemSettings.system());
    }

    /** The configuration file path of {@code settings}. */
    public static Path configurationFilePath(SystemSettings settings) {
        return resolve(settings, settings.value(CONFIG_FILE_PROPERTY, CONFIG_FILE_ENVIRONMENT), ".aws", "config");
    }

    /** The credentials file path of {@code settings}. */
    public static Path credentialsFilePath(SystemSettings settings) {
        return resolve(settings, settings.value(CREDENTIALS_FILE_PROPERTY, CREDENTIALS_FILE_ENVIRONMENT), ".aws",
                       "credentials");
    }

    /** The active profile name of {@code settings}. */
    public static String profileName(SystemSettings settings) {
        return settings.value(PROFILE_PROPERTY, PROFILE_ENVIRONMENT).map(String::strip).orElse(DEFAULT_PROFILE_NAME);
    }

    /** {@code path} with a leading {@code ~} replaced by the user home directory of {@code settings}. */
    public static Path expandHome(SystemSettings settings, String path) {
        if (path.equals("~") || path.startsWith("~/") || path.startsWith("~" + FileSystems.getDefault().getSeparator())) {
            return Paths.get(userHome(settings) + path.substring(1));
        }
        return Paths.get(path);
    }

    /** The user home directory of {@code settings}. */
    public static String userHome(SystemSettings settings) {
        Optional<String> home = settings.environment().setting("HOME").filter(v -> !v.isBlank());
        if (home.isPresent()) {
            return home.get();
        }
        Optional<String> profile = settings.environment().setting("USERPROFILE").filter(v -> !v.isBlank());
        if (profile.isPresent()) {
            return profile.get();
        }
        Optional<String> drive = settings.environment().setting("HOMEDRIVE");
        Optional<String> homePath = settings.environment().setting("HOMEPATH");
        if (drive.isPresent() && homePath.isPresent()) {
            return drive.get() + homePath.get();
        }
        return settings.jvmProperties().setting("user.home")
                       .orElseThrow(() -> new IllegalStateException("Unable to determine the user home directory."));
    }

    private static Path resolve(SystemSettings settings, Optional<String> configured, String directory, String file) {
        return configured.map(path -> expandHome(settings, path.strip()))
                         .orElseGet(() -> Paths.get(userHome(settings), directory, file));
    }
}
