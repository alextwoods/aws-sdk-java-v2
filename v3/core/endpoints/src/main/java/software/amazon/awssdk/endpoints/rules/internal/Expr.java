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

package software.amazon.awssdk.endpoints.rules.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.endpoints.internal.EndpointValues;

/**
 * An expression of the endpoint rules language, parsed once from a canonical rule set and evaluated against a
 * {@link Scope}. Values are the rules value types of {@link EndpointValues}; {@code null} is the empty value.
 *
 * <p>Evaluation follows the canonical Smithy rules evaluator exactly: a reference to an unset name is empty; the
 * built-in functions {@code isSet}, {@code not}, {@code booleanEquals}, {@code stringEquals}, {@code getAttr},
 * {@code coalesce} and {@code ite} are evaluated here, and every other function is a {@link LibraryFunction} applied to
 * its evaluated arguments. A value of the wrong type is a {@link RuleEvaluationError}; canonical rule sets are
 * type-checked when they are built, so this indicates an invalid rule set.
 */
public sealed interface Expr {

    /** Evaluates the expression in {@code scope}. */
    Object eval(Scope scope);

    /** A reference to a parameter or an assigned condition result. */
    record Ref(String name) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return scope.get(name);
        }
    }

    /** A boolean, integer or static string literal. */
    record Literal(Object value) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return value;
        }
    }

    /** A string template with at least one dynamic part; {@code parts} are literal strings and expressions. */
    record Template(String source, List<Object> parts) implements Expr {
        public Template {
            parts = List.copyOf(parts);
        }

        @Override
        public Object eval(Scope scope) {
            StringBuilder builder = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof String literal) {
                    builder.append(literal);
                } else {
                    builder.append(expectString(((Expr) part).eval(scope), "template \"" + source + "\""));
                }
            }
            return builder.toString();
        }
    }

    /** A record literal (an endpoint property object). */
    record RecordLiteral(Map<String, Expr> members) implements Expr {
        @Override
        public Object eval(Scope scope) {
            Map<String, Object> result = new LinkedHashMap<>();
            members.forEach((name, member) -> result.put(name, member.eval(scope)));
            return Collections.unmodifiableMap(result);
        }
    }

    /** A tuple (array) literal. */
    record TupleLiteral(List<Expr> elements) implements Expr {
        @Override
        public Object eval(Scope scope) {
            List<Object> result = new ArrayList<>(elements.size());
            for (Expr element : elements) {
                result.add(element.eval(scope));
            }
            return Collections.unmodifiableList(result);
        }
    }

    /** {@code isSet(value)}: whether the value is not empty. */
    record IsSet(Expr value) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return value.eval(scope) != null;
        }
    }

    /** {@code not(value)}. */
    record Not(Expr value) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return !expectBoolean(value.eval(scope), "not");
        }
    }

    /** {@code booleanEquals(left, right)}. */
    record BooleanEquals(Expr left, Expr right) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return expectBoolean(left.eval(scope), "booleanEquals") == expectBoolean(right.eval(scope),
                                                                                     "booleanEquals");
        }
    }

    /** {@code stringEquals(left, right)}. */
    record StringEquals(Expr left, Expr right) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return expectString(left.eval(scope), "stringEquals")
                .equals(expectString(right.eval(scope), "stringEquals"));
        }
    }

    /**
     * {@code getAttr(target, path)} and the {@code {Name#path}} template short form: record keys and (possibly
     * negative) list indexes; a missing key or out-of-range index is empty.
     */
    record GetAttr(Expr target, List<Object> path) implements Expr {
        public GetAttr {
            path = List.copyOf(path);
        }

        @Override
        public Object eval(Scope scope) {
            Object current = target.eval(scope);
            for (Object part : path) {
                if (current == null) {
                    return null;
                }
                if (part instanceof String key) {
                    if (!(current instanceof Map<?, ?> record)) {
                        throw new RuleEvaluationError("getAttr key '" + key + "' applied to a non-record value "
                                                      + EndpointValues.render(current));
                    }
                    current = record.get(key);
                } else {
                    int index = (Integer) part;
                    if (!(current instanceof List<?> list)) {
                        throw new RuleEvaluationError("getAttr index [" + index + "] applied to a non-array value "
                                                      + EndpointValues.render(current));
                    }
                    int resolved = index >= 0 ? index : list.size() + index;
                    current = resolved >= 0 && resolved < list.size() ? list.get(resolved) : null;
                }
            }
            return current;
        }
    }

    /** {@code coalesce(a, b, ...)}: the first non-empty argument, evaluated lazily. */
    record Coalesce(List<Expr> values) implements Expr {
        @Override
        public Object eval(Scope scope) {
            for (Expr value : values) {
                Object result = value.eval(scope);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    /** {@code ite(condition, then, else)}: evaluates only the selected branch. */
    record Ite(Expr condition, Expr then, Expr otherwise) implements Expr {
        @Override
        public Object eval(Scope scope) {
            return expectBoolean(condition.eval(scope), "ite") ? then.eval(scope) : otherwise.eval(scope);
        }
    }

    /** A library function applied to its evaluated arguments. */
    record Call(LibraryFunction function, List<Expr> arguments) implements Expr {
        @Override
        public Object eval(Scope scope) {
            Object[] values = new Object[arguments.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = arguments.get(i).eval(scope);
            }
            return function.apply(values);
        }
    }

    static boolean expectBoolean(Object value, String where) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new RuleEvaluationError(where + " expected a boolean but got " + EndpointValues.render(value));
    }

    static String expectString(Object value, String where) {
        if (value instanceof String s) {
            return s;
        }
        throw new RuleEvaluationError(where + " expected a string but got " + EndpointValues.render(value));
    }
}
