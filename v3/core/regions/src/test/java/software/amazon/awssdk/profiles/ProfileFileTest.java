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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/** Shared configuration file parsing (V2 rules) and the default region chain precedence. */
class ProfileFileTest {
    @Test
    void configurationFileRules() {
        ProfileFile file = ProfileFile.parse(String.join("\n",
            "# comment",
            "[default]",
            "region = us-west-2 ; trailing comment",
            "[profile dev]",
            "aws_access_key_id=AKID",
            "s3 =",
            "  max_concurrent_requests = 20",
            "[notprofile]",
            "ignored = yes",
            "[profile default]",
            "region = eu-west-1",
            "[sso-session my-sso]",
            "sso_region = us-east-1",
            "[profile bad name!]",
            "x = y"), ProfileFile.Type.CONFIGURATION);

        // [profile default] replaces an earlier [default].
        assertEquals("eu-west-1", file.profile("default").orElseThrow().property("region").orElseThrow());
        Profile dev = file.profile("dev").orElseThrow();
        assertEquals("AKID", dev.property("aws_access_key_id").orElseThrow());
        assertEquals("\nmax_concurrent_requests = 20", dev.property("s3").orElseThrow());
        assertEquals("20", dev.property("s3.max_concurrent_requests").orElseThrow());
        assertFalse(file.profile("notprofile").isPresent());
        assertFalse(file.profile("bad name!").isPresent());
        assertEquals("us-east-1", file.section(ProfileFile.SSO_SESSION_SECTION, "my-sso").orElseThrow()
                                      .property("sso_region").orElseThrow());
    }

    @Test
    void credentialsFileNamesProfilesWithoutPrefixAndOverridesConfiguration() {
        ProfileFile config = ProfileFile.parse("[profile p]\nregion=us-east-1\naws_access_key_id=config\n",
                                               ProfileFile.Type.CONFIGURATION);
        ProfileFile credentials = ProfileFile.parse("[p]\naws_access_key_id=credentials\n",
                                                    ProfileFile.Type.CREDENTIALS);
        Profile merged = ProfileFile.aggregate(List.of(config, credentials)).profile("p").orElseThrow();
        assertEquals("credentials", merged.property("aws_access_key_id").orElseThrow());
        assertEquals("us-east-1", merged.property("region").orElseThrow());
        assertFalse(merged.toString().contains("credentials=")); // values are never rendered
    }

    @Test
    void syntaxErrorsNameTheLine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> ProfileFile.parse("[default]\nnoequals\n",
                                                                          ProfileFile.Type.CONFIGURATION));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
        assertThrows(IllegalArgumentException.class,
                     () -> ProfileFile.parse("key=value\n", ProfileFile.Type.CONFIGURATION));
    }

    @Test
    void locationsAndDefaultRegionChainPrecedence(@TempDir Path home) throws Exception {
        Files.createDirectories(home.resolve(".aws"));
        Files.writeString(home.resolve(".aws/config"), "[profile work]\nregion = ap-south-1\n[default]\nregion=us-east-2\n");
        SettingSource env = SettingSource.fromMap(Map.of("HOME", home.toString(), "AWS_PROFILE", "work"));
        SystemSettings settings = new SystemSettings(SettingSource.empty(), env);
        assertEquals(home.resolve(".aws/config"), ProfileFileLocation.configurationFilePath(settings));
        assertEquals("work", ProfileFileLocation.profileName(settings));

        assertEquals(Region.AP_SOUTH_1, DefaultAwsRegionProviderChain.builder().settings(settings).build().getRegion());

        SystemSettings withEnv = new SystemSettings(SettingSource.empty(), SettingSource.fromMap(
            Map.of("HOME", home.toString(), "AWS_PROFILE", "work", "AWS_REGION", "eu-north-1")));
        assertEquals(Region.EU_NORTH_1, DefaultAwsRegionProviderChain.builder().settings(withEnv).build().getRegion());

        SystemSettings withProperty = new SystemSettings(SettingSource.fromMap(Map.of("aws.region", "sa-east-1")),
                                                         withEnv.environment());
        assertEquals(Region.SA_EAST_1, DefaultAwsRegionProviderChain.builder().settings(withProperty).build()
                                                                    .getRegion());

        assertEquals(Region.CA_CENTRAL_1, DefaultAwsRegionProviderChain.builder()
                                                                       .settings(new SystemSettings(
                                                                           SettingSource.empty(),
                                                                           SettingSource.fromMap(Map.of(
                                                                               "HOME", home.resolve("none").toString()))))
                                                                       .instanceMetadataRegionProvider(
                                                                           () -> Region.CA_CENTRAL_1)
                                                                       .build().getRegion());
        SdkClientException none = assertThrows(SdkClientException.class, () -> DefaultAwsRegionProviderChain
            .builder().settings(new SystemSettings(SettingSource.empty(), SettingSource.fromMap(
                Map.of("HOME", home.resolve("none").toString())))).build().getRegion());
        assertTrue(none.getMessage().contains("Unable to load region"), none.getMessage());
    }
}
