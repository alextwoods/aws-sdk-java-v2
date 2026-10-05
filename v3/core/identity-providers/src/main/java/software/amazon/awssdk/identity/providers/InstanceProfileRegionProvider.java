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

import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.internal.Ec2MetadataClient;
import software.amazon.awssdk.identity.providers.internal.Json;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/**
 * The region of the EC2 instance, from the instance identity document in instance metadata, with the V2 name. The
 * region is loaded once and cached. {@link #defaultChain()} is the full V2 default region chain.
 */
public final class InstanceProfileRegionProvider implements AwsRegionProvider, AutoCloseable {
    private static final String IDENTITY_DOCUMENT = "/latest/dynamic/instance-identity/document";

    private final Ec2MetadataClient imds;
    private volatile Region region;

    private InstanceProfileRegionProvider(SystemSettings settings, String endpoint, SdkSyncHttpClient httpClient) {
        String profileName = ProfileFileLocation.profileName(settings);
        this.imds = new Ec2MetadataClient(settings,
                                          () -> ProfileFile.defaultProfileFile(settings).profile(profileName),
                                          endpoint, httpClient);
    }

    /** The provider of the process's IMDS settings and the default HTTP implementation. */
    public static InstanceProfileRegionProvider create() {
        return new InstanceProfileRegionProvider(SystemSettings.system(), null, null);
    }

    /** The provider of {@code settings}, IMDS {@code endpoint} (null for the configured one) and client. */
    public static InstanceProfileRegionProvider create(SystemSettings settings, String endpoint,
                                                       SdkSyncHttpClient httpClient) {
        return new InstanceProfileRegionProvider(settings, endpoint, httpClient);
    }

    /**
     * The V2 default region chain: {@code aws.region}, {@code AWS_REGION}, the active profile's {@code region}, then
     * instance metadata.
     */
    public static DefaultAwsRegionProviderChain defaultChain() {
        return DefaultAwsRegionProviderChain.builder().instanceMetadataRegionProvider(create()).build();
    }

    @Override
    public Region getRegion() {
        Region cached = region;
        if (cached != null) {
            return cached;
        }
        if (imds.disabled()) {
            throw SdkClientException.create("Unable to contact EC2 metadata service: it is disabled.");
        }
        try {
            Optional<String> token = imds.token();
            String value = Json.string(Json.parseObject(imds.get(IDENTITY_DOCUMENT, token)), "region");
            if (value == null || value.isBlank()) {
                throw SdkClientException.create("The instance identity document has no region.");
            }
            region = Region.of(value);
            return region;
        } catch (RuntimeException e) {
            throw SdkClientException.create("Unable to contact EC2 metadata service.", e);
        }
    }

    @Override
    public void close() {
        imds.close();
    }
}
