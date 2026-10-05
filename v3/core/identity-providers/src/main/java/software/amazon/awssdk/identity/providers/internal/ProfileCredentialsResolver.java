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

package software.amazon.awssdk.identity.providers.internal;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.AwsCredentialsProviderChain;
import software.amazon.awssdk.identity.providers.ContainerCredentialsProvider;
import software.amazon.awssdk.identity.providers.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.identity.providers.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.identity.providers.ProcessCredentialsProvider;
import software.amazon.awssdk.identity.providers.SsoCredentialsProvider;
import software.amazon.awssdk.identity.providers.SsoTokenProvider;
import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
import software.amazon.awssdk.identity.providers.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.identity.providers.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.identity.providers.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * Chooses the credentials provider a profile configures, with the V2 {@code ProfileCredentialsUtils} precedence
 * (Requirement 13.4):
 *
 * <ol>
 *   <li>{@code role_arn} + {@code web_identity_token_file}: web identity;</li>
 *   <li>any of {@code sso_role_name}, {@code sso_account_id}, {@code sso_region}, {@code sso_start_url},
 *       {@code sso_session}: SSO;</li>
 *   <li>{@code role_arn} + {@code source_profile} (recursively, cycles rejected) or {@code credential_source}
 *       ({@code Environment}, {@code Ec2InstanceMetadata}, {@code EcsContainer}): STS {@code AssumeRole}; both
 *       together are rejected;</li>
 *   <li>{@code login_session}: rejected, not supported by this SDK version;</li>
 *   <li>{@code credential_process}: process;</li>
 *   <li>{@code aws_session_token}: session credentials; {@code aws_access_key_id}: basic credentials.</li>
 * </ol>
 *
 * <p>Not a public API.
 */
public final class ProfileCredentialsResolver {
    private final ProfileFile profileFile;
    private final Context context;

    /**
     * The transports, clocks and endpoint overrides of the providers a profile creates.
     *
     * @param settings JVM property and environment sources
     * @param httpClient caller-owned HTTP client, or null for the default implementation
     * @param stsEndpoint STS endpoint override, or null
     * @param ssoPortalEndpoint SSO portal endpoint override, or null
     * @param ssoOidcEndpoint SSO-OIDC endpoint override, or null
     * @param ssoCacheDirectory SSO token cache directory override, or null
     * @param imdsEndpoint IMDS endpoint override, or null
     * @param clock clock, or null
     * @param executor load executor, or null
     */
    public record Context(SystemSettings settings, SdkSyncHttpClient httpClient, URI stsEndpoint, URI ssoPortalEndpoint,
                          URI ssoOidcEndpoint, Path ssoCacheDirectory, String imdsEndpoint, Clock clock,
                          Executor executor) {
    }

    public ProfileCredentialsResolver(ProfileFile profileFile, Context context) {
        this.profileFile = profileFile;
        this.context = context;
    }

    /** The provider profile {@code profileName} configures, or empty if it configures none. */
    public Optional<IdentityProvider<? extends AwsCredentialsIdentity>> provider(String profileName) {
        return profileFile.profile(profileName).flatMap(profile -> provider(profile, new HashSet<>()));
    }

    private Optional<IdentityProvider<? extends AwsCredentialsIdentity>> provider(Profile profile,
                                                                                  Set<String> children) {
        Map<String, String> properties = profile.properties();
        if (properties.containsKey(ProfileProperty.ROLE_ARN)
            && properties.containsKey(ProfileProperty.WEB_IDENTITY_TOKEN_FILE)) {
            return Optional.of(webIdentity(profile));
        }
        if (properties.containsKey(ProfileProperty.SSO_ROLE_NAME) || properties.containsKey(ProfileProperty.SSO_ACCOUNT_ID)
            || properties.containsKey(ProfileProperty.SSO_REGION) || properties.containsKey(ProfileProperty.SSO_START_URL)
            || properties.containsKey(ProfileProperty.SSO_SESSION)) {
            return Optional.of(sso(profile));
        }
        if (properties.containsKey(ProfileProperty.ROLE_ARN)) {
            boolean hasSource = properties.containsKey(ProfileProperty.SOURCE_PROFILE);
            boolean hasCredentialSource = properties.containsKey(ProfileProperty.CREDENTIAL_SOURCE);
            if (hasSource && hasCredentialSource) {
                throw new IllegalStateException("Invalid profile file: profile has both " + ProfileProperty.SOURCE_PROFILE
                                                + " and " + ProfileProperty.CREDENTIAL_SOURCE + ".");
            }
            if (hasSource) {
                return Optional.of(roleWithSourceProfile(profile, children));
            }
            if (hasCredentialSource) {
                return Optional.of(assumeRole(profile, credentialSource(profile)));
            }
        }
        if (properties.containsKey(ProfileProperty.LOGIN_SESSION)) {
            throw SdkClientException.create("Profile '" + profile.name() + "' uses " + ProfileProperty.LOGIN_SESSION
                                            + ", which this SDK version does not support.");
        }
        if (properties.containsKey(ProfileProperty.CREDENTIAL_PROCESS)) {
            ProcessCredentialsProvider.Builder process =
                ProcessCredentialsProvider.builder()
                                          .command(properties.get(ProfileProperty.CREDENTIAL_PROCESS))
                                          .staticAccountId(properties.get(ProfileProperty.AWS_ACCOUNT_ID))
                                          .clock(context.clock());
            if (context.executor() != null) {
                process.executor(context.executor());
            }
            return Optional.of(process.build());
        }
        if (properties.containsKey(ProfileProperty.AWS_SESSION_TOKEN)) {
            require(profile, ProfileProperty.AWS_ACCESS_KEY_ID, ProfileProperty.AWS_SECRET_ACCESS_KEY,
                    ProfileProperty.AWS_SESSION_TOKEN);
            return Optional.of(StaticCredentialsProvider.create(
                AwsSessionCredentialsIdentity.builder()
                                             .accessKeyId(properties.get(ProfileProperty.AWS_ACCESS_KEY_ID))
                                             .secretAccessKey(properties.get(ProfileProperty.AWS_SECRET_ACCESS_KEY))
                                             .sessionToken(properties.get(ProfileProperty.AWS_SESSION_TOKEN))
                                             .accountId(properties.get(ProfileProperty.AWS_ACCOUNT_ID))
                                             .providerName("ProfileCredentialsProvider")
                                             .build()));
        }
        if (properties.containsKey(ProfileProperty.AWS_ACCESS_KEY_ID)) {
            require(profile, ProfileProperty.AWS_ACCESS_KEY_ID, ProfileProperty.AWS_SECRET_ACCESS_KEY);
            return Optional.of(StaticCredentialsProvider.create(
                AwsCredentialsIdentity.builder()
                                      .accessKeyId(properties.get(ProfileProperty.AWS_ACCESS_KEY_ID))
                                      .secretAccessKey(properties.get(ProfileProperty.AWS_SECRET_ACCESS_KEY))
                                      .accountId(properties.get(ProfileProperty.AWS_ACCOUNT_ID))
                                      .providerName("ProfileCredentialsProvider")
                                      .build()));
        }
        return Optional.empty();
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> webIdentity(Profile profile) {
        Map<String, String> properties = profile.properties();
        WebIdentityTokenFileCredentialsProvider.Builder builder =
            WebIdentityTokenFileCredentialsProvider.builder()
                                                   .roleArn(properties.get(ProfileProperty.ROLE_ARN))
                                                   .webIdentityTokenFile(
                                                       Paths.get(properties.get(ProfileProperty.WEB_IDENTITY_TOKEN_FILE)))
                                                   .roleSessionName(properties.get(ProfileProperty.ROLE_SESSION_NAME))
                                                   .region(properties.get(ProfileProperty.REGION))
                                                   .endpoint(context.stsEndpoint())
                                                   .settings(context.settings())
                                                   .httpClient(context.httpClient())
                                                   .clock(context.clock());
        if (context.executor() != null) {
            builder.executor(context.executor());
        }
        return builder.build();
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> sso(Profile profile) {
        require(profile, ProfileProperty.SSO_ACCOUNT_ID, ProfileProperty.SSO_ROLE_NAME);
        Map<String, String> properties = profile.properties();
        String sessionName = properties.get(ProfileProperty.SSO_SESSION);
        String region;
        String startUrl;
        if (sessionName != null) {
            Profile session = profileFile.section(ProfileFile.SSO_SESSION_SECTION, sessionName).orElseThrow(
                () -> new IllegalArgumentException("Sso-session section not found with sso-session title "
                                                   + sessionName + "."));
            region = matching(profile, session, ProfileProperty.SSO_REGION);
            startUrl = matching(profile, session, ProfileProperty.SSO_START_URL);
        } else {
            require(profile, ProfileProperty.SSO_REGION, ProfileProperty.SSO_START_URL);
            region = properties.get(ProfileProperty.SSO_REGION);
            startUrl = properties.get(ProfileProperty.SSO_START_URL);
        }
        SsoTokenProvider.Builder token = SsoTokenProvider.builder()
                                                         .sessionName(sessionName)
                                                         .startUrl(startUrl)
                                                         .region(region)
                                                         .cacheDirectory(context.ssoCacheDirectory())
                                                         .oidcEndpoint(context.ssoOidcEndpoint())
                                                         .settings(context.settings())
                                                         .httpClient(context.httpClient())
                                                         .clock(context.clock());
        SsoCredentialsProvider.Builder credentials =
            SsoCredentialsProvider.builder()
                                  .accountId(properties.get(ProfileProperty.SSO_ACCOUNT_ID))
                                  .roleName(properties.get(ProfileProperty.SSO_ROLE_NAME))
                                  .region(region)
                                  .endpoint(context.ssoPortalEndpoint())
                                  .httpClient(context.httpClient())
                                  .clock(context.clock());
        if (context.executor() != null) {
            token.executor(context.executor());
            credentials.executor(context.executor());
        }
        return credentials.tokenProvider(token.build()).build();
    }

    private static String matching(Profile profile, Profile session, String property) {
        String sessionValue = session.property(property).orElseThrow(
            () -> new IllegalArgumentException("Property '" + property + "' was not configured for sso-session '"
                                               + session.name() + "'."));
        Optional<String> profileValue = profile.property(property);
        if (profileValue.isPresent() && !profileValue.get().equals(sessionValue)) {
            throw new IllegalArgumentException("Profile " + profile.name() + " and Sso-session " + session.name()
                                               + " has different " + property + ".");
        }
        return sessionValue;
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> roleWithSourceProfile(Profile profile,
                                                                                    Set<String> children) {
        if (children.contains(profile.name())) {
            throw new IllegalStateException("Invalid profile file: Circular relationship detected with profiles "
                                            + children + ".");
        }
        children.add(profile.name());
        String sourceName = profile.properties().get(ProfileProperty.SOURCE_PROFILE);
        IdentityProvider<? extends AwsCredentialsIdentity> source =
            profileFile.profile(sourceName)
                       .flatMap(p -> provider(p, children))
                       .orElseThrow(() -> new IllegalStateException(
                           "The source profile of '" + profile.name() + "' was configured to be '" + sourceName
                           + "', but that source profile has no credentials configured."));
        return assumeRole(profile, source);
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> credentialSource(Profile profile) {
        String source = profile.properties().get(ProfileProperty.CREDENTIAL_SOURCE);
        return switch (source.strip().toLowerCase(Locale.ROOT)) {
            case "environment" -> AwsCredentialsProviderChain.of(
                SystemPropertyCredentialsProvider.create(context.settings().jvmProperties()),
                EnvironmentVariableCredentialsProvider.create(context.settings().environment()));
            case "ec2instancemetadata" -> {
                InstanceProfileCredentialsProvider.Builder builder =
                    InstanceProfileCredentialsProvider.builder()
                                                      .settings(context.settings())
                                                      .profileFile(() -> profileFile)
                                                      .profileName(profile.name())
                                                      .endpoint(context.imdsEndpoint())
                                                      .httpClient(context.httpClient())
                                                      .clock(context.clock());
                if (context.executor() != null) {
                    builder.executor(context.executor());
                }
                yield builder.build();
            }
            case "ecscontainer" -> {
                ContainerCredentialsProvider.Builder builder = ContainerCredentialsProvider.builder()
                                                                                           .settings(context.settings())
                                                                                           .httpClient(
                                                                                               context.httpClient())
                                                                                           .clock(context.clock());
                if (context.executor() != null) {
                    builder.executor(context.executor());
                }
                yield builder.build();
            }
            default -> throw new IllegalArgumentException(source + " is not a valid credential_source");
        };
    }

    private IdentityProvider<? extends AwsCredentialsIdentity> assumeRole(
        Profile profile, IdentityProvider<? extends AwsCredentialsIdentity> source) {
        Map<String, String> properties = profile.properties();
        String duration = properties.get(ProfileProperty.DURATION_SECONDS);
        StsAssumeRoleCredentialsProvider.Builder builder =
            StsAssumeRoleCredentialsProvider.builder()
                                            .sourceCredentialsProvider(source)
                                            .roleArn(properties.get(ProfileProperty.ROLE_ARN))
                                            .roleSessionName(properties.get(ProfileProperty.ROLE_SESSION_NAME))
                                            .externalId(properties.get(ProfileProperty.EXTERNAL_ID))
                                            .durationSeconds(duration == null ? null : Integer.valueOf(duration.strip()))
                                            .region(properties.get(ProfileProperty.REGION))
                                            .endpoint(context.stsEndpoint())
                                            .settings(context.settings())
                                            .httpClient(context.httpClient())
                                            .clock(context.clock());
        if (context.executor() != null) {
            builder.executor(context.executor());
        }
        return builder.build();
    }

    private static void require(Profile profile, String... properties) {
        for (String property : properties) {
            if (!profile.properties().containsKey(property)) {
                throw new IllegalArgumentException("Profile property '" + property + "' was not configured for '"
                                                   + profile.name() + "'.");
            }
        }
    }
}
