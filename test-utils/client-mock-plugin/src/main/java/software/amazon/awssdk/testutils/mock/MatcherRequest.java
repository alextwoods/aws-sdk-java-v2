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
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The argument of a matcher added with {@link MockPlugin.Builder#addMatcher}.
 *
 * <p>Port of smithy-java's {@code MatcherRequest} record; the call context is the call's {@link ExecutionAttributes}.
 */
@SdkPublicApi
public final class MatcherRequest {
    private final ExecutionAttributes context;
    private final ApiOperation<?, ?> operation;
    private final SerializableStruct input;
    private final SdkHttpFullRequest request;

    MatcherRequest(ExecutionAttributes context,
                   ApiOperation<?, ?> operation,
                   SerializableStruct input,
                   SdkHttpFullRequest request) {
        this.context = Validate.paramNotNull(context, "context");
        this.operation = Validate.paramNotNull(operation, "operation");
        this.input = input;
        this.request = Validate.paramNotNull(request, "request");
    }

    /**
     * @return the attributes of the call
     */
    public ExecutionAttributes context() {
        return context;
    }

    /**
     * @return the operation being called
     */
    public ApiOperation<?, ?> operation() {
        return operation;
    }

    /**
     * @return the input of the call, or null if the client's request is not a {@link SerializableStruct}
     */
    public SerializableStruct input() {
        return input;
    }

    /**
     * @return the HTTP request being mocked
     */
    public SdkHttpFullRequest request() {
        return request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MatcherRequest)) {
            return false;
        }
        MatcherRequest that = (MatcherRequest) o;
        return context.equals(that.context) && operation.equals(that.operation) && Objects.equals(input, that.input)
               && request.equals(that.request);
    }

    @Override
    public int hashCode() {
        int result = context.hashCode();
        result = 31 * result + operation.hashCode();
        result = 31 * result + Objects.hashCode(input);
        return 31 * result + request.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("MatcherRequest")
                       .add("operation", operation.schema().id())
                       .add("method", request.method())
                       .add("uri", request.getUri())
                       .build();
    }
}
