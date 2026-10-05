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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.net.URI;
import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.partition.PartitionMetadata;
import software.amazon.awssdk.endpoints.partition.Partitions;
import software.amazon.awssdk.regions.Region;

/**
 * Determines both the regional endpoint that SNS certificates are expected to be hosted from, and the expected common
 * name (CN) that the certificate from that endpoint must have. Thread safe.
 *
 * <p>The endpoint is the one the canonical SNS endpoint rule set resolves for the region with FIPS and dual-stack off
 * and no custom endpoint, {@code https://sns.{Region}.{PartitionResult#dnsSuffix}}, evaluated over the canonical
 * partition table ({@link Partitions#defaults()}, the table of the {@code aws.partition} rules function). V2 evaluated
 * the generated SNS endpoint provider; this library uses the same rule output without depending on the SNS client.
 */
public final class SnsHostProvider {
    private final Region region;
    private final Partitions partitions;

    public SnsHostProvider(Region region) {
        this(region, Partitions.defaults());
    }

    SnsHostProvider(Region region, Partitions partitions) {
        this.region = Objects.requireNonNull(region, "region must not be null");
        this.partitions = Objects.requireNonNull(partitions, "partitions must not be null");
    }

    public URI regionalEndpoint() {
        try {
            return URI.create("https://sns." + region.id() + "." + partition().dnsSuffix());
        } catch (RuntimeException e) {
            throw SdkClientException.create("Unable to resolve SNS endpoint for region " + region, e);
        }
    }

    public String signingCertCommonName() {
        // If we don't know about this region, try to guess common name
        if (!Region.regions().contains(region)) {
            // Find the partition where it belongs by checking the region against the published pattern for known
            // partitions, e.g. 'us-gov-west-3' would match the 'aws-us-gov' partition. This falls back to the 'aws'
            // partition if it fails to match any partition.
            return "sns." + partition().dnsSuffix();
        }

        String regionId = region.id();

        switch (regionId) {
            case "cn-north-1":
                return "sns-cn-north-1.amazonaws.com.cn";
            case "cn-northwest-1":
                return "sns-cn-northwest-1.amazonaws.com.cn";
            case "us-gov-west-1":
            case "us-gov-east-1":
                return "sns-us-gov-west-1.amazonaws.com";
            case "us-iso-east-1":
                return "sns-us-iso-east-1.c2s.ic.gov";
            case "us-isob-east-1":
                return "sns-us-isob-east-1.sc2s.sgov.gov";
            case "us-isof-east-1":
                return "sns-signing.us-isof-east-1.csp.hci.ic.gov";
            case "us-isof-south-1":
                return "sns-signing.us-isof-south-1.csp.hci.ic.gov";
            case "eu-isoe-west-1":
                return "sns-signing.eu-isoe-west-1.cloud.adc-e.uk";
            case "eusc-de-east-1":
                return "sns-signing.eusc-de-east-1.amazonaws.eu";
            case "ap-east-1":
            case "ap-east-2":
            case "ap-south-2":
            case "ap-southeast-5":
            case "ap-southeast-6":
            case "ap-southeast-7":
            case "me-south-1":
            case "me-central-1":
            case "eu-south-1":
            case "eu-south-2":
            case "eu-central-2":
            case "af-south-1":
            case "ap-southeast-3":
            case "ap-southeast-4":
            case "il-central-1":
            case "ca-west-1":
            case "mx-central-1":
                return "sns-signing." + regionId + ".amazonaws.com";
            default:
                return "sns.amazonaws.com";
        }
    }

    private PartitionMetadata partition() {
        return partitions.of(region);
    }
}
