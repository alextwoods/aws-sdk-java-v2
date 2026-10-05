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

package software.amazon.awssdk.transfer.s3.internal.model;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.utils.ToString;

/** The {@link DirectoryUpload} handle. */
public final class DefaultDirectoryUpload implements DirectoryUpload {
    private final CompletableFuture<CompletedDirectoryUpload> completionFuture;

    public DefaultDirectoryUpload(CompletableFuture<CompletedDirectoryUpload> completionFuture) {
        this.completionFuture = completionFuture;
    }

    @Override
    public CompletableFuture<CompletedDirectoryUpload> completionFuture() {
        return completionFuture;
    }

    @Override
    public String toString() {
        return ToString.builder("DirectoryUpload").add("completionFuture", completionFuture).build();
    }
}
