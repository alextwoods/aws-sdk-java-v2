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

package software.amazon.awssdk.checksums;

import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;

/**
 * The client options of request and response checksums and how each is resolved from its sources, with the V2
 * JVM property, environment variable and profile property names. Both options are client scoped.
 */
public final class ChecksumOptions {
    /** When the SDK calculates optional request checksums. */
    public static final ClientOption<RequestChecksumCalculation> REQUEST_CHECKSUM_CALCULATION =
        ClientOption.create("RequestChecksumCalculation", RequestChecksumCalculation.class);

    /** When the SDK validates response checksums the caller did not explicitly enable. */
    public static final ClientOption<ResponseChecksumValidation> RESPONSE_CHECKSUM_VALIDATION =
        ClientOption.create("ResponseChecksumValidation", ResponseChecksumValidation.class);

    /**
     * {@link #REQUEST_CHECKSUM_CALCULATION} from builder value, JVM property {@code aws.requestChecksumCalculation},
     * environment variable {@code AWS_REQUEST_CHECKSUM_CALCULATION}, profile property
     * {@code request_checksum_calculation}, then {@link RequestChecksumCalculation#WHEN_SUPPORTED}.
     */
    public static final ClientSetting<RequestChecksumCalculation> REQUEST_CHECKSUM_CALCULATION_SETTING =
        ClientSetting.builder(REQUEST_CHECKSUM_CALCULATION)
                     .jvmProperty("aws.requestChecksumCalculation")
                     .environmentVariable("AWS_REQUEST_CHECKSUM_CALCULATION")
                     .profileProperty("request_checksum_calculation")
                     .parser(RequestChecksumCalculation::fromValue)
                     .defaultValue(RequestChecksumCalculation.WHEN_SUPPORTED)
                     .build();

    /**
     * {@link #RESPONSE_CHECKSUM_VALIDATION} from builder value, JVM property {@code aws.responseChecksumValidation},
     * environment variable {@code AWS_RESPONSE_CHECKSUM_VALIDATION}, profile property
     * {@code response_checksum_validation}, then {@link ResponseChecksumValidation#WHEN_SUPPORTED}.
     */
    public static final ClientSetting<ResponseChecksumValidation> RESPONSE_CHECKSUM_VALIDATION_SETTING =
        ClientSetting.builder(RESPONSE_CHECKSUM_VALIDATION)
                     .jvmProperty("aws.responseChecksumValidation")
                     .environmentVariable("AWS_RESPONSE_CHECKSUM_VALIDATION")
                     .profileProperty("response_checksum_validation")
                     .parser(ResponseChecksumValidation::fromValue)
                     .defaultValue(ResponseChecksumValidation.WHEN_SUPPORTED)
                     .build();

    private ChecksumOptions() {
    }

    /** The resolved request checksum calculation, {@link RequestChecksumCalculation#WHEN_SUPPORTED} if unset. */
    public static RequestChecksumCalculation requestChecksumCalculation(SdkClientConfiguration configuration) {
        return configuration.get(REQUEST_CHECKSUM_CALCULATION).orElse(RequestChecksumCalculation.WHEN_SUPPORTED);
    }

    /** The resolved response checksum validation, {@link ResponseChecksumValidation#WHEN_SUPPORTED} if unset. */
    public static ResponseChecksumValidation responseChecksumValidation(SdkClientConfiguration configuration) {
        return configuration.get(RESPONSE_CHECKSUM_VALIDATION).orElse(ResponseChecksumValidation.WHEN_SUPPORTED);
    }
}
