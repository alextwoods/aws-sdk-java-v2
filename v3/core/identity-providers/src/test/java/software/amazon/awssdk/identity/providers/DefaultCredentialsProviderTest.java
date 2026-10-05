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

package software.amazon.awssdk.identity.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.identity.providers.DefaultCredentialsProvider.Source;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.profiles.SystemSettings;

/** The default chain evaluates the sources in the Parity Manifest precedence (Requirement 13.3). */
class DefaultCredentialsProviderTest {
    @TempDir
    Path home;

    @Test
    void precedenceIsTheV2BaselineAndBuildsTheChainInOrder() {
        assertEquals(List.of(Source.SYSTEM_PROPERTIES, Source.ENVIRONMENT, Source.WEB_IDENTITY, Source.PROFILE,
                             Source.CONTAINER, Source.INSTANCE_PROFILE), DefaultCredentialsProvider.sourcePrecedence());
        List<Class<?>> types = DefaultCredentialsProvider.builder().settings(settings(Map.of(), Map.of())).build()
                                                         .chain().providers().stream()
                                                         .<Class<?>>map(Object::getClass).toList();
        assertEquals(List.of(SystemPropertyCredentialsProvider.class, EnvironmentVariableCredentialsProvider.class,
                             WebIdentityTokenFileCredentialsProvider.class, ProfileCredentialsProvider.class,
                             ContainerCredentialsProvider.class, InstanceProfileCredentialsProvider.class), types);
    }

    @Test
    void earlierSourcesWin() throws Exception {
        Files.createDirectories(home.resolve(".aws"));
        Files.writeString(home.resolve(".aws/credentials"), "[default]\naws_access_key_id=PROFILE\n"
                                                            + "aws_secret_access_key=S\n");
        Map<String, String> env = Map.of("AWS_ACCESS_KEY_ID", "ENV", "AWS_SECRET_ACCESS_KEY", "S");
        Map<String, String> props = Map.of("aws.accessKeyId", "PROP", "aws.secretAccessKey", "S");

        assertEquals("PROP", resolve(settings(props, env)).accessKeyId());
        assertEquals("ENV", resolve(settings(Map.of(), env)).accessKeyId());
        assertEquals("PROFILE", resolve(settings(Map.of(), Map.of())).accessKeyId());
    }

    @Test
    void failureListsEverySourceInOrder() {
        SystemSettings settings = settings(Map.of(), Map.of("AWS_EC2_METADATA_DISABLED", "true"));
        CompletionException e = assertThrows(CompletionException.class, () -> resolve(settings));
        String message = e.getCause().getMessage();
        assertTrue(message.startsWith("Unable to load credentials from any of the providers in the chain"), message);
        int system = message.indexOf("SystemPropertyCredentialsProvider()");
        int environment = message.indexOf("EnvironmentVariableCredentialsProvider()");
        int webIdentity = message.indexOf("WebIdentityTokenCredentialsProvider()");
        int profile = message.indexOf("ProfileCredentialsProvider(profileName=default)");
        int container = message.indexOf("ContainerCredentialsProvider()");
        int imds = message.indexOf("InstanceProfileCredentialsProvider()");
        assertTrue(0 < system && system < environment && environment < webIdentity && webIdentity < profile
                   && profile < container && container < imds, message);
    }

    private AwsCredentialsIdentity resolve(SystemSettings settings) {
        try (DefaultCredentialsProvider provider = DefaultCredentialsProvider.builder().settings(settings).build()) {
            return provider.resolveIdentity().join();
        }
    }

    private SystemSettings settings(Map<String, String> properties, Map<String, String> env) {
        Map<String, String> environment = new HashMap<>(env);
        environment.put("HOME", home.toString());
        return new SystemSettings(SettingSource.fromMap(properties), SettingSource.fromMap(environment));
    }
}
