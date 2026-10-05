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

package software.amazon.awssdk.transfer.s3.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Stream helpers used by the resumable-transfer serializers. Not a public API. */
public final class IoUtils {
    private IoUtils() {
    }

    /** Copies {@code in} to {@code out} and returns the number of bytes copied; neither stream is closed. */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        return in.transferTo(out);
    }
}
