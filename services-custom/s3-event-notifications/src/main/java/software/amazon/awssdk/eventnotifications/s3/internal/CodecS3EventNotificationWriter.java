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

package software.amazon.awssdk.eventnotifications.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

/**
 * Writes an S3 event notification with the JSON codec, through the model classes' {@code serializeMembers}. Pretty
 * output has the layout of Jackson's default pretty printer, which the previous writer used, with {@code \n} line
 * endings.
 */
@SdkInternalApi
public final class CodecS3EventNotificationWriter implements S3EventNotificationWriter {
    private static final S3EventNotificationWriter INSTANCE = builder().build();

    private final Boolean prettyPrint;

    private CodecS3EventNotificationWriter(DefaultBuilder builder) {
        this.prettyPrint = builder.prettyPrint;
    }

    public static S3EventNotificationWriter create() {
        return INSTANCE;
    }

    public static S3EventNotificationWriter.Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public String writeToString(S3EventNotification event) {
        return Boolean.TRUE.equals(prettyPrint) ? S3EventNotificationSerde.PRETTY_JSON.serializeToString(event)
                                                : S3EventNotificationSerde.JSON.serializeToString(event);
    }

    @Override
    public S3EventNotificationWriter.Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    private static final class DefaultBuilder implements S3EventNotificationWriter.Builder {
        private Boolean prettyPrint;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CodecS3EventNotificationWriter writer) {
            this.prettyPrint = writer.prettyPrint;
        }

        @Override
        public S3EventNotificationWriter.Builder prettyPrint(Boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        @Override
        public S3EventNotificationWriter build() {
            return new CodecS3EventNotificationWriter(this);
        }
    }
}
