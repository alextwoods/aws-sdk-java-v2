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

package software.amazon.awssdk.transfer.s3.model;

import java.util.concurrent.CompletableFuture;

/**
 * An upload transfer of a single object to S3.
 */
public interface FileUpload extends ObjectTransfer {

    /**
     * Pauses the current upload operation and return the information that can
     * be used to resume the upload at a later time.
     * <p>
     * The information object is serializable for persistent storage until it should be resumed.
     * See {@link ResumableFileUpload} for supported formats.
     *
     * <p>
     * ADAPTED from V2: the V3 Transfer Manager runs multipart uploads itself over the public {@code S3AsyncClient} API,
     * so pausing is supported with every client. Pausing stops the parts in flight without aborting the multipart
     * upload; a file smaller than the multipart threshold is uploaded with a single request, which pausing cancels, and
     * the returned token then resumes by uploading the whole file again.
     *
     * @return A {@link ResumableFileUpload} that can be used to resume the upload.
     */
    ResumableFileUpload pause();

    @Override
    CompletableFuture<CompletedFileUpload> completionFuture();
}
