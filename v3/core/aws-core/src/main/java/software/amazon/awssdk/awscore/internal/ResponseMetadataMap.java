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

package software.amazon.awssdk.awscore.internal;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.http.HttpHeaders;

/**
 * The raw metadata map of the V2-compatible {@code AwsResponseMetadata} view of a response, with V2's content: the
 * request ID under {@code AWS_REQUEST_ID}, and the first value of every response header under its name. Lookups read
 * through to the immutable headers (header names case-insensitively), so creating the view copies nothing; the entry
 * set is materialized only if iterated. Immutable.
 */
public final class ResponseMetadataMap extends AbstractMap<String, String> {
    private static final String AWS_REQUEST_ID = "AWS_REQUEST_ID";

    private final String requestId;
    private final HttpHeaders headers;
    private Set<Map.Entry<String, String>> entries;

    /**
     * @param requestId the request ID, or {@code null} if the response supplied none
     * @param headers the response's headers
     */
    public ResponseMetadataMap(String requestId, HttpHeaders headers) {
        this.requestId = requestId;
        this.headers = Objects.requireNonNull(headers, "headers");
    }

    @Override
    public String get(Object key) {
        if (AWS_REQUEST_ID.equals(key)) {
            return requestId;
        }
        return key instanceof String name ? headers.firstValue(name).orElse(null) : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        Set<Map.Entry<String, String>> result = entries;
        if (result == null) {
            Map<String, String> all = new LinkedHashMap<>();
            if (requestId != null) {
                all.put(AWS_REQUEST_ID, requestId);
            }
            for (int i = 0; i < headers.size(); i++) {
                all.putIfAbsent(headers.name(i), headers.value(i));
            }
            result = Collections.unmodifiableMap(all).entrySet();
            entries = result;
        }
        return result;
    }
}
