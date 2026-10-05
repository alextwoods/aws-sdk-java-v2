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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.providers.internal.CachingIdentityProvider;
import software.amazon.awssdk.identity.providers.internal.Json;
import software.amazon.awssdk.identity.providers.internal.LoadedCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.cache.IdentityCache;
import software.amazon.awssdk.identity.spi.cache.RefreshResult;

/**
 * Credentials printed by an external command ({@code credential_process}), with the V2 name and semantics: the command
 * runs through {@code sh -c} ({@code cmd.exe /C} on Windows); a non-zero exit status or more than
 * {@code processOutputLimit} bytes (default 64000) of output fails; the output is a {@code Version} 1 JSON document;
 * credentials with an {@code Expiration} go stale at expiry and are prefetched {@code credentialRefreshThreshold}
 * (default 15 seconds) before it, and credentials without one are kept indefinitely.
 */
public final class ProcessCredentialsProvider extends CachingIdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "ProcessCredentialsProvider";

    private final List<String> command;
    private final long outputLimit;
    private final Duration refreshThreshold;
    private final String staticAccountId;

    private ProcessCredentialsProvider(Builder builder) {
        super(AwsCredentialsIdentity.class, PROVIDER_NAME, builder.clock, builder.executor,
              IdentityCache.StaleValueBehavior.STRICT);
        String text = Objects.requireNonNull(builder.command, "command");
        this.command = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                       ? List.of("cmd.exe", "/C", text)
                       : List.of("sh", "-c", text);
        this.outputLimit = builder.processOutputLimit;
        this.refreshThreshold = builder.credentialRefreshThreshold;
        this.staticAccountId = builder.staticAccountId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected RefreshResult<AwsCredentialsIdentity> load() {
        String output = run();
        Map<String, Object> document;
        try {
            document = Json.parseObject(output);
        } catch (IllegalArgumentException e) {
            throw SdkClientException.create("Failed to parse the credentials process output: " + e.getMessage(), e);
        }
        Object version = document.get("Version");
        if (!(version instanceof BigDecimal number) || number.intValue() != 1) {
            throw SdkClientException.create("Unsupported credential version: " + version);
        }
        AwsCredentialsIdentity credentials = LoadedCredentials.credentials(document, "SessionToken", staticAccountId,
                                                                           PROVIDER_NAME, "the credentials process");
        Instant expiration = credentials.expirationTime().orElse(null);
        if (expiration == null) {
            return RefreshResult.of(credentials);
        }
        return RefreshResult.of(credentials, expiration, expiration.minus(refreshThreshold));
    }

    private String run() {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start the credentials process", e);
        }
        try (InputStream in = process.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > outputLimit) {
                    process.destroyForcibly();
                    throw SdkClientException.create("Process output exceeded limit of " + outputLimit + " bytes.");
                }
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw SdkClientException.create("Command returned non-zero exit value: " + exit);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the credentials process output", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw SdkClientException.create("Interrupted while running the credentials process", e);
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "()";
    }

    /** Builder of a {@link ProcessCredentialsProvider}. */
    public static final class Builder {
        private String command;
        private long processOutputLimit = 64000;
        private Duration credentialRefreshThreshold = Duration.ofSeconds(15);
        private String staticAccountId;
        private Clock clock;
        private Executor executor;

        private Builder() {
        }

        /** The command line. Required. */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder processOutputLimit(long processOutputLimit) {
            this.processOutputLimit = processOutputLimit;
            return this;
        }

        public Builder credentialRefreshThreshold(Duration credentialRefreshThreshold) {
            this.credentialRefreshThreshold = Objects.requireNonNull(credentialRefreshThreshold, "threshold");
            return this;
        }

        /** The account ID used when the output has none (the profile's {@code aws_account_id}). */
        public Builder staticAccountId(String staticAccountId) {
            this.staticAccountId = staticAccountId;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public ProcessCredentialsProvider build() {
            return new ProcessCredentialsProvider(this);
        }
    }
}
