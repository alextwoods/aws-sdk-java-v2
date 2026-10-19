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

package software.amazon.awssdk.testutils.mock;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A request received by the {@link MockPlugin}: the call's input and the serialized, signed HTTP request of one
 * attempt.
 *
 * <p>Port of smithy-java's {@code MockedRequest} record.
 */
@SdkPublicApi
public final class MockedRequest {
    private final SerializableStruct input;
    private final SdkHttpFullRequest request;

    MockedRequest(SerializableStruct input, SdkHttpFullRequest request) {
        this.input = input;
        this.request = Validate.paramNotNull(request, "request");
    }

    /**
     * Returns the input of the call, after the {@code modifyRequest} interceptors ran.
     *
     * @return the input, for example a generated request class, or null if the client's request is not a
     *     {@link SerializableStruct}
     */
    public SerializableStruct input() {
        return input;
    }

    /**
     * Returns the HTTP request the client's protocol serialized and the pipeline sent to the transport.
     *
     * @return the request
     */
    public SdkHttpFullRequest request() {
        return request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MockedRequest)) {
            return false;
        }
        MockedRequest that = (MockedRequest) o;
        return Objects.equals(input, that.input) && request.equals(that.request);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(input) + request.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("MockedRequest")
                       .add("input", input == null ? null : input.sdkSchema().id())
                       .add("method", request.method())
                       .add("uri", request.getUri())
                       .build();
    }
}
