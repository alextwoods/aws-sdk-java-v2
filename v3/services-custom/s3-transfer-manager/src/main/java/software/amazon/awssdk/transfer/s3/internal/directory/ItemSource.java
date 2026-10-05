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

package software.amazon.awssdk.transfer.s3.internal.directory;

/**
 * The items of a directory transfer, pulled by a {@link BoundedDispatcher} from its (serialized) pull loop.
 */
interface ItemSource<T> {

    /** The next item, or {@code null} if none is available now. May block on file-system I/O. */
    T poll() throws Exception;

    /** True once every item has been returned. */
    boolean exhausted();

    /** Registers the callback an asynchronous source calls when items may have become available. */
    default void onAvailable(Runnable callback) {
    }

    /** Releases the source (directory stream, listing subscription); idempotent. */
    void close();
}
