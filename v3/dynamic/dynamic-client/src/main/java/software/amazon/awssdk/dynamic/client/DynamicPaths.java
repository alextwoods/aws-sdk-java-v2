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

package software.amazon.awssdk.dynamic.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.core.waiters.WaiterMatchers;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;

/**
 * Waiter acceptor paths over documents: the JMESPath subset {@code aws-codegen} compiles generated acceptors from
 * (field access, sub-expressions, list and filter projections, flattening, {@code length}, comparisons, {@code &&},
 * {@code ||}, {@code !}, literals and the current node), evaluated with the same {@link WaiterMatchers} runtime the
 * generated acceptors call. Where a generated acceptor calls a typed getter, a compiled path reads the document
 * member of the same name; a path outside the subset is rejected when the waiter is compiled, as generation rejects
 * it.
 *
 * <p>Values are those of generated getters: a structure or union is its document, a list a {@code List}, a map a
 * {@code Map}, a string or enum a {@code String}, a number a {@code Number}, a boolean a {@code Boolean}, a
 * timestamp an {@code Instant}, a blob a {@code ByteBuffer}, and an absent member {@code null}.
 */
final class DynamicPaths {

    private DynamicPaths() {
    }

    /** A path outside the supported subset. */
    static final class UnsupportedPathException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UnsupportedPathException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * {@code path} compiled to a function of the value it is applied to (the output document, or the
     * {@code {input, output}} map of an {@code inputOutput} matcher).
     *
     * @throws UnsupportedPathException if {@code path} is not valid JMESPath or is outside the supported subset
     */
    static Function<Object, Object> compile(String path) {
        JmespathExpression expression;
        try {
            expression = JmespathExpression.parse(path);
        } catch (RuntimeException e) {
            throw new UnsupportedPathException("invalid JMESPath " + path, e);
        }
        return compile(expression, path);
    }

    /** The root of an {@code inputOutput} path: the object of the request's input and the poll's output. */
    static Object inputOutput(Document input, Document output) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("input", input);
        root.put("output", output);
        return root;
    }

    private static Function<Object, Object> compile(JmespathExpression expression, String path) {
        if (expression instanceof CurrentExpression) {
            return current -> current;
        }
        if (expression instanceof FieldExpression field) {
            String name = field.getName();
            return current -> field(current, name);
        }
        if (expression instanceof Subexpression sub) {
            Function<Object, Object> left = compile(sub.getLeft(), path);
            Function<Object, Object> right = compile(sub.getRight(), path);
            return current -> right.apply(left.apply(current));
        }
        if (expression instanceof FlattenExpression flatten) {
            Function<Object, Object> list = compile(flatten.getExpression(), path);
            return current -> WaiterMatchers.flatten(list.apply(current));
        }
        if (expression instanceof ProjectionExpression projection) {
            Function<Object, Object> list = compile(projection.getLeft(), path);
            Function<Object, Object> element = compile(projection.getRight(), path);
            return current -> WaiterMatchers.project(list.apply(current), element);
        }
        if (expression instanceof FunctionExpression function && "length".equals(function.getName())
            && function.getArguments().size() == 1) {
            Function<Object, Object> value = compile(function.getArguments().get(0), path);
            return current -> WaiterMatchers.length(value.apply(current));
        }
        if (expression instanceof ComparatorExpression comparison) {
            Function<Object, Object> left = compile(comparison.getLeft(), path);
            Function<Object, Object> right = compile(comparison.getRight(), path);
            String comparator = comparison.getComparator().toString();
            return current -> WaiterMatchers.compare(left.apply(current), comparator, right.apply(current));
        }
        if (expression instanceof LiteralExpression literal) {
            Object value = literal(literal, path);
            return current -> value;
        }
        if (expression instanceof AndExpression and) {
            Function<Object, Object> left = compile(and.getLeft(), path);
            Function<Object, Object> right = compile(and.getRight(), path);
            return current -> WaiterMatchers.and(left.apply(current), right.apply(current));
        }
        if (expression instanceof OrExpression or) {
            Function<Object, Object> left = compile(or.getLeft(), path);
            Function<Object, Object> right = compile(or.getRight(), path);
            return current -> WaiterMatchers.or(left.apply(current), right.apply(current));
        }
        if (expression instanceof NotExpression not) {
            Function<Object, Object> value = compile(not.getExpression(), path);
            return current -> WaiterMatchers.not(value.apply(current));
        }
        if (expression instanceof FilterProjectionExpression filter) {
            Function<Object, Object> list = compile(filter.getLeft(), path);
            Function<Object, Object> condition = compile(filter.getComparison(), path);
            Function<Object, Object> element = compile(filter.getRight(), path);
            return current -> WaiterMatchers.project(WaiterMatchers.filter(list.apply(current), condition), element);
        }
        throw new UnsupportedPathException("JMESPath " + expression.getClass().getSimpleName() + " in " + path
                                           + " is outside the supported waiter subset", null);
    }

    private static Object literal(LiteralExpression literal, String path) {
        if (literal.isStringValue()) {
            return literal.expectStringValue();
        }
        if (literal.isBooleanValue()) {
            return literal.expectBooleanValue();
        }
        if (literal.isNumberValue()) {
            Number number = literal.expectNumberValue();
            return number instanceof Double || number instanceof Float ? (Object) number.doubleValue()
                                                                       : (Object) number.longValue();
        }
        if (literal.isNullValue()) {
            return null;
        }
        throw new UnsupportedPathException("unsupported JMESPath literal " + literal + " in " + path, null);
    }

    /**
     * The member {@code name} of a structure, union or map value as a getter value, or {@code null} when the value
     * has no such member or is not an aggregate (null-propagating field access).
     */
    static Object field(Object value, String name) {
        if (value instanceof Map<?, ?> map) {
            return map.get(name);
        }
        if (value instanceof Document document) {
            Document member = switch (document.type()) {
                case STRUCTURE, UNION, MAP -> document.getMember(name);
                default -> null;
            };
            return member == null ? null : value(member);
        }
        return null;
    }

    /** {@code document} as the value a generated getter returns for it. */
    static Object value(Document document) {
        return switch (document.type()) {
            case STRUCTURE, UNION -> document;
            case LIST, SET -> {
                List<Object> list = new ArrayList<>();
                for (Document element : document.asList()) {
                    list.add(element == null ? null : value(element));
                }
                yield list;
            }
            case MAP -> {
                Map<String, Object> map = new LinkedHashMap<>();
                document.asStringMap().forEach((key, entry) -> map.put(key, entry == null ? null : value(entry)));
                yield map;
            }
            case STRING, ENUM -> document.asString();
            case BOOLEAN -> document.asBoolean();
            case INT_ENUM -> document.asInteger();
            case BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL -> document.asNumber();
            case TIMESTAMP -> document.asTimestamp();
            case BLOB -> document.asBlob();
            default -> document.asObject();
        };
    }
}
