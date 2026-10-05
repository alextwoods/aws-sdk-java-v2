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

package software.amazon.awssdk.core.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.core.resource.ResourceOwnership;
import software.amazon.awssdk.smithy.runtime.version.RuntimeModuleVersion;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionHandshake;
import software.amazon.awssdk.smithy.runtime.version.RuntimeVersionMismatchException;

class ClientConfigurationResolverTest {
    private static final ClientOption<String> REGION = ClientOption.create("Region", String.class);
    private static final ClientOption<URI> ENDPOINT = ClientOption.create("Endpoint", URI.class);
    private static final ClientOption<Integer> MAX_ATTEMPTS = ClientOption.create("MaxAttempts", Integer.class);
    private static final ClientOption<String> LEGACY_RETRY_POLICY = ClientOption.create("RetryPolicy", String.class);
    private static final ClientOption<AutoCloseable> HTTP_CLIENT = ClientOption.create("HttpClient", AutoCloseable.class);

    private static final ClientSetting<String> REGION_SETTING = ClientSetting.builder(REGION)
                                                                             .jvmProperty("aws.region")
                                                                             .environmentVariable("AWS_REGION")
                                                                             .profileProperty("region")
                                                                             .parser(text -> text)
                                                                             .defaultValue("us-east-1")
                                                                             .build();

    @Test
    void sourceOrderIsRequestOverrideBuilderJvmEnvironmentProfileDefault() {
        assertEquals(List.of(ConfigurationSource.REQUEST_OVERRIDE, ConfigurationSource.BUILDER,
                             ConfigurationSource.JVM_PROPERTY, ConfigurationSource.ENVIRONMENT,
                             ConfigurationSource.PROFILE, ConfigurationSource.DEFAULT),
                     Arrays.asList(ConfigurationSource.values()));
        assertTrue(ConfigurationSource.REQUEST_OVERRIDE.outranks(ConfigurationSource.BUILDER));
        assertFalse(ConfigurationSource.PROFILE.outranks(ConfigurationSource.PROFILE));
        assertFalse(ConfigurationSource.DEFAULT.outranks(ConfigurationSource.PROFILE));
    }

    @Test
    void eachSourceWinsOnlyWhenEveryHigherSourceIsAbsent() {
        Map<String, String> jvm = new HashMap<>(Map.of("aws.region", "jvm"));
        Map<String, String> env = new HashMap<>(Map.of("AWS_REGION", "env"));
        Map<String, String> profile = new HashMap<>(Map.of("region", "profile"));
        SdkClientConfiguration explicit = SdkClientConfiguration.builder().option(REGION, "builder").build();

        assertRegion(explicit, jvm, env, profile, "builder", ConfigurationSource.BUILDER);
        assertRegion(SdkClientConfiguration.empty(), jvm, env, profile, "jvm", ConfigurationSource.JVM_PROPERTY);
        jvm.clear();
        assertRegion(SdkClientConfiguration.empty(), jvm, env, profile, "env", ConfigurationSource.ENVIRONMENT);
        env.clear();
        assertRegion(SdkClientConfiguration.empty(), jvm, env, profile, "profile", ConfigurationSource.PROFILE);
        profile.clear();
        assertRegion(SdkClientConfiguration.empty(), jvm, env, profile, "us-east-1", ConfigurationSource.DEFAULT);
    }

    @Test
    void requestOverrideOutranksTheResolvedClientValue() {
        ResolvedOption<String> client = ResolvedOption.of(REGION, "builder", ConfigurationSource.BUILDER);
        ResolvedOption<String> override = ResolvedOption.of(REGION, "override", ConfigurationSource.REQUEST_OVERRIDE);
        ResolvedOption<String> profile = ResolvedOption.of(REGION, "profile", ConfigurationSource.PROFILE);

        assertEquals(Optional.of(override), ClientConfigurationResolver.select(Arrays.asList(profile, override, client)));
        assertEquals(Optional.of(client), ClientConfigurationResolver.select(Arrays.asList(null, profile, client)));
        assertEquals(Optional.empty(), ClientConfigurationResolver.select(List.of()));
        assertThrows(IllegalArgumentException.class,
                     () -> ClientConfigurationResolver.select(List.of(client, client)));
        assertThrows(IllegalArgumentException.class, () -> ClientConfigurationResolver.select(
            List.of(client, ResolvedOption.of(ClientOption.create("Region", String.class), "x",
                                              ConfigurationSource.DEFAULT))));
    }

    @Test
    void provenanceRecordsSourceAndOwnershipWithoutPrintingValues() {
        SdkClientConfiguration resolved = resolver(Map.of(), Map.of("AWS_REGION", "secret-region"), Map.of())
            .resolve(SdkClientConfiguration.empty());

        ResolvedOption<String> region = resolved.resolved(REGION).orElseThrow();
        assertEquals(new ResolvedOption<>(REGION, "secret-region", ConfigurationSource.ENVIRONMENT,
                                          ResourceOwnership.SDK_OWNED), region);
        assertEquals(Optional.of("secret-region"), resolved.get(REGION));
        assertFalse(region.toString().contains("secret-region"), region.toString());
        assertTrue(region.toString().contains("ENVIRONMENT"), region.toString());
        assertEquals(ResourceOwnership.CALLER_OWNED,
                     ResolvedOption.of(REGION, "r", ConfigurationSource.BUILDER).ownership());
        assertThrows(ClassCastException.class, () -> new ResolvedOption<>(
            castUnchecked(REGION), 42, ConfigurationSource.BUILDER, ResourceOwnership.CALLER_OWNED));
    }

    @Test
    void builderSourceOverloadKeepsProvenance() {
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(REGION, "profile", ConfigurationSource.PROFILE)
                                                                     .option(MAX_ATTEMPTS, 3)
                                                                     .build();

        assertEquals(ConfigurationSource.PROFILE, configuration.resolved(REGION).orElseThrow().source());
        assertEquals(ConfigurationSource.BUILDER, configuration.resolved(MAX_ATTEMPTS).orElseThrow().source());
        assertEquals(Optional.empty(), configuration.resolved(ENDPOINT));
        assertEquals(configuration, configuration.toBuilder().build());
        assertFalse(configuration.equals(SdkClientConfiguration.builder()
                                                                .option(REGION, "profile")
                                                                .option(MAX_ATTEMPTS, 3)
                                                                .build()));
        assertNull(configuration.toBuilder().option(REGION, null, ConfigurationSource.PROFILE).option(REGION));
    }

    @Test
    void clientConstantsAreResolvedOncePerClient() {
        AtomicInteger jvmReads = new AtomicInteger();
        AtomicInteger envReads = new AtomicInteger();
        AtomicInteger parses = new AtomicInteger();
        AtomicInteger defaults = new AtomicInteger();
        ClientSetting<Integer> attempts = ClientSetting.builder(MAX_ATTEMPTS)
                                                       .jvmProperty("aws.maxAttempts")
                                                       .environmentVariable("AWS_MAX_ATTEMPTS")
                                                       .parser(text -> {
                                                           parses.incrementAndGet();
                                                           return Integer.valueOf(text);
                                                       })
                                                       .build();
        ClientSetting<URI> endpoint = ClientSetting.builder(ENDPOINT)
                                                   .derivedDefault(resolved -> {
                                                       defaults.incrementAndGet();
                                                       return URI.create("https://service."
                                                                         + resolved.get(REGION).orElseThrow()
                                                                         + ".example.test");
                                                   })
                                                   .build();
        ConfigurationSources sources = ConfigurationSources.builder()
                                                           .jvmProperties(name -> {
                                                               jvmReads.incrementAndGet();
                                                               return Optional.empty();
                                                           })
                                                           .environment(name -> {
                                                               envReads.incrementAndGet();
                                                               return Optional.ofNullable(
                                                                   Map.of("AWS_MAX_ATTEMPTS", "5",
                                                                          "AWS_REGION", "eu-west-1").get(name));
                                                           })
                                                           .build();
        ClientConfigurationResolver resolver = ClientConfigurationResolver.builder()
                                                                          .sources(sources)
                                                                          .addSetting(REGION_SETTING)
                                                                          .addSetting(attempts)
                                                                          .addSetting(endpoint)
                                                                          .build();

        SdkClientConfiguration client = resolver.resolve(SdkClientConfiguration.empty());
        for (int call = 0; call < 100; call++) {
            assertEquals(Optional.of(5), client.get(MAX_ATTEMPTS));
            assertEquals(Optional.of(URI.create("https://service.eu-west-1.example.test")), client.get(ENDPOINT));
        }

        // One JVM and one environment read per setting that names them, one parse, one derived default.
        assertEquals(2, jvmReads.get());
        assertEquals(2, envReads.get());
        assertEquals(1, parses.get());
        assertEquals(1, defaults.get());
        assertEquals(ConfigurationSource.DEFAULT, client.resolved(ENDPOINT).orElseThrow().source());

        // A second client resolves its own constants once more.
        resolver.resolve(SdkClientConfiguration.empty());
        assertEquals(2, parses.get());
        assertEquals(2, defaults.get());
    }

    @Test
    void lowerSourcesAreNotReadOnceAValueIsFound() {
        List<String> reads = new ArrayList<>();
        ConfigurationSources sources = ConfigurationSources.builder()
                                                           .jvmProperties(name -> {
                                                               reads.add("jvm");
                                                               return Optional.of("jvm");
                                                           })
                                                           .environment(name -> {
                                                               reads.add("env");
                                                               return Optional.of("env");
                                                           })
                                                           .profile(name -> {
                                                               reads.add("profile");
                                                               return Optional.of("profile");
                                                           })
                                                           .build();
        ClientConfigurationResolver resolver = ClientConfigurationResolver.builder()
                                                                          .sources(sources)
                                                                          .addSetting(REGION_SETTING)
                                                                          .build();

        resolver.resolve(SdkClientConfiguration.builder().option(REGION, "builder").build());
        assertEquals(List.of(), reads);
        resolver.resolve(SdkClientConfiguration.empty());
        assertEquals(List.of("jvm"), reads);
    }

    @Test
    void invalidExternalTextIsRejectedWithoutPrintingIt() {
        ClientSetting<Integer> attempts = ClientSetting.builder(MAX_ATTEMPTS)
                                                       .environmentVariable("AWS_MAX_ATTEMPTS")
                                                       .parser(Integer::valueOf)
                                                       .build();
        ClientConfigurationResolver resolver =
            ClientConfigurationResolver.builder()
                                       .sources(ConfigurationSources.builder()
                                                                    .environment(SettingSource.fromMap(
                                                                        Map.of("AWS_MAX_ATTEMPTS", "s3cr3t")))
                                                                    .build())
                                       .addSetting(attempts)
                                       .build();

        SdkClientConfigurationException e = assertThrows(SdkClientConfigurationException.class,
                                                         () -> resolver.resolve(SdkClientConfiguration.empty()));
        assertEquals(Optional.of("MaxAttempts"), e.option());
        assertTrue(e.getMessage().contains("environment variable 'AWS_MAX_ATTEMPTS'"), e.getMessage());
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void unsupportedOptionsRejectConstructionByName() {
        ClientConfigurationResolver resolver = ClientConfigurationResolver.builder()
                                                                          .addSetting(REGION_SETTING)
                                                                          .addUnsupportedOption(LEGACY_RETRY_POLICY,
                                                                                                "EXCLUDED")
                                                                          .build();

        SdkClientConfigurationException e = assertThrows(
            SdkClientConfigurationException.class,
            () -> resolver.resolve(SdkClientConfiguration.builder()
                                                         .option(REGION, "us-west-2")
                                                         .option(LEGACY_RETRY_POLICY, "legacy")
                                                         .build()));
        assertEquals(Optional.of("RetryPolicy"), e.option());
        assertTrue(e.getMessage().contains("RetryPolicy (disposition EXCLUDED)"), e.getMessage());
        assertFalse(e.getMessage().contains("legacy"), e.getMessage());

        // Not supplying the option is fine.
        assertEquals(Optional.of("us-west-2"),
                     resolver.resolve(SdkClientConfiguration.builder().option(REGION, "us-west-2").build()).get(REGION));
        assertThrows(IllegalArgumentException.class,
                     () -> ClientConfigurationResolver.builder().addSetting(REGION_SETTING)
                                                      .addUnsupportedOption(REGION, "EXCLUDED").build());
        assertThrows(IllegalArgumentException.class,
                     () -> ClientConfigurationResolver.builder().addSetting(REGION_SETTING).addSetting(REGION_SETTING));
    }

    @Test
    void incompatibleRuntimeVersionsRejectConstructionNamingEveryModule() {
        RuntimeVersionHandshake handshake = RuntimeVersionHandshake.create(getClass().getClassLoader());
        List<RuntimeModuleVersion> sdkCore = handshake.discover(List.of("sdk-core")).get("sdk-core");
        assertFalse(sdkCore.isEmpty(), "the build generates sdk-core's version resource");
        String release = sdkCore.get(0).version();

        ClientConfigurationResolver.builder()
                                   .runtimeVersions(handshake, release, List.of("sdk-core", "smithy-runtime"))
                                   .build()
                                   .resolve(SdkClientConfiguration.empty());

        ClientConfigurationResolver mismatched =
            ClientConfigurationResolver.builder()
                                       .runtimeVersions(handshake, "3.999.0", List.of("sdk-core", "no-such-module"))
                                       .build();
        SdkClientConfigurationException e = assertThrows(SdkClientConfigurationException.class,
                                                         () -> mismatched.resolve(SdkClientConfiguration.empty()));
        RuntimeVersionMismatchException cause = assertInstanceOf(RuntimeVersionMismatchException.class, e.getCause());
        assertEquals(List.of("no-such-module", "sdk-core"),
                     cause.conflicts().stream().map(c -> c.artifactId()).toList());
        assertTrue(e.getMessage().contains("sdk-core") && e.getMessage().contains("no-such-module")
                   && e.getMessage().contains(release), e.getMessage());
    }

    @Test
    void closeableValuesAreRecordedWithTheirOwnership() {
        AutoCloseable callerClient = () -> { };
        AutoCloseable defaultClient = () -> { };
        ClientSetting<AutoCloseable> http = ClientSetting.builder(HTTP_CLIENT).defaultValue(defaultClient).build();
        ClientConfigurationResolver resolver = ClientConfigurationResolver.builder().addSetting(http).build();

        SdkClientConfiguration sdkCreated = resolver.resolve(SdkClientConfiguration.empty());
        assertEquals(List.of(ManagedResource.sdkOwned(defaultClient)), sdkCreated.resources());

        SdkClientConfiguration supplied = resolver.resolve(SdkClientConfiguration.builder()
                                                                                 .option(HTTP_CLIENT, callerClient)
                                                                                 .addResource(ManagedResource.callerOwned(
                                                                                     callerClient))
                                                                                 .build());
        assertEquals(List.of(ManagedResource.callerOwned(callerClient)), supplied.resources());
    }

    @Test
    void unresolvedBuilderOptionsPassThroughWithProvenance() {
        ClientOption<String> serviceOption = ClientOption.create("ServiceOption", String.class);
        SdkClientConfiguration resolved = ClientConfigurationResolver.builder().build().resolve(
            SdkClientConfiguration.builder().option(serviceOption, "v", ConfigurationSource.PROFILE).build());

        assertEquals(ResolvedOption.of(serviceOption, "v", ConfigurationSource.PROFILE),
                     resolved.resolved(serviceOption).orElseThrow());
    }

    @Test
    void systemSourcesReadTheProcessOnDemand() {
        String key = "aws.v3.test." + System.nanoTime();
        SettingSource jvm = ConfigurationSources.system().jvmProperties();
        assertEquals(Optional.empty(), jvm.setting(key));
        System.setProperty(key, "value");
        try {
            assertEquals(Optional.of("value"), jvm.setting(key));
        } finally {
            System.clearProperty(key);
        }
        assertEquals(Optional.empty(), ConfigurationSources.system().profile().setting("region"));
    }

    private static void assertRegion(SdkClientConfiguration builderValues, Map<String, String> jvm,
                                     Map<String, String> env, Map<String, String> profile,
                                     String expected, ConfigurationSource expectedSource) {
        ResolvedOption<String> region = resolver(jvm, env, profile).resolve(builderValues).resolved(REGION).orElseThrow();
        assertEquals(expected, region.value());
        assertEquals(expectedSource, region.source());
    }

    private static ClientConfigurationResolver resolver(Map<String, String> jvm, Map<String, String> env,
                                                        Map<String, String> profile) {
        return ClientConfigurationResolver.builder()
                                          .sources(ConfigurationSources.builder()
                                                                       .jvmProperties(SettingSource.fromMap(jvm))
                                                                       .environment(SettingSource.fromMap(env))
                                                                       .profile(SettingSource.fromMap(profile))
                                                                       .build())
                                          .addSetting(REGION_SETTING)
                                          .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> ClientOption<T> castUnchecked(ClientOption<?> option) {
        return (ClientOption<T>) option;
    }
}
