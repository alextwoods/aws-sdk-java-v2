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

package software.amazon.awssdk.regions.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.SystemSettings;

/**
 * The default region lookup, with the V2 name and precedence (Requirement 16.10):
 *
 * <ol>
 *   <li>the {@code aws.region} JVM system property, then the {@code AWS_REGION} environment variable;</li>
 *   <li>the {@code region} of the active profile ({@code aws.profile} / {@code AWS_PROFILE}, else {@code default});</li>
 *   <li>the region of the EC2 instance, from instance metadata.</li>
 * </ol>
 *
 * <p>Instance metadata needs an HTTP transport, which this module does not depend on: the instance-metadata step is the
 * {@link Builder#instanceMetadataRegionProvider(AwsRegionProvider) supplied} provider (the {@code identity-providers}
 * {@code InstanceProfileRegionProvider}, which {@code DefaultRegionProviders} there wires in). Without one, the chain
 * stops after the profile.
 */
public final class DefaultAwsRegionProviderChain extends AwsRegionProviderChain {
    private DefaultAwsRegionProviderChain(List<AwsRegionProvider> providers) {
        super(providers);
    }

    /** The chain over the process's settings and default profile file, without instance metadata. */
    public static DefaultAwsRegionProviderChain create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder of a {@link DefaultAwsRegionProviderChain}. */
    public static final class Builder {
        private SystemSettings settings = SystemSettings.system();
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private AwsRegionProvider instanceMetadataRegionProvider;

        private Builder() {
        }

        /** The JVM property and environment sources; the process's by default. */
        public Builder settings(SystemSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
            return this;
        }

        /** The profile file; the default profile file of {@link #settings} by default, read when needed. */
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /** The profile; the active profile of {@link #settings} by default. */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /** The last provider, consulted after the profile: the instance-metadata region provider. */
        public Builder instanceMetadataRegionProvider(AwsRegionProvider provider) {
            this.instanceMetadataRegionProvider = provider;
            return this;
        }

        public DefaultAwsRegionProviderChain build() {
            SystemSettings resolvedSettings = settings;
            Supplier<ProfileFile> file = profileFile != null ? profileFile
                                                             : () -> ProfileFile.defaultProfileFile(resolvedSettings);
            String name = profileName != null ? profileName : ProfileFileLocation.profileName(settings);
            List<AwsRegionProvider> providers = new ArrayList<>();
            providers.add(SystemSettingsRegionProvider.create(settings));
            providers.add(new AwsProfileRegionProvider(file, name));
            if (instanceMetadataRegionProvider != null) {
                providers.add(instanceMetadataRegionProvider);
            }
            return new DefaultAwsRegionProviderChain(providers);
        }
    }
}
