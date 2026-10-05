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

package software.amazon.awssdk.core.waiters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The runtime of the JMESPath subset generated waiter acceptors evaluate ({@code smithy.waiters#waitable} matchers):
 * null-propagating field access, list and map-value projections that drop null results, multi-select lists,
 * {@code length}, {@code contains}, comparisons, and the
 * {@code stringEquals}, {@code booleanEquals}, {@code allStringEquals} and {@code anyStringEquals} comparators.
 * Generated code compiles each path to typed getter calls over these helpers; nothing is parsed at run time.
 */
public final class WaiterMatchers {
    private WaiterMatchers() {
    }

    /** {@code getter} applied to {@code value}, or {@code null} when {@code value} is not an {@code S}. */
    public static <S> Object field(Object value, Class<S> type, Function<? super S, ?> getter) {
        return type.isInstance(value) ? getter.apply(type.cast(value)) : null;
    }

    /**
     * A JMESPath projection: {@code projection} of each element of the list {@code value}, without {@code null}
     * results; {@code null} when {@code value} is not a list.
     */
    public static List<Object> project(Object value, Function<Object, ?> projection) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<Object> result = new ArrayList<>(list.size());
        for (Object element : list) {
            Object projected = projection.apply(element);
            if (projected != null) {
                result.add(projected);
            }
        }
        return result;
    }

    /** A JMESPath flatten: lists of {@code value} merged one level; {@code null} when it is not a list. */
    public static List<Object> flatten(Object value) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<Object> result = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof List<?> inner) {
                result.addAll(inner);
            } else if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    /** JMESPath {@code length}: of a string, list or map; {@code null} otherwise. */
    public static Object length(Object value) {
        if (value instanceof CharSequence string) {
            return string.codePoints().count();
        }
        if (value instanceof Collection<?> collection) {
            return (long) collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return (long) map.size();
        }
        return null;
    }

    /**
     * A JMESPath comparison ({@code ==}, {@code !=}, {@code <}, {@code <=}, {@code >}, {@code >=}): numbers compare
     * by value, ordering comparisons of non-numbers are {@code null}.
     */
    public static Object compare(Object left, String comparator, Object right) {
        if (left instanceof Number l && right instanceof Number r) {
            int order = new BigDecimal(l.toString()).compareTo(new BigDecimal(r.toString()));
            return switch (comparator) {
                case "==" -> order == 0;
                case "!=" -> order != 0;
                case "<" -> order < 0;
                case "<=" -> order <= 0;
                case ">" -> order > 0;
                case ">=" -> order >= 0;
                default -> throw new IllegalArgumentException(comparator);
            };
        }
        return switch (comparator) {
            case "==" -> Objects.equals(text(left), text(right));
            case "!=" -> !Objects.equals(text(left), text(right));
            default -> null;
        };
    }

    /** JMESPath filter: the elements of the list {@code value} whose {@code condition} is truthy. */
    public static List<Object> filter(Object value, Function<Object, ?> condition) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<Object> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (truthy(condition.apply(element))) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * The values of the map {@code value}, in its iteration order, for a JMESPath object projection ({@code a.*.b});
     * {@code null} when {@code value} is not a map.
     */
    public static List<Object> values(Object value) {
        return value instanceof Map<?, ?> map ? new ArrayList<>(map.values()) : null;
    }

    /**
     * JMESPath {@code contains(subject, search)}: whether the list {@code subject} has an element equal to
     * {@code search}, or the string {@code subject} contains the string {@code search}; {@code null} for any other
     * subject.
     */
    public static Object contains(Object subject, Object search) {
        if (subject instanceof List<?> list) {
            return list.stream().anyMatch(element -> Boolean.TRUE.equals(compare(element, "==", search)));
        }
        if (subject instanceof CharSequence string) {
            return search instanceof CharSequence s && string.toString().contains(s);
        }
        return null;
    }

    /**
     * A JMESPath multi-select list ({@code [a, b]}): {@code select} applied to {@code value}, or {@code null} when
     * {@code value} is {@code null}.
     */
    public static List<Object> multiSelect(Object value, Function<Object, List<Object>> select) {
        return value == null ? null : select.apply(value);
    }

    /** The items of a multi-select list, in order; {@code null} items are kept, as JMESPath requires. */
    public static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    /** JMESPath {@code &&}: {@code left} when it is falsy, else {@code right}. */
    public static Object and(Object left, Object right) {
        return truthy(left) ? right : left;
    }

    /** JMESPath {@code ||}: {@code left} when it is truthy, else {@code right}. */
    public static Object or(Object left, Object right) {
        return truthy(left) ? left : right;
    }

    /** JMESPath {@code !}. */
    public static Object not(Object value) {
        return !truthy(value);
    }

    /** JMESPath truthiness: not null, false, or an empty string, list or map. */
    public static boolean truthy(Object value) {
        if (value == null || Boolean.FALSE.equals(value)) {
            return false;
        }
        if (value instanceof CharSequence string) {
            return !string.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return true;
    }

    /** The {@code stringEquals} comparator. */
    public static boolean stringEquals(Object value, String expected) {
        return value != null && expected.equals(text(value));
    }

    /** The {@code booleanEquals} comparator. */
    public static boolean booleanEquals(Object value, boolean expected) {
        return value instanceof Boolean b && b == expected;
    }

    /** The {@code allStringEquals} comparator: a non-empty list whose every element equals {@code expected}. */
    public static boolean allStringEquals(Object value, String expected) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        return list.stream().allMatch(element -> stringEquals(element, expected));
    }

    /** The {@code anyStringEquals} comparator: a list with an element equal to {@code expected}. */
    public static boolean anyStringEquals(Object value, String expected) {
        return value instanceof List<?> list && list.stream().anyMatch(element -> stringEquals(element, expected));
    }

    private static Object text(Object value) {
        return value instanceof CharSequence || value instanceof Enum<?> ? value.toString() : value;
    }
}
