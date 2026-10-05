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

package software.amazon.awssdk.protocols.core;

import java.util.Objects;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The {@code aws.protocols#awsQueryError} trait of one modeled error: the error code the awsQuery/ec2Query wire (and
 * {@code awsQueryCompatible} JSON services) use for it, and its HTTP status code.
 *
 * @param error absolute shape ID of the error structure
 * @param code the wire error code
 * @param httpResponseCode the HTTP status code
 */
public record AwsQueryErrorCode(ShapeId error, String code, int httpResponseCode) {

    public AwsQueryErrorCode {
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(code, "code");
        if (error.hasMember()) {
            throw new IllegalArgumentException("An error is a structure shape ID, not the member ID " + error);
        }
        if (code.isEmpty()) {
            throw new IllegalArgumentException("The awsQueryError code of " + error + " is empty");
        }
        if (httpResponseCode < 100 || httpResponseCode > 599) {
            throw new IllegalArgumentException("The awsQueryError httpResponseCode of " + error + " is not an HTTP "
                                               + "status code: " + httpResponseCode);
        }
    }
}
