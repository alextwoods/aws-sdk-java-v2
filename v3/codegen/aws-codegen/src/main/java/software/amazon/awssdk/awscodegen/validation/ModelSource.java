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

package software.amazon.awssdk.awscodegen.validation;

import java.nio.file.Path;
import java.util.Objects;

/**
 * One classified file of a service's model directory.
 *
 * @param relativePath path of the file relative to the model directory
 * @param format the detected format
 * @param detail how the format was identified plus any identity the file itself declares (for example a C2J model's
 *        {@code serviceId}); for unreadable files, the read or parse failure
 */
public record ModelSource(Path relativePath, ModelSourceFormat format, String detail) {

    public ModelSource {
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(detail, "detail");
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("Model source path must be relative: " + relativePath);
        }
    }

    /** The relative path with {@code /} separators on every platform. */
    public String portablePath() {
        return ModelSourceValidator.portable(relativePath);
    }
}
