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

package software.amazon.awssdk.codecs.json.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Indents the compact JSON that {@link SmithyJsonSerializer} writes, as it passes through to a sink, in the layout of
 * Jackson's {@code DefaultPrettyPrinter} (which the SDK's legacy JSON writers use for pretty output):
 * <pre>
 * {
 *   "name" : "value",
 *   "list" : [ 1, 2 ],
 *   "nested" : {
 *     "empty" : { },
 *     "none" : [ ]
 *   }
 * }
 * </pre>
 * Objects put each member on its own line, indented two spaces per object level; arrays stay on one line with a space
 * after {@code [} and each comma and before {@code ]}. Lines end with {@code \n}.
 *
 * <p>SDK addition: smithy-java's native provider delegates {@code prettyPrint} to its Jackson provider, which is not
 * ported. The input must be compact JSON (no whitespace outside strings), which is what the serializer writes. State
 * is kept across writes, so the input may arrive in any chunks. Not thread-safe; closing it does not close the sink.
 */
@SdkInternalApi
final class JsonPrettyPrinter extends OutputStream {

    private final OutputStream sink;

    private byte[] out = new byte[1024];
    private int outPos;

    private boolean inString;
    private boolean escaped;

    // Kinds of the open containers, innermost last: true for an object, false for an array.
    private boolean[] containers = new boolean[16];
    private int containerCount;
    private int objectDepth;

    // Whether the previous byte opened a container that has neither a value nor its end yet.
    private boolean pendingOpen;

    JsonPrettyPrinter(OutputStream sink) {
        this.sink = sink;
    }

    @Override
    public void write(int b) throws IOException {
        process((byte) b);
        drain();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            process(b[i]);
        }
        drain();
    }

    @Override
    public void flush() throws IOException {
        drain();
        sink.flush();
    }

    @Override
    public void close() throws IOException {
        drain();
    }

    private void process(byte b) {
        if (inString) {
            put(b);
            if (escaped) {
                escaped = false;
            } else if (b == '\\') {
                escaped = true;
            } else if (b == '"') {
                inString = false;
            }
            return;
        }

        if (pendingOpen) {
            pendingOpen = false;
            if (b == '}' || b == ']') {
                // Empty container: "{ }" or "[ ]".
                closeContainer();
                put((byte) ' ');
                put(b);
                return;
            }
            if (innermostIsObject()) {
                newLine();
            } else {
                put((byte) ' ');
            }
        }

        switch (b) {
            case '{':
            case '[':
                put(b);
                openContainer(b == '{');
                pendingOpen = true;
                break;
            case '}':
                closeContainer();
                newLine();
                put(b);
                break;
            case ']':
                closeContainer();
                put((byte) ' ');
                put(b);
                break;
            case ',':
                put(b);
                // Object members go on new lines; array elements stay on the line.
                if (innermostIsObject()) {
                    newLine();
                } else {
                    put((byte) ' ');
                }
                break;
            case ':':
                put((byte) ' ');
                put(b);
                put((byte) ' ');
                break;
            case '"':
                put(b);
                inString = true;
                break;
            default:
                put(b);
                break;
        }
    }

    private void openContainer(boolean object) {
        if (containerCount == containers.length) {
            containers = Arrays.copyOf(containers, containers.length * 2);
        }
        containers[containerCount++] = object;
        if (object) {
            objectDepth++;
        }
    }

    private void closeContainer() {
        if (containerCount > 0 && containers[--containerCount]) {
            objectDepth--;
        }
    }

    private boolean innermostIsObject() {
        return containerCount > 0 && containers[containerCount - 1];
    }

    private void newLine() {
        ensureCapacity(1 + 2 * objectDepth);
        out[outPos++] = '\n';
        for (int i = 0; i < objectDepth; i++) {
            out[outPos++] = ' ';
            out[outPos++] = ' ';
        }
    }

    private void put(byte b) {
        ensureCapacity(1);
        out[outPos++] = b;
    }

    private void ensureCapacity(int needed) {
        if (outPos + needed > out.length) {
            out = Arrays.copyOf(out, Math.max(out.length * 2, outPos + needed));
        }
    }

    private void drain() throws IOException {
        if (outPos > 0) {
            sink.write(out, 0, outPos);
            outPos = 0;
        }
    }
}
