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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * The default AWS credentials chain, with the V2 name (Requirements 13.3, 13.4). The sources are evaluated in the
 * precedence the Parity Manifest table {@value #PRECEDENCE_RESOURCE} records, which is the V2 baseline order:
 *
 * <ol>
 *   <li>{@link SystemPropertyCredentialsProvider} ({@code aws.accessKeyId}, ...);</li>
 *   <li>{@link EnvironmentVariableCredentialsProvider} ({@code AWS_ACCESS_KEY_ID}, ...);</li>
 *   <li>{@link WebIdentityTokenFileCredentialsProvider} ({@code AWS_WEB_IDENTITY_TOKEN_FILE} + {@code AWS_ROLE_ARN});</li>
 *   <li>{@link ProfileCredentialsProvider} (the active profile);</li>
 *   <li>{@link ContainerCredentialsProvider} ({@code AWS_CONTAINER_CREDENTIALS_*});</li>
 *   <li>{@link InstanceProfileCredentialsProvider} (EC2 instance metadata).</li>
 * </ol>
 *
 * <p>The chain is created on first resolution. By default the source that last succeeded is reused (V2
 * {@code reuseLastProviderEnabled}), and refreshable sources refresh their credentials; a rejected identity is
 * {@linkplain #invalidate forwarded} to its source. Closing releases every source's resources.
 */
public final class DefaultCredentialsProvider implements RefreshableIdentityProvider<AwsCredentialsIdentity>,
                                                         AutoCloseable {
    /** The classpath resource that records the default source precedence. */
    public static final String PRECEDENCE_RESOURCE = "META-INF/aws-sdk-java-v3/identity/default-credentials-precedence.txt";

    private static final List<Source> PRECEDENCE = readPrecedence();

    private final Builder builder;
    private AwsCredentialsProviderChain chain;

    private DefaultCredentialsProvider(Builder builder) {
        this.builder = builder.copy();
    }

    /** The default chain over the process's settings and default profile file. */
    public static DefaultCredentialsProvider create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The default credential sources, in evaluation order (the Parity Manifest precedence). */
    public static List<Source> sourcePrecedence() {
        return PRECEDENCE;
    }

    /** One default credential source. */
    public enum Source {
        SYSTEM_PROPERTIES("system-properties"),
        ENVIRONMENT("environment"),
        WEB_IDENTITY("web-identity"),
        PROFILE("profile"),
        CONTAINER("container"),
        INSTANCE_PROFILE("instance-profile");

        private final String id;

        Source(String id) {
            this.id = id;
        }

        /** The ID of the source in the precedence table. */
        public String id() {
            return id;
        }

        static Source of(String id) {
            for (Source source : values()) {
                if (source.id.equals(id)) {
                    return source;
                }
            }
            throw new IllegalStateException("Unknown default credential source '" + id + "' in " + PRECEDENCE_RESOURCE);
        }
    }

    /** The chain, created on first use. */
    public synchronized AwsCredentialsProviderChain chain() {
        if (chain == null) {
            AwsCredentialsProviderChain.Builder chainBuilder =
                AwsCredentialsProviderChain.builder().reuseLastProviderEnabled(builder.reuseLastProviderEnabled);
            for (Source source : PRECEDENCE) {
                chainBuilder.addCredentialsProvider(create(source));
            }
            chain = chainBuilder.build();
        }
        return chain;
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> create(Source source) {
        SystemSettings settings = builder.settings;
        return switch (source) {
            case SYSTEM_PROPERTIES -> SystemPropertyCredentialsProvider.create(settings.jvmProperties());
            case ENVIRONMENT -> EnvironmentVariableCredentialsProvider.create(settings.environment());
            case WEB_IDENTITY -> WebIdentityTokenFileCredentialsProvider.builder()
                                                                        .settings(settings)
                                                                        .httpClient(builder.httpClient)
                                                                        .clock(builder.clock)
                                                                        .executor(builder.executor)
                                                                        .build();
            case PROFILE -> ProfileCredentialsProvider.builder()
                                                      .settings(settings)
                                                      .profileFile(builder.profileFile)
                                                      .profileName(builder.profileName)
                                                      .imdsEndpoint(builder.imdsEndpoint)
                                                      .httpClient(builder.httpClient)
                                                      .clock(builder.clock)
                                                      .executor(builder.executor)
                                                      .build();
            case CONTAINER -> ContainerCredentialsProvider.builder()
                                                          .settings(settings)
                                                          .httpClient(builder.httpClient)
                                                          .clock(builder.clock)
                                                          .executor(builder.executor)
                                                          .build();
            case INSTANCE_PROFILE -> InstanceProfileCredentialsProvider.builder()
                                                                       .settings(settings)
                                                                       .profileFile(builder.profileFile)
                                                                       .profileName(builder.profileName)
                                                                       .endpoint(builder.imdsEndpoint)
                                                                       .httpClient(builder.httpClient)
                                                                       .clock(builder.clock)
                                                                       .executor(builder.executor)
                                                                       .build();
        };
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        return chain().resolveIdentity(request);
    }

    @Override
    public void invalidate(Identity identity) {
        chain().invalidate(identity);
    }

    @Override
    public void close() {
        AwsCredentialsProviderChain current;
        synchronized (this) {
            current = chain;
        }
        if (current != null) {
            current.close();
        }
    }

    @Override
    public String toString() {
        return "DefaultCredentialsProvider(providerChain=" + chain + ")";
    }

    private static List<Source> readPrecedence() {
        InputStream in = DefaultCredentialsProvider.class.getClassLoader().getResourceAsStream(PRECEDENCE_RESOURCE);
        if (in == null) {
            throw new IllegalStateException("Missing " + PRECEDENCE_RESOURCE);
        }
        List<Source> sources = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.strip();
                if (!id.isEmpty() && !id.startsWith("#")) {
                    Source source = Source.of(id);
                    if (sources.contains(source)) {
                        throw new IllegalStateException("Duplicate source '" + id + "' in " + PRECEDENCE_RESOURCE);
                    }
                    sources.add(source);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(sources);
    }

    /** Builder of a {@link DefaultCredentialsProvider}. */
    public static final class Builder {
        private SystemSettings settings = SystemSettings.system();
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private SdkSyncHttpClient httpClient;
        private String imdsEndpoint;
        private boolean reuseLastProviderEnabled = true;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        private Builder copy() {
            Builder copy = new Builder();
            copy.settings = settings;
            copy.profileFile = profileFile;
            copy.profileName = profileName;
            copy.httpClient = httpClient;
            copy.imdsEndpoint = imdsEndpoint;
            copy.reuseLastProviderEnabled = reuseLastProviderEnabled;
            copy.clock = clock;
            copy.executor = executor;
            return copy;
        }

        /** The JVM property and environment sources; the process's by default. */
        public Builder settings(SystemSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
            return this;
        }

        /** The profile file of the profile and instance-metadata sources; the default profile file by default. */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /** The profile; the active profile by default. */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /** A caller-owned HTTP client for every network source; by default the installed implementation. */
        public Builder httpClient(SdkSyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /** The IMDS endpoint, overriding the endpoint settings. */
        public Builder imdsEndpoint(String imdsEndpoint) {
            this.imdsEndpoint = imdsEndpoint;
            return this;
        }

        /** Whether the source that last succeeded is reused; default true (V2). */
        public Builder reuseLastProviderEnabled(boolean enabled) {
            this.reuseLastProviderEnabled = enabled;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /** Where blocking loads run; virtual threads by default. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public DefaultCredentialsProvider build() {
            return new DefaultCredentialsProvider(this);
        }
    }
}
