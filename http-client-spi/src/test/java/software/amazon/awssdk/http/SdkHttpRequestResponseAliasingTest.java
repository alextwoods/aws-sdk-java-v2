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

package software.amazon.awssdk.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * {@link SdkHttpFullRequest} and {@link SdkHttpFullResponse} share their header and query-parameter storage between
 * builders and built objects until something is mutated. These tests pin the observable contract of that sharing: a
 * mutation through a builder must never be visible on an object that was already built, whichever mutator is used.
 *
 * <p>This complements {@code LowCopyListMapTest}, which covers the storage layer directly. These go through the public
 * builder API so they also catch a mutator wired to the wrong copy-on-write path.
 */
class SdkHttpRequestResponseAliasingTest {

    private static SdkHttpFullRequest.Builder requestBuilder() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("https://aws.amazon.com"))
                                 .method(SdkHttpMethod.GET);
    }

    @Test
    void appendHeader_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putHeader("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder().appendHeader("foo", "2").build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(modified.matchingHeaders("foo")).containsExactly("1", "2");
    }

    @Test
    void putHeader_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putHeader("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder().putHeader("foo", "2").build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(modified.matchingHeaders("foo")).containsExactly("2");
    }

    @Test
    void removeHeader_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putHeader("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder().removeHeader("foo").build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(modified.matchingHeaders("foo")).isEmpty();
    }

    /**
     * A put makes the builder's map private but may leave the value lists shared with the built request. A subsequent
     * append has to privatize the lists too.
     */
    @Test
    void putThenAppendHeader_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putHeader("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder()
                                              .putHeader("bar", "9")
                                              .appendHeader("foo", "2")
                                              .build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(original.matchingHeaders("bar")).isEmpty();
        assertThat(modified.matchingHeaders("foo")).containsExactly("1", "2");
        assertThat(modified.matchingHeaders("bar")).containsExactly("9");
    }

    /**
     * This is the shape the signing stage and the retry loop produce: one built request, several derived builders.
     */
    @Test
    void twoBuildersFromSameRequest_areIndependent() {
        SdkHttpFullRequest original = requestBuilder().putHeader("foo", "1").build();

        SdkHttpFullRequest left = original.toBuilder().appendHeader("foo", "L").build();
        SdkHttpFullRequest right = original.toBuilder().appendHeader("foo", "R").build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(left.matchingHeaders("foo")).containsExactly("1", "L");
        assertThat(right.matchingHeaders("foo")).containsExactly("1", "R");
    }

    @Test
    void appendRawQueryParameter_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putRawQueryParameter("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder().appendRawQueryParameter("foo", "2").build();

        assertThat(original.rawQueryParameters().get("foo")).containsExactly("1");
        assertThat(modified.rawQueryParameters().get("foo")).containsExactly("1", "2");
    }

    @Test
    void putThenAppendRawQueryParameter_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putRawQueryParameter("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder()
                                              .putRawQueryParameter("bar", "9")
                                              .appendRawQueryParameter("foo", "2")
                                              .build();

        assertThat(original.rawQueryParameters().get("foo")).containsExactly("1");
        assertThat(original.rawQueryParameters()).doesNotContainKey("bar");
        assertThat(modified.rawQueryParameters().get("foo")).containsExactly("1", "2");
    }

    @Test
    void clearQueryParameters_onBuilderFromBuiltRequest_doesNotMutateBuiltRequest() {
        SdkHttpFullRequest original = requestBuilder().putRawQueryParameter("foo", "1").build();

        SdkHttpFullRequest modified = original.toBuilder().clearQueryParameters().build();

        assertThat(original.rawQueryParameters().get("foo")).containsExactly("1");
        assertThat(modified.rawQueryParameters()).isEmpty();
    }

    @Test
    void appendHeader_onBuilderFromBuiltResponse_doesNotMutateBuiltResponse() {
        SdkHttpFullResponse original = SdkHttpFullResponse.builder()
                                                         .statusCode(200)
                                                         .putHeader("foo", "1")
                                                         .build();

        SdkHttpFullResponse modified = original.toBuilder().appendHeader("foo", "2").build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(modified.matchingHeaders("foo")).containsExactly("1", "2");
    }

    @Test
    void putThenAppendHeader_onBuilderFromBuiltResponse_doesNotMutateBuiltResponse() {
        SdkHttpFullResponse original = SdkHttpFullResponse.builder()
                                                         .statusCode(200)
                                                         .putHeader("foo", "1")
                                                         .build();

        SdkHttpFullResponse modified = original.toBuilder()
                                               .putHeader("bar", "9")
                                               .appendHeader("foo", "2")
                                               .build();

        assertThat(original.matchingHeaders("foo")).containsExactly("1");
        assertThat(modified.matchingHeaders("foo")).containsExactly("1", "2");
    }

    /**
     * The externally-supplied map is owned by the caller, so it must be copied on the way in.
     */
    @Test
    void headersFromExternalMap_areNotAliasedToCaller() {
        java.util.Map<String, java.util.List<String>> external = new java.util.HashMap<>();
        java.util.List<String> callerValues = new java.util.ArrayList<>();
        callerValues.add("1");
        external.put("foo", callerValues);

        SdkHttpFullRequest request = requestBuilder().headers(external).build();
        callerValues.add("caller-mutation");

        assertThat(request.matchingHeaders("foo")).containsExactly("1");
    }
}
