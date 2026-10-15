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

package software.amazon.awssdk.sizereport;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * How the bytes of one class file are attributed to the {@link SizeCategory categories}.
 *
 * <p>Every class has exactly one {@link #category() category}. A {@link Carve carve} moves a measured part of the
 * class's bytes (whole methods, fields and static-initializer segments, see {@link ClassAttributor}) to another
 * category. The class's own category keeps the rest, so {@link #categoryBytes()} always adds up to
 * {@link #bytes()}.
 */
@SdkInternalApi
final class ClassAttribution {
    private final String className;
    private final long bytes;
    private final SizeCategory category;
    private final List<Carve> carves;

    ClassAttribution(String className, long bytes, SizeCategory category, List<Carve> carves) {
        this.className = className;
        this.bytes = bytes;
        this.category = category;
        this.carves = Collections.unmodifiableList(carves);
    }

    /**
     * The internal class name.
     */
    public String className() {
        return className;
    }

    /**
     * The size of the class file in bytes.
     */
    public long bytes() {
        return bytes;
    }

    /**
     * The class's category.
     */
    public SizeCategory category() {
        return category;
    }

    /**
     * The parts of the class attributed to other categories.
     */
    public List<Carve> carves() {
        return carves;
    }

    /**
     * The class's bytes by category: the carves, and the rest in {@link #category()}. The values add up to
     * {@link #bytes()}.
     */
    public Map<SizeCategory, Long> categoryBytes() {
        Map<SizeCategory, Long> result = new EnumMap<>(SizeCategory.class);
        long carved = 0;
        for (Carve carve : carves) {
            result.merge(carve.category(), carve.bytes(), Long::sum);
            carved += carve.bytes();
        }
        result.merge(category, bytes - carved, Long::sum);
        return result;
    }

    /**
     * A part of a class attributed to another category by bytecode attribution.
     */
    @SdkInternalApi
    static final class Carve {
        private final SizeCategory category;
        private final long bytes;
        private final List<String> methods;
        private final List<String> fields;
        private final int staticInitializerSegments;

        Carve(SizeCategory category, long bytes, List<String> methods, List<String> fields,
              int staticInitializerSegments) {
            this.category = category;
            this.bytes = bytes;
            this.methods = Collections.unmodifiableList(methods);
            this.fields = Collections.unmodifiableList(fields);
            this.staticInitializerSegments = staticInitializerSegments;
        }

        /**
         * The category the bytes are attributed to.
         */
        public SizeCategory category() {
            return category;
        }

        /**
         * The class-file bytes of the carved members: the class's size minus its size without them.
         */
        public long bytes() {
            return bytes;
        }

        /**
         * The carved methods, as {@code name + descriptor}.
         */
        public List<String> methods() {
            return methods;
        }

        /**
         * The carved fields.
         */
        public List<String> fields() {
            return fields;
        }

        /**
         * The number of carved {@code <clinit>} segments (the code that initializes a carved static field).
         */
        public int staticInitializerSegments() {
            return staticInitializerSegments;
        }
    }
}
