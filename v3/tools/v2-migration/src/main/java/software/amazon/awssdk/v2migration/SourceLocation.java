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

import java.util.Comparator;
import java.util.Objects;

/**
 * A source range in one project file. Offsets index the file's decoded characters (UTF-16 code units, the unit of
 * {@link String}); lines and columns are 1-based, columns count characters from the start of the line, and the end is
 * exclusive. A zero-length range ({@code startOffset == endOffset}) marks a position: it locates a construct that has
 * no editable text of its own in the file, such as a dependency inherited through the dependency graph.
 *
 * @param path        the project-relative path with {@code /} separators
 * @param startOffset the first character of the range
 * @param endOffset   the character after the range
 * @param line        the line of {@code startOffset}
 * @param column      the column of {@code startOffset}
 * @param endLine     the line of {@code endOffset}
 * @param endColumn   the column of {@code endOffset}
 */
public record SourceLocation(String path, int startOffset, int endOffset, int line, int column, int endLine,
                             int endColumn) implements Comparable<SourceLocation> {

    private static final Comparator<SourceLocation> ORDER =
        Comparator.comparing(SourceLocation::path)
                  .thenComparingInt(SourceLocation::startOffset)
                  .thenComparingInt(SourceLocation::endOffset);

    public SourceLocation {
        Objects.requireNonNull(path, "path");
        if (startOffset < 0 || endOffset < startOffset || line < 1 || column < 1 || endLine < line) {
            throw new IllegalArgumentException("Invalid source range " + path + " [" + startOffset + ", " + endOffset
                                               + ") " + line + ":" + column + "-" + endLine + ":" + endColumn);
        }
    }

    /** True if the range has no characters. */
    public boolean isPosition() {
        return startOffset == endOffset;
    }

    /** True if this range and {@code [start, end)} share at least one character of the same file. */
    public boolean overlaps(String otherPath, int start, int end) {
        return path.equals(otherPath) && start < endOffset && startOffset < end;
    }

    /** True if {@code offset} lies strictly inside this range (not at either boundary). */
    public boolean strictlyContains(String otherPath, int offset) {
        return path.equals(otherPath) && startOffset < offset && offset < endOffset;
    }

    /** {@code path:line:column}. */
    @Override
    public String toString() {
        return path + ":" + line + ":" + column;
    }

    @Override
    public int compareTo(SourceLocation other) {
        return ORDER.compare(this, other);
    }
}
