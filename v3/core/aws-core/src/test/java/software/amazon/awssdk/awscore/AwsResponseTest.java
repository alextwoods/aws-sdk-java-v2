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

package software.amazon.awssdk.awscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.ResponseMetadataMap;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpResponse;

/** Task 16.1 (Requirements 21.1, 21.13): the response and error metadata views of aws-core. */
class AwsResponseTest {
    private static final SdkHttpResponse HTTP = SdkHttpResponse.builder()
                                                                .statusCode(200)
                                                                .addHeader("x-amzn-RequestId", "req")
                                                                .addHeader("X-Amz-Cf-Id", "cf")
                                                                .body(HttpBody.fromUtf8("payload"))
                                                                .build();

    @Test
    void aReceivedResponseExposesItsMetadataWithoutRetainingTheBody() {
        SdkResponseMetadata metadata = SdkResponseMetadata.builder().requestId("req").statusCode(200).attemptCount(2)
                                                          .build();
        TestResponse response = TestResponse.builder().sdkHttpResponse(HTTP).sdkResponseMetadata(metadata).build();

        assertSame(metadata, response.sdkResponseMetadata());
        assertEquals(200, response.sdkHttpResponse().statusCode());
        assertEquals(HttpBody.Kind.EMPTY, response.sdkHttpResponse().body().kind());
        assertEquals(HTTP.headers(), response.sdkHttpResponse().headers());
        // The V2-compatible view: the request ID under AWS_REQUEST_ID, headers by name (case-insensitively).
        TestMetadata view = TestMetadata.create(response.responseMetadata());
        assertEquals("req", view.requestId());
        assertEquals("cf", view.value("x-amz-cf-id"));
        assertEquals("UNKNOWN", view.value("x-amz-id-2"));
        // Copies keep the metadata, which takes no part in the response's identity.
        TestResponse copy = response.toBuilder().build();
        assertSame(metadata, copy.sdkResponseMetadata());
        assertSame(response.sdkHttpResponse(), copy.sdkHttpResponse());
    }

    @Test
    void aBuiltResponseHasTheDocumentedAbsentValues() {
        TestResponse response = TestResponse.builder().build();

        assertSame(SdkResponseMetadata.empty(), response.sdkResponseMetadata());
        assertNull(response.sdkHttpResponse());
        assertEquals("UNKNOWN", response.responseMetadata().requestId());
    }

    @Test
    void anExplicitMetadataViewTakesPrecedence() {
        AwsResponseMetadata explicit = DefaultAwsResponseMetadata.create(Map.of("AWS_REQUEST_ID", "explicit"));
        TestResponse response = TestResponse.builder().sdkHttpResponse(HTTP).responseMetadata(explicit).build();

        assertSame(explicit, response.responseMetadata());
        assertSame(explicit, response.toBuilder().build().responseMetadata());
    }

    @Test
    void theRawMetadataMapEqualsItsMaterializedContent() {
        HttpHeaders headers = HTTP.headers();
        ResponseMetadataMap map = new ResponseMetadataMap("req", headers);
        Map<String, String> expected = Map.of("AWS_REQUEST_ID", "req", "x-amzn-RequestId", "req", "X-Amz-Cf-Id", "cf");
        assertEquals(expected, map);
        assertEquals(expected.hashCode(), map.hashCode());
        assertEquals(DefaultAwsResponseMetadata.create(expected), DefaultAwsResponseMetadata.create(map));
        assertNotEquals(DefaultAwsResponseMetadata.create(expected), TestMetadata.create(DefaultAwsResponseMetadata
                                                                                           .create(expected)));
        assertEquals("AwsResponseMetadata(metadata=[AWS_REQUEST_ID, x-amzn-RequestId, X-Amz-Cf-Id])",
                     DefaultAwsResponseMetadata.create(map).toString());
    }

    @Test
    void aServiceErrorExposesItsCodeAndHttpResponse() {
        AwsErrorDetails details = AwsErrorDetails.builder().errorCode("Throttling").sdkHttpResponse(HTTP).build();
        AwsServiceException error = AwsServiceException.builder().message("m").statusCode(400).requestId("req")
                                                       .awsErrorDetails(details).build();

        SdkResponseMetadata metadata = error.sdkResponseMetadata();
        assertEquals(Optional.of("Throttling"), metadata.errorCode());
        assertEquals(Optional.of("req"), metadata.requestId());
        assertEquals(OptionalInt.of(400), metadata.statusCode());
        assertEquals(OptionalInt.empty(), metadata.attemptCount());
        assertEquals(HttpBody.Kind.EMPTY, details.sdkHttpResponse().body().kind());
        assertEquals("cf", details.toBuilder().build().sdkHttpResponse().headers().firstValue("X-Amz-Cf-Id")
                                  .orElseThrow());
        assertEquals(AwsErrorDetails.builder().errorCode("Throttling").build(), details,
                     "the HTTP response is transport metadata, not error content");
    }

    /** A generated-style response with no modeled members. */
    static final class TestResponse extends AwsResponse {
        private TestResponse(BuilderImpl builder) {
            super(builder);
        }

        static Builder builder() {
            return new Builder();
        }

        @Override
        public Builder toBuilder() {
            return new Builder(this);
        }

        static final class Builder extends AwsResponse.BuilderImpl {
            private Builder() {
            }

            private Builder(TestResponse response) {
                super(response);
            }

            @Override
            public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
                super.sdkHttpResponse(sdkHttpResponse);
                return this;
            }

            @Override
            public Builder sdkResponseMetadata(SdkResponseMetadata sdkResponseMetadata) {
                super.sdkResponseMetadata(sdkResponseMetadata);
                return this;
            }

            @Override
            public Builder responseMetadata(AwsResponseMetadata responseMetadata) {
                super.responseMetadata(responseMetadata);
                return this;
            }

            @Override
            public TestResponse build() {
                return new TestResponse(this);
            }
        }
    }

    /** A generated-style service metadata view. */
    static final class TestMetadata extends AwsResponseMetadata {
        private TestMetadata(AwsResponseMetadata metadata) {
            super(metadata);
        }

        static TestMetadata create(AwsResponseMetadata metadata) {
            return new TestMetadata(metadata);
        }

        String value(String key) {
            return getValue(key);
        }
    }
}
