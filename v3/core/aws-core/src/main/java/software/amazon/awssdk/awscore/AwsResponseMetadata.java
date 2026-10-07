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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.utils.ToString;

/**
 * The V2-compatible view of a response's metadata, source compatible with the V2 {@code AwsResponseMetadata}: a map
 * of raw metadata keyed by {@value #AWS_REQUEST_ID} and by the response's header names, read through
 * {@link #requestId()} and, by generated service subclasses ({@code <Service>ResponseMetadata}), through
 * {@link #getValue(String)}.
 *
 * <p>The documented absent value of this view is V2's {@value #UNKNOWN} (Requirement 21.13): {@link #requestId()}
 * and {@link #getValue(String)} return it, never {@code null}, for a value the response did not supply. The V3
 * metadata API, {@link software.amazon.awssdk.core.SdkResponse#sdkResponseMetadata()}, reports absence as an empty
 * {@code Optional} instead.
 */
public abstract class AwsResponseMetadata {
    /** The metadata key of the AWS request ID, as in V2. */
    static final String AWS_REQUEST_ID = "AWS_REQUEST_ID";

    /** The value of a metadata key the response did not supply, as in V2. */
    private static final String UNKNOWN = "UNKNOWN";

    private final Map<String, String> metadata;

    /**
     * Creates the view of {@code metadata}, a map of raw metadata.
     *
     * @param metadata the raw metadata; read through, never copied
     */
    protected AwsResponseMetadata(Map<String, String> metadata) {
        this.metadata = Collections.unmodifiableMap(Objects.requireNonNull(metadata, "metadata"));
    }

    /** Creates a view of the same raw metadata as {@code responseMetadata}; no metadata if it is {@code null}. */
    protected AwsResponseMetadata(AwsResponseMetadata responseMetadata) {
        this(responseMetadata == null ? Map.of() : responseMetadata.metadata);
    }

    /**
     * The AWS request ID of the response, or {@value #UNKNOWN} if it supplied none. Generated service subclasses may
     * read it from a service-specific key.
     */
    public String requestId() {
        return getValue(AWS_REQUEST_ID);
    }

    /** Names the metadata keys only: values are not rendered. */
    @Override
    public final String toString() {
        return ToString.builder("AwsResponseMetadata").add("metadata", metadata.keySet()).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return metadata.equals(((AwsResponseMetadata) o).metadata);
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    /**
     * The value of {@code key}: {@value #AWS_REQUEST_ID} or a response header name (case-insensitive for metadata
     * the SDK produced).
     *
     * @return the value, or {@value #UNKNOWN} if the response did not supply it
     */
    protected final String getValue(String key) {
        String value = metadata.get(key);
        return value == null ? UNKNOWN : value;
    }
}
