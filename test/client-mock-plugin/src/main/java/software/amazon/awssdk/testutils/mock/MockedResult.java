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
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A mocked result to return from a {@link MockPlugin}: an {@link Output} serialized into a response, a raw
 * {@link Response}, or an {@link Error} thrown by the transport.
 *
 * <p>Port of smithy-java {@code MockedResult}. The sealed interface of records is a class with a private constructor
 * and three final nested classes, created with {@link #output(SerializableStruct)}, {@link #response(SdkHttpFullResponse)}
 * and {@link #error(Throwable)}.
 */
@SdkPublicApi
public abstract class MockedResult {
    private MockedResult() {
    }

    /**
     * Returns a result that serializes an output value, a schema-backed Document or a modeled exception with the
     * {@link ResponseGenerator} of the client's protocol.
     *
     * @param output the value to serialize
     * @return the result
     * @throws NullPointerException if {@code output} is null
     */
    public static Output output(SerializableStruct output) {
        return new Output(output, null);
    }

    /**
     * Returns a result that serializes a value with a specific generator.
     *
     * @param output the value to serialize
     * @param generator the generator to use, or null for the generator of the client's protocol
     * @return the result
     * @throws NullPointerException if {@code output} is null
     */
    public static Output output(SerializableStruct output, ResponseGenerator generator) {
        return new Output(output, generator);
    }

    /**
     * Returns a result that hands a raw response to the client's protocol unchanged.
     *
     * @param response the response
     * @return the result
     * @throws NullPointerException if {@code response} is null
     */
    public static Response response(SdkHttpFullResponse response) {
        return new Response(response);
    }

    /**
     * Returns a result that makes the transport throw an exception instead of returning a response.
     *
     * @param error the exception
     * @return the result
     * @throws NullPointerException if {@code error} is null
     */
    public static Error error(Throwable error) {
        return new Error(error);
    }

    /**
     * A value serialized with a {@link ResponseGenerator}: an output (successful response) or a modeled exception, whose
     * schema has the {@code error} trait (error response).
     */
    public static final class Output extends MockedResult {
        private final SerializableStruct output;
        private final ResponseGenerator generator;

        private Output(SerializableStruct output, ResponseGenerator generator) {
            this.output = Validate.paramNotNull(output, "output");
            this.generator = generator;
        }

        /**
         * @return the value to serialize
         */
        public SerializableStruct output() {
            return output;
        }

        /**
         * @return the generator to use, or null for the generator of the client's protocol
         */
        public ResponseGenerator generator() {
            return generator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Output)) {
                return false;
            }
            Output that = (Output) o;
            return output.equals(that.output) && Objects.equals(generator, that.generator);
        }

        @Override
        public int hashCode() {
            return 31 * output.hashCode() + Objects.hashCode(generator);
        }

        @Override
        public String toString() {
            return ToString.builder("MockedResult.Output")
                           .add("output", output.sdkSchema().id())
                           .add("generator", generator)
                           .build();
        }
    }

    /**
     * A response returned unchanged.
     */
    public static final class Response extends MockedResult {
        private final SdkHttpFullResponse response;

        private Response(SdkHttpFullResponse response) {
            this.response = Validate.paramNotNull(response, "response");
        }

        /**
         * @return the response
         */
        public SdkHttpFullResponse response() {
            return response;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Response && response.equals(((Response) o).response);
        }

        @Override
        public int hashCode() {
            return response.hashCode();
        }

        @Override
        public String toString() {
            return ToString.builder("MockedResult.Response").add("statusCode", response.statusCode()).build();
        }
    }

    /**
     * An exception thrown by the transport instead of a response, without serializing it.
     *
     * <p>To serialize a modeled exception and have the client's protocol deserialize and throw it, use an
     * {@link Output}.
     */
    public static final class Error extends MockedResult {
        private final Throwable error;

        private Error(Throwable error) {
            this.error = Validate.paramNotNull(error, "error");
        }

        /**
         * @return the exception to throw
         */
        public Throwable error() {
            return error;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Error && error.equals(((Error) o).error);
        }

        @Override
        public int hashCode() {
            return error.hashCode();
        }

        @Override
        public String toString() {
            return ToString.builder("MockedResult.Error").add("error", error).build();
        }
    }
}
