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

package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.util.function.Consumer;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonWriter;

/**
 * A DynamoDB service error raised by the in-process service: an awsJson1.0 error with an HTTP status, the shape name
 * as {@code __type}, a {@code message} and optional extra members (for example {@code CancellationReasons}).
 */
public final class DynamoDbError extends RuntimeException {
    private static final String NAMESPACE = "com.amazonaws.dynamodb.v20120810#";

    private final int status;
    private final String type;
    private final transient Consumer<JsonWriter> extraMembers;

    public DynamoDbError(int status, String type, String message, Consumer<JsonWriter> extraMembers) {
        super(type + ": " + message);
        this.status = status;
        this.type = type;
        this.extraMembers = extraMembers;
    }

    public static DynamoDbError validation(String message) {
        return new DynamoDbError(400, "ValidationException", message, null);
    }

    public static DynamoDbError resourceNotFound(String message) {
        return new DynamoDbError(400, "ResourceNotFoundException", message, null);
    }

    public int status() {
        return status;
    }

    public String type() {
        return type;
    }

    /** The awsJson1.0 error body. */
    public String body() {
        JsonWriter w = new JsonWriter().writeStartObject();
        w.writeFieldName("__type").writeValue(NAMESPACE + type);
        w.writeFieldName("message").writeValue(getMessage().substring(type.length() + 2));
        if (extraMembers != null) {
            extraMembers.accept(w);
        }
        return w.writeEndObject().toString();
    }
}
