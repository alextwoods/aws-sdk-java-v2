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

package software.amazon.awssdk.pipeline;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * A signed request the pipeline did not transmit: the outcome of {@link AwsCallEngine#presign}. Whoever holds it
 * can send it until {@link #expiration()}, so it carries no identity, only the signature.
 *
 * @param httpRequest the signed request, signature included
 * @param expiration when the signature stops being valid
 */
public record PresignedHttpRequest(SdkHttpRequest httpRequest, Instant expiration) {
    public PresignedHttpRequest {
        Objects.requireNonNull(httpRequest, "httpRequest");
        Objects.requireNonNull(expiration, "expiration");
    }

    /** The presigned URL: the request target with its signed query string. */
    public URI url() {
        String query = httpRequest.encodedQuery();
        return URI.create(httpRequest.scheme() + "://" + httpRequest.authority() + httpRequest.encodedPath()
                          + (query == null || query.isEmpty() ? "" : "?" + query));
    }

    /** The HTTP method the request must be sent with. */
    public String method() {
        return httpRequest.method();
    }

    /** The headers (lower-case names, in order) the sender must send exactly; they may be signed. */
    public Map<String, List<String>> signedHeaders() {
        HttpHeaders headers = httpRequest.headers();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            out.computeIfAbsent(headers.name(i).toLowerCase(Locale.ROOT), n -> new ArrayList<>()).add(headers.value(i));
        }
        out.replaceAll((name, values) -> List.copyOf(values));
        return Map.copyOf(out);
    }

    /**
     * Whether a browser can send the request by following {@link #url()} alone: a {@code GET} without a payload whose
     * only header is {@code host}, which a browser sets itself (the V2 definition).
     */
    public boolean isBrowserExecutable() {
        HttpHeaders headers = httpRequest.headers();
        for (int i = 0; i < headers.size(); i++) {
            if (!"host".equalsIgnoreCase(headers.name(i))) {
                return false;
            }
        }
        return "GET".equals(httpRequest.method()) && httpRequest.body().kind() == HttpBody.Kind.EMPTY;
    }

    @Override
    public String toString() {
        // The URL holds the signature, a bearer credential until it expires: never rendered.
        return "PresignedHttpRequest(" + httpRequest.method() + " " + httpRequest.host() + httpRequest.encodedPath()
               + ", expiration=" + expiration + ")";
    }
}
