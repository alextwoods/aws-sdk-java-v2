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

package software.amazon.awssdk.transfer.s3.internal.upload;

/**
 * A pull-based source of the parts of an upload, in part-number order: the replayable split-body factory of a
 * multipart upload. The uploader has at most one {@link #next} outstanding.
 */
public interface PartSource {

    /** Asks for the next part; exactly one receiver method is called, possibly on this thread. */
    void next(Receiver receiver);

    /** Stops producing parts and releases buffers and subscriptions; idempotent. */
    void cancel();

    /** The callbacks of one {@link #next} call. */
    interface Receiver {
        void onPart(UploadPart part);

        /** No more parts. */
        void onEnd();

        void onError(Throwable error);
    }
}
