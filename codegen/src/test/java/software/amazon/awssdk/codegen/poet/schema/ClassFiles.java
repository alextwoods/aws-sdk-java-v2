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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the bytecode length of a compiled class's static initializer, to check the Schema_Generator's size estimate
 * against what {@code javac} produces.
 */
final class ClassFiles {
    private ClassFiles() {
    }

    /**
     * @return the {@code code_length} of the class's {@code <clinit>} method, or 0 if it has none
     */
    static int staticInitializerLength(Path classFile) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(classFile))) {
            in.readInt();
            in.readUnsignedShort();
            in.readUnsignedShort();
            int count = in.readUnsignedShort();
            String[] utf8 = new String[count];
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1:
                        utf8[i] = in.readUTF();
                        break;
                    case 3:
                    case 4:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 17:
                    case 18:
                        in.readInt();
                        break;
                    case 5:
                    case 6:
                        in.readLong();
                        i++;
                        break;
                    case 7:
                    case 8:
                    case 16:
                    case 19:
                    case 20:
                        in.readUnsignedShort();
                        break;
                    case 15:
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                        break;
                    default:
                        throw new IllegalStateException("Unknown constant pool tag " + tag + " in " + classFile);
                }
            }
            in.readUnsignedShort();
            in.readUnsignedShort();
            in.readUnsignedShort();
            skip(in, in.readUnsignedShort() * 2);
            int fields = in.readUnsignedShort();
            for (int i = 0; i < fields; i++) {
                in.readUnsignedShort();
                in.readUnsignedShort();
                in.readUnsignedShort();
                skipAttributes(in);
            }
            int methods = in.readUnsignedShort();
            for (int i = 0; i < methods; i++) {
                in.readUnsignedShort();
                String name = utf8[in.readUnsignedShort()];
                in.readUnsignedShort();
                int attributes = in.readUnsignedShort();
                for (int a = 0; a < attributes; a++) {
                    String attribute = utf8[in.readUnsignedShort()];
                    int length = in.readInt();
                    if ("<clinit>".equals(name) && "Code".equals(attribute)) {
                        in.readUnsignedShort();
                        in.readUnsignedShort();
                        return in.readInt();
                    }
                    skip(in, length);
                }
            }
            return 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int attributes = in.readUnsignedShort();
        for (int a = 0; a < attributes; a++) {
            in.readUnsignedShort();
            skip(in, in.readInt());
        }
    }

    private static void skip(DataInputStream in, int bytes) throws IOException {
        int remaining = bytes;
        while (remaining > 0) {
            int skipped = in.skipBytes(remaining);
            if (skipped <= 0) {
                throw new IOException("Unexpected end of class file");
            }
            remaining -= skipped;
        }
    }
}
