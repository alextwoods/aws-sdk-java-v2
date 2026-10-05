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

import software.amazon.awssdk.core.async.AsyncRequestBody;

/**
 * One part of an upload: its 1-based number, length, replayable body (every subscription reads the part from its first
 * byte) and whether it is known to be the object's last part. {@link #release()} returns the part's buffer capacity to
 * its source once the part's request has finished; it is idempotent.
 */
public final class UploadPart {
    private final int partNumber;
    private final long length;
    private final AsyncRequestBody body;
    private final boolean last;
    private final Runnable release;
    private boolean released;

    public UploadPart(int partNumber, long length, AsyncRequestBody body, boolean last, Runnable release) {
        this.partNumber = partNumber;
        this.length = length;
        this.body = body;
        this.last = last;
        this.release = release;
    }

    public int partNumber() {
        return partNumber;
    }

    public long length() {
        return length;
    }

    public AsyncRequestBody body() {
        return body;
    }

    public boolean last() {
        return last;
    }

    public void release() {
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
        }
        release.run();
    }
}
