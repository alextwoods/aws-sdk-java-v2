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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.CodeBlock;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Conservative estimates of the static-initializer bytecode that generated schema expressions compile to. The
 * Schema_Generator sums them to keep each partition class's {@code <clinit>} under its budget (Requirement 10.7).
 *
 * <p>smithy-java partitions by the character count of the generated source, which is not comparable for JavaPoet
 * output. These estimates count instructions instead: 3 bytes for each {@code ldc_w}, {@code getstatic},
 * {@code putstatic} and invoke instruction, 4 for each {@code new}/{@code dup} pair, and 4 for each array element
 * store. They overestimate, which only makes partitions smaller.
 */
@SdkInternalApi
final class BytecodeEstimate {
    /** {@code ldc}, {@code getstatic}, {@code putstatic} or an invoke instruction. */
    static final int INSTRUCTION = 3;
    /** {@code ShapeId.fromParts(ns, name)}: two constants and an {@code invokestatic}. */
    static final int SHAPE_ID = 3 * INSTRUCTION;
    /** {@code new}, {@code dup}. */
    static final int NEW = 4;
    /** The array length constant and {@code anewarray}. */
    static final int ARRAY = 5;
    /** {@code dup}, the index constant and {@code aastore} for one array element. */
    static final int ARRAY_ELEMENT = 4;

    private BytecodeEstimate() {
    }

    /**
     * @param traits the rendered trait expressions passed as varargs
     * @return the bytes that build the varargs array
     */
    static int traitArray(List<CodeBlock> traits) {
        int size = ARRAY;
        for (CodeBlock trait : traits) {
            size += ARRAY_ELEMENT + expression(trait);
        }
        return size;
    }

    /**
     * Estimates an arbitrary rendered expression, such as a trait initializer or a Document literal, from its tokens:
     * every string, number, method call, constructor and upper-case field reference costs an instruction, every
     * {@code new} a {@code new}/{@code dup} pair, and every comma an array element store.
     *
     * @param expression the expression
     * @return the estimated bytes
     */
    static int expression(CodeBlock expression) {
        String text = expression.toString();
        int size = 0;
        int i = 0;
        int length = text.length();
        while (i < length) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipLiteral(text, i, c);
                size += INSTRUCTION;
            } else if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                while (i < length && Character.isJavaIdentifierPart(text.charAt(i))) {
                    i++;
                }
                String word = text.substring(start, i);
                boolean call = i < length && text.charAt(i) == '(';
                boolean afterDot = start > 0 && text.charAt(start - 1) == '.';
                if ("new".equals(word)) {
                    size += NEW;
                } else if ("true".equals(word) || "false".equals(word) || "null".equals(word)) {
                    size += 1;
                } else if (call || afterDot && isConstantName(word)) {
                    size += INSTRUCTION;
                }
            } else if (Character.isDigit(c)) {
                while (i < length && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '.')) {
                    i++;
                }
                size += INSTRUCTION;
            } else {
                if (c == ',') {
                    size += ARRAY_ELEMENT;
                }
                i++;
            }
        }
        return size;
    }

    private static boolean isConstantName(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

    private static int skipLiteral(String text, int start, char quote) {
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                return i + 1;
            } else {
                i++;
            }
        }
        return i;
    }
}
