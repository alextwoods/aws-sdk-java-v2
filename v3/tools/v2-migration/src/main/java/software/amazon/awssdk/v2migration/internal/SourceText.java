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

package software.amazon.awssdk.v2migration.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.v2migration.SourceLocation;

/** The text of one project file and its line index, for offset-to-location conversion. */
public final class SourceText {

    private static final int MAX_CONSTRUCT_LENGTH = 160;

    private final String path;
    private final String content;
    private final int[] lineStarts;

    public SourceText(String path, String content) {
        this.path = path;
        this.content = content;
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\n' || (c == '\r' && (i + 1 == content.length() || content.charAt(i + 1) != '\n'))) {
                starts.add(i + 1);
            }
        }
        this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
    }

    public String path() {
        return path;
    }

    public String content() {
        return content;
    }

    /** The location of {@code [start, end)}, clamped to the text. */
    public SourceLocation location(int start, int end) {
        int s = Math.max(0, Math.min(start, content.length()));
        int e = Math.max(s, Math.min(end, content.length()));
        int startLine = lineOf(s);
        int endLine = lineOf(e);
        return new SourceLocation(path, s, e, startLine + 1, s - lineStarts[startLine] + 1, endLine + 1,
                                  e - lineStarts[endLine] + 1);
    }

    /** The whitespace-normalized text of {@code [start, end)}, abbreviated to a report-friendly length. */
    public String construct(int start, int end) {
        return abbreviate(content.substring(start, end));
    }

    /** {@code text} with whitespace runs collapsed to one space, abbreviated with an ellipsis. */
    public static String abbreviate(String text) {
        String normalized = text.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_CONSTRUCT_LENGTH) {
            return normalized;
        }
        int cut = MAX_CONSTRUCT_LENGTH - 1;
        if (Character.isHighSurrogate(normalized.charAt(cut - 1))) {
            cut--;
        }
        return normalized.substring(0, cut) + "\u2026";
    }

    private int lineOf(int offset) {
        int index = Arrays.binarySearch(lineStarts, offset);
        return index >= 0 ? index : -index - 2;
    }
}
