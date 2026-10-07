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

import java.util.Map;

/**
 * The default {@link AwsResponseMetadata}, source compatible with the V2 {@code DefaultAwsResponseMetadata}: the
 * view of a raw metadata map with no service-specific accessor.
 */
public final class DefaultAwsResponseMetadata extends AwsResponseMetadata {
    private DefaultAwsResponseMetadata(Map<String, String> metadata) {
        super(metadata);
    }

    /** The view of {@code metadata}, keyed by {@code "AWS_REQUEST_ID"} and by header names. */
    public static DefaultAwsResponseMetadata create(Map<String, String> metadata) {
        return new DefaultAwsResponseMetadata(metadata);
    }
}
