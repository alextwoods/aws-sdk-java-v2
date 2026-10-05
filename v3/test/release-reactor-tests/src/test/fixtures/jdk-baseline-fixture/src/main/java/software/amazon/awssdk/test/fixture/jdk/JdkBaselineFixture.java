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

package software.amazon.awssdk.test.fixture.jdk;

import java.util.List;

/**
 * Uses language features and APIs that are final in Java 21 (sealed interfaces, records, record patterns, pattern
 * matching for switch, sequenced collections), so it compiles only with {@code --release 21} or later and would fail
 * to compile on an older JDK if the build did not reject that JDK first.
 */
public final class JdkBaselineFixture {
    private JdkBaselineFixture() {
    }

    /** A closed shape hierarchy. */
    public sealed interface Shape permits Circle, Square {
    }

    public record Circle(double radius) implements Shape {
    }

    public record Square(double side) implements Shape {
    }

    public static String describe(Object value) {
        return switch (value) {
            case null -> "nothing";
            case Circle(double radius) when radius == 0 -> "point";
            case Circle(double radius) -> "circle of radius " + radius;
            case Square(double side) -> "square of side " + side;
            default -> "unknown";
        };
    }

    public static Shape last(List<Shape> shapes) {
        return shapes.getLast();
    }
}
