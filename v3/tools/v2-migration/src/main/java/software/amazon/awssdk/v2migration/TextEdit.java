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

package software.amazon.awssdk.v2migration;

import java.util.Objects;

/**
 * Replaces the characters {@code [startOffset, endOffset)} of one project file with {@code replacement}. An edit with
 * {@code startOffset == endOffset} is an insertion. Offsets index the file as originally read; the engine applies all
 * accepted edits of a file at once, so recipes never see each other's output.
 *
 * @param path        the project-relative path with {@code /} separators
 * @param startOffset the first replaced character
 * @param endOffset   the character after the replaced range
 * @param replacement the new text
 */
public record TextEdit(String path, int startOffset, int endOffset, String replacement) {

    public TextEdit {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(replacement, "replacement");
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid edit range [" + startOffset + ", " + endOffset + ")");
        }
    }

    /** Replaces the text of {@code location}. */
    public static TextEdit replace(SourceLocation location, String replacement) {
        return new TextEdit(location.path(), location.startOffset(), location.endOffset(), replacement);
    }

    /** Inserts {@code text} at {@code offset} of {@code path}. */
    public static TextEdit insert(String path, int offset, String text) {
        return new TextEdit(path, offset, offset, text);
    }

    /** True if the edit replaces no characters. */
    public boolean isInsertion() {
        return startOffset == endOffset;
    }
}
