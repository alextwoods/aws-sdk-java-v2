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

package software.amazon.awssdk.httpbinding.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Reads an already percent-decoded {@code @httpLabel} value; timestamps default to {@code date-time}. Port of
 * smithy-java {@code HttpPathLabelDeserializer}.
 */
@SdkInternalApi
final class HttpPathLabelDeserializer extends BasicStringValueDeserializer {
    HttpPathLabelDeserializer(String value) {
        super(value, "HTTP path bindings", null);
    }

    @Override
    TimestampFormatTrait.Format defaultTimestampFormat() {
        return TimestampFormatTrait.Format.DATE_TIME;
    }
}
