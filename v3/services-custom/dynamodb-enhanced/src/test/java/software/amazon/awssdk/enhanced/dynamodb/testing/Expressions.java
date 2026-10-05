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

package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An evaluator for the DynamoDB expression language subset the enhanced client emits: condition, filter and key
 * condition expressions (comparisons, {@code BETWEEN}, {@code IN}, {@code AND}/{@code OR}/{@code NOT}, parentheses,
 * {@code attribute_exists}, {@code attribute_not_exists}, {@code attribute_type}, {@code begins_with},
 * {@code contains}, {@code size}), update expressions ({@code SET} with {@code +}, {@code -}, {@code if_not_exists}
 * and {@code list_append}; {@code REMOVE}; {@code ADD}; {@code DELETE}) and projection expressions, with
 * {@code #name} and {@code :value} placeholders. Invalid expressions or unused/undefined placeholders raise
 * {@link DynamoDbError} {@code ValidationException}, as the service does.
 */
public final class Expressions {
    private final List<Token> tokens;
    private final Map<String, String> names;
    private final Map<String, AttributeValue> values;
    private int pos;

    private Expressions(String expression, Map<String, String> names, Map<String, AttributeValue> values) {
        this.tokens = tokenize(expression);
        this.names = names == null ? Map.of() : names;
        this.values = values == null ? Map.of() : values;
    }

    // ---- public entry points ---------------------------------------------------------------------------------------

    public static Predicate<Map<String, AttributeValue>> condition(String expression, Map<String, String> names,
                                                                    Map<String, AttributeValue> values) {
        Expressions p = new Expressions(expression, names, values);
        Predicate<Map<String, AttributeValue>> result = p.orExpr();
        p.expectEnd();
        return result;
    }

    /** Applies {@code expression} to {@code item} in place. */
    public static void update(String expression, Map<String, String> names, Map<String, AttributeValue> values,
                              Map<String, AttributeValue> item) {
        Expressions p = new Expressions(expression, names, values);
        p.updateExpression(item);
    }

    /** The attributes of {@code item} named by the projection expression. */
    public static Map<String, AttributeValue> project(String expression, Map<String, String> names,
                                                      Map<String, AttributeValue> item) {
        Expressions p = new Expressions(expression, names, Map.of());
        Map<String, AttributeValue> result = new LinkedHashMap<>();
        do {
            List<Object> path = p.path();
            AttributeValue value = get(item, path);
            if (value != null) {
                put(result, path, value, true);
            }
        } while (p.accept(","));
        p.expectEnd();
        return result;
    }

    /** Top-level attribute names referenced by a projection expression. */
    public static Set<String> topLevelNames(String expression, Map<String, String> names) {
        Expressions p = new Expressions(expression, names, Map.of());
        Set<String> result = new LinkedHashSet<>();
        do {
            result.add((String) p.path().get(0));
        } while (p.accept(","));
        return result;
    }

    // ---- conditions ------------------------------------------------------------------------------------------------

    private Predicate<Map<String, AttributeValue>> orExpr() {
        Predicate<Map<String, AttributeValue>> left = andExpr();
        while (acceptKeyword("OR")) {
            Predicate<Map<String, AttributeValue>> l = left;
            Predicate<Map<String, AttributeValue>> r = andExpr();
            left = item -> l.test(item) || r.test(item);
        }
        return left;
    }

    private Predicate<Map<String, AttributeValue>> andExpr() {
        Predicate<Map<String, AttributeValue>> left = notExpr();
        while (acceptKeyword("AND")) {
            Predicate<Map<String, AttributeValue>> l = left;
            Predicate<Map<String, AttributeValue>> r = notExpr();
            left = item -> l.test(item) && r.test(item);
        }
        return left;
    }

    private Predicate<Map<String, AttributeValue>> notExpr() {
        if (acceptKeyword("NOT")) {
            Predicate<Map<String, AttributeValue>> inner = notExpr();
            return item -> !inner.test(item);
        }
        return primary();
    }

    private Predicate<Map<String, AttributeValue>> primary() {
        if (accept("(")) {
            Predicate<Map<String, AttributeValue>> inner = orExpr();
            expect(")");
            return inner;
        }
        Token t = peek();
        if (t.kind == Kind.IDENT && peekIs(1, "(")) {
            String fn = t.text.toLowerCase(Locale.ROOT);
            switch (fn) {
                case "attribute_exists", "attribute_not_exists" -> {
                    pos += 2;
                    List<Object> path = path();
                    expect(")");
                    boolean exists = fn.equals("attribute_exists");
                    return item -> (get(item, path) != null) == exists;
                }
                case "attribute_type" -> {
                    pos += 2;
                    List<Object> path = path();
                    expect(",");
                    AttributeValue type = valueRef();
                    expect(")");
                    return item -> {
                        AttributeValue v = get(item, path);
                        return v != null && v.type().name().equals(type.s());
                    };
                }
                case "begins_with" -> {
                    pos += 2;
                    Function<Map<String, AttributeValue>, AttributeValue> a = operand();
                    expect(",");
                    Function<Map<String, AttributeValue>, AttributeValue> b = operand();
                    expect(")");
                    return item -> beginsWith(a.apply(item), b.apply(item));
                }
                case "contains" -> {
                    pos += 2;
                    Function<Map<String, AttributeValue>, AttributeValue> a = operand();
                    expect(",");
                    Function<Map<String, AttributeValue>, AttributeValue> b = operand();
                    expect(")");
                    return item -> contains(a.apply(item), b.apply(item));
                }
                default -> {
                    // size(...) is an operand; fall through to a comparison.
                }
            }
        }
        Function<Map<String, AttributeValue>, AttributeValue> left = operand();
        if (acceptKeyword("BETWEEN")) {
            Function<Map<String, AttributeValue>, AttributeValue> low = operand();
            expectKeyword("AND");
            Function<Map<String, AttributeValue>, AttributeValue> high = operand();
            return item -> {
                AttributeValue v = left.apply(item);
                Integer lo = Av.compare(v, low.apply(item));
                Integer hi = Av.compare(v, high.apply(item));
                return lo != null && hi != null && lo >= 0 && hi <= 0;
            };
        }
        if (acceptKeyword("IN")) {
            expect("(");
            List<Function<Map<String, AttributeValue>, AttributeValue>> candidates = new ArrayList<>();
            do {
                candidates.add(operand());
            } while (accept(","));
            expect(")");
            return item -> {
                AttributeValue v = left.apply(item);
                return candidates.stream().anyMatch(c -> Av.same(v, c.apply(item)));
            };
        }
        Token op = next();
        if (op.kind != Kind.SYMBOL) {
            throw invalid("Expected a comparator, found '" + op.text + "'");
        }
        Function<Map<String, AttributeValue>, AttributeValue> right = operand();
        return switch (op.text) {
            case "=" -> item -> Av.same(left.apply(item), right.apply(item));
            case "<>" -> item -> !Av.same(left.apply(item), right.apply(item));
            case "<" -> item -> sign(left.apply(item), right.apply(item), c -> c < 0);
            case "<=" -> item -> sign(left.apply(item), right.apply(item), c -> c <= 0);
            case ">" -> item -> sign(left.apply(item), right.apply(item), c -> c > 0);
            case ">=" -> item -> sign(left.apply(item), right.apply(item), c -> c >= 0);
            default -> throw invalid("Unknown comparator '" + op.text + "'");
        };
    }

    private static boolean sign(AttributeValue a, AttributeValue b, Predicate<Integer> test) {
        Integer c = Av.compare(a, b);
        return c != null && test.test(c);
    }

    private static boolean beginsWith(AttributeValue a, AttributeValue prefix) {
        if (a == null || prefix == null || a.type() != prefix.type()) {
            return false;
        }
        return switch (a.type()) {
            case S -> a.s().startsWith(prefix.s());
            case B -> {
                byte[] v = a.b().asByteArray();
                byte[] p = prefix.b().asByteArray();
                yield v.length >= p.length && java.util.Arrays.equals(v, 0, p.length, p, 0, p.length);
            }
            default -> false;
        };
    }

    private static boolean contains(AttributeValue a, AttributeValue element) {
        if (a == null || element == null) {
            return false;
        }
        return switch (a.type()) {
            case S -> element.type() == AttributeValue.Type.S && a.s().contains(element.s());
            case SS -> element.type() == AttributeValue.Type.S && a.ss().contains(element.s());
            case NS -> element.type() == AttributeValue.Type.N
                       && a.ns().stream().anyMatch(n -> Av.same(AttributeValue.fromN(n), element));
            case BS -> element.type() == AttributeValue.Type.B && a.bs().contains(element.b());
            case L -> a.l().stream().anyMatch(e -> Av.same(e, element));
            default -> false;
        };
    }

    private Function<Map<String, AttributeValue>, AttributeValue> operand() {
        Token t = peek();
        if (t.kind == Kind.VALUE) {
            AttributeValue v = valueRef();
            return item -> v;
        }
        if (t.kind == Kind.IDENT && t.text.equalsIgnoreCase("size") && peekIs(1, "(")) {
            pos += 2;
            List<Object> path = path();
            expect(")");
            return item -> size(get(item, path));
        }
        List<Object> path = path();
        return item -> get(item, path);
    }

    private static AttributeValue size(AttributeValue v) {
        if (v == null) {
            return null;
        }
        int size = switch (v.type()) {
            case S -> v.s().length();
            case B -> v.b().asByteArray().length;
            case SS -> v.ss().size();
            case NS -> v.ns().size();
            case BS -> v.bs().size();
            case L -> v.l().size();
            case M -> v.m().size();
            default -> -1;
        };
        return size < 0 ? null : AttributeValue.fromN(Integer.toString(size));
    }

    // ---- updates ---------------------------------------------------------------------------------------------------

    private void updateExpression(Map<String, AttributeValue> item) {
        Map<String, AttributeValue> before = Av.copy(item);
        List<Runnable> actions = new ArrayList<>();
        Set<String> clauses = new LinkedHashSet<>();
        while (!atEnd()) {
            Token clause = next();
            String keyword = clause.text.toUpperCase(Locale.ROOT);
            if (clause.kind != Kind.IDENT || !clauses.add(keyword)) {
                throw invalid("Invalid UpdateExpression clause '" + clause.text + "'");
            }
            do {
                switch (keyword) {
                    case "SET" -> {
                        List<Object> path = path();
                        expect("=");
                        Function<Map<String, AttributeValue>, AttributeValue> value = setValue();
                        AttributeValue computed = value.apply(before);
                        actions.add(() -> put(item, path, computed, false));
                    }
                    case "REMOVE" -> {
                        List<Object> path = path();
                        actions.add(() -> remove(item, path));
                    }
                    case "ADD" -> {
                        List<Object> path = path();
                        AttributeValue delta = valueRef();
                        AttributeValue current = get(before, path);
                        actions.add(() -> put(item, path, add(current, delta), false));
                    }
                    case "DELETE" -> {
                        List<Object> path = path();
                        AttributeValue subset = valueRef();
                        AttributeValue current = get(before, path);
                        actions.add(() -> {
                            AttributeValue remaining = deleteFromSet(current, subset);
                            if (remaining == null) {
                                remove(item, path);
                            } else {
                                put(item, path, remaining, false);
                            }
                        });
                    }
                    default -> throw invalid("Invalid UpdateExpression clause '" + clause.text + "'");
                }
            } while (accept(","));
        }
        if (actions.isEmpty()) {
            throw invalid("Invalid UpdateExpression: The expression can not be empty");
        }
        actions.forEach(Runnable::run);
    }

    private Function<Map<String, AttributeValue>, AttributeValue> setValue() {
        Function<Map<String, AttributeValue>, AttributeValue> left = setTerm();
        if (accept("+")) {
            Function<Map<String, AttributeValue>, AttributeValue> right = setTerm();
            return item -> arithmetic(left.apply(item), right.apply(item), true);
        }
        if (accept("-")) {
            Function<Map<String, AttributeValue>, AttributeValue> right = setTerm();
            return item -> arithmetic(left.apply(item), right.apply(item), false);
        }
        return item -> {
            AttributeValue v = left.apply(item);
            if (v == null) {
                throw invalid("The provided expression refers to an attribute that does not exist in the item");
            }
            return v;
        };
    }

    private Function<Map<String, AttributeValue>, AttributeValue> setTerm() {
        Token t = peek();
        if (t.kind == Kind.IDENT && peekIs(1, "(")) {
            String fn = t.text.toLowerCase(Locale.ROOT);
            if (fn.equals("if_not_exists")) {
                pos += 2;
                List<Object> path = path();
                expect(",");
                Function<Map<String, AttributeValue>, AttributeValue> fallback = setTerm();
                expect(")");
                return item -> {
                    AttributeValue v = get(item, path);
                    return v != null ? v : fallback.apply(item);
                };
            }
            if (fn.equals("list_append")) {
                pos += 2;
                Function<Map<String, AttributeValue>, AttributeValue> a = setTerm();
                expect(",");
                Function<Map<String, AttributeValue>, AttributeValue> b = setTerm();
                expect(")");
                return item -> {
                    AttributeValue x = a.apply(item);
                    AttributeValue y = b.apply(item);
                    if (x == null || y == null || x.type() != AttributeValue.Type.L
                        || y.type() != AttributeValue.Type.L) {
                        throw invalid("Incorrect operand type for operator or function; operator or function: "
                                      + "list_append");
                    }
                    List<AttributeValue> joined = new ArrayList<>(x.l());
                    joined.addAll(y.l());
                    return AttributeValue.fromL(joined);
                };
            }
        }
        return operand();
    }

    private static AttributeValue arithmetic(AttributeValue a, AttributeValue b, boolean plus) {
        if (a == null || b == null || a.type() != AttributeValue.Type.N || b.type() != AttributeValue.Type.N) {
            throw invalid("An operand in the update expression has an incorrect data type");
        }
        BigDecimal x = new BigDecimal(a.n());
        BigDecimal y = new BigDecimal(b.n());
        return AttributeValue.fromN((plus ? x.add(y) : x.subtract(y)).toPlainString());
    }

    private static AttributeValue add(AttributeValue current, AttributeValue delta) {
        if (delta.type() == AttributeValue.Type.N) {
            return arithmetic(current == null ? AttributeValue.fromN("0") : current, delta, true);
        }
        Set<Object> union = new LinkedHashSet<>();
        if (current != null) {
            union.addAll(setElements(current));
        }
        union.addAll(setElements(delta));
        return rebuildSet(delta.type(), union);
    }

    private static AttributeValue deleteFromSet(AttributeValue current, AttributeValue subset) {
        if (current == null) {
            return null;
        }
        Set<Object> remaining = new LinkedHashSet<>(setElements(current));
        remaining.removeAll(setElements(subset));
        return remaining.isEmpty() ? null : rebuildSet(current.type(), remaining);
    }

    private static List<?> setElements(AttributeValue v) {
        return switch (v.type()) {
            case SS -> v.ss();
            case NS -> v.ns();
            case BS -> v.bs();
            default -> throw invalid("An operand in the update expression has an incorrect data type");
        };
    }

    @SuppressWarnings("unchecked")
    private static AttributeValue rebuildSet(AttributeValue.Type type, Set<Object> elements) {
        List<Object> list = new ArrayList<>(elements);
        return switch (type) {
            case SS -> AttributeValue.fromSs((List<String>) (List<?>) list);
            case NS -> AttributeValue.fromNs((List<String>) (List<?>) list);
            case BS -> AttributeValue.fromBs((List<software.amazon.awssdk.core.SdkBytes>) (List<?>) list);
            default -> throw invalid("An operand in the update expression has an incorrect data type");
        };
    }

    // ---- document paths --------------------------------------------------------------------------------------------

    private List<Object> path() {
        List<Object> path = new ArrayList<>();
        path.add(name());
        while (true) {
            if (accept(".")) {
                path.add(name());
            } else if (accept("[")) {
                Token index = next();
                if (index.kind != Kind.NUMBER) {
                    throw invalid("Invalid list index '" + index.text + "'");
                }
                path.add(Integer.parseInt(index.text));
                expect("]");
            } else {
                return path;
            }
        }
    }

    private String name() {
        Token t = next();
        if (t.kind == Kind.NAME) {
            String resolved = names.get(t.text);
            if (resolved == null) {
                throw invalid("An expression attribute name used in the document path is not defined; attribute "
                              + "name: " + t.text);
            }
            return resolved;
        }
        if (t.kind == Kind.IDENT) {
            return t.text;
        }
        throw invalid("Expected an attribute name, found '" + t.text + "'");
    }

    private AttributeValue valueRef() {
        Token t = next();
        if (t.kind != Kind.VALUE) {
            throw invalid("Expected an expression attribute value, found '" + t.text + "'");
        }
        AttributeValue v = values.get(t.text);
        if (v == null) {
            throw invalid("An expression attribute value used in expression is not defined; attribute value: "
                          + t.text);
        }
        return v;
    }

    static AttributeValue get(Map<String, AttributeValue> item, List<Object> path) {
        AttributeValue current = item.get((String) path.get(0));
        for (int i = 1; i < path.size() && current != null; i++) {
            Object segment = path.get(i);
            if (segment instanceof String key) {
                current = current.type() == AttributeValue.Type.M ? current.m().get(key) : null;
            } else {
                int index = (Integer) segment;
                current = current.type() == AttributeValue.Type.L && index < current.l().size()
                          ? current.l().get(index) : null;
            }
        }
        return current;
    }

    /** Sets {@code path}; parents must exist unless {@code createParents} (used to build projections). */
    static void put(Map<String, AttributeValue> item, List<Object> path, AttributeValue value, boolean createParents) {
        if (path.size() == 1) {
            item.put((String) path.get(0), value);
            return;
        }
        String first = (String) path.get(0);
        AttributeValue parent = item.get(first);
        if (parent == null) {
            if (!createParents) {
                throw invalid("The document path provided in the update expression is invalid for update");
            }
            parent = path.get(1) instanceof String ? AttributeValue.fromM(Map.of()) : AttributeValue.fromL(List.of());
        }
        item.put(first, withChild(parent, path.subList(1, path.size()), value, createParents));
    }

    private static AttributeValue withChild(AttributeValue parent, List<Object> rest, AttributeValue value,
                                            boolean createParents) {
        Object segment = rest.get(0);
        if (segment instanceof String key) {
            if (parent.type() != AttributeValue.Type.M) {
                throw invalid("The document path provided in the update expression is invalid for update");
            }
            Map<String, AttributeValue> copy = new LinkedHashMap<>(parent.m());
            if (rest.size() == 1) {
                copy.put(key, value);
            } else {
                AttributeValue child = copy.get(key);
                if (child == null) {
                    if (!createParents) {
                        throw invalid("The document path provided in the update expression is invalid for update");
                    }
                    child = rest.get(1) instanceof String ? AttributeValue.fromM(Map.of())
                                                          : AttributeValue.fromL(List.of());
                }
                copy.put(key, withChild(child, rest.subList(1, rest.size()), value, createParents));
            }
            return AttributeValue.fromM(copy);
        }
        int index = (Integer) segment;
        if (parent.type() != AttributeValue.Type.L) {
            throw invalid("The document path provided in the update expression is invalid for update");
        }
        List<AttributeValue> copy = new ArrayList<>(parent.l());
        if (rest.size() == 1) {
            if (index >= copy.size()) {
                copy.add(value);
            } else {
                copy.set(index, value);
            }
        } else {
            if (index >= copy.size()) {
                throw invalid("The document path provided in the update expression is invalid for update");
            }
            copy.set(index, withChild(copy.get(index), rest.subList(1, rest.size()), value, createParents));
        }
        return AttributeValue.fromL(copy);
    }

    static void remove(Map<String, AttributeValue> item, List<Object> path) {
        if (path.size() == 1) {
            item.remove((String) path.get(0));
            return;
        }
        String first = (String) path.get(0);
        AttributeValue parent = item.get(first);
        if (parent != null) {
            AttributeValue updated = without(parent, path.subList(1, path.size()));
            if (updated != null) {
                item.put(first, updated);
            }
        }
    }

    private static AttributeValue without(AttributeValue parent, List<Object> rest) {
        Object segment = rest.get(0);
        if (segment instanceof String key && parent.type() == AttributeValue.Type.M) {
            Map<String, AttributeValue> copy = new LinkedHashMap<>(parent.m());
            if (rest.size() == 1) {
                copy.remove(key);
            } else if (copy.containsKey(key)) {
                AttributeValue child = without(copy.get(key), rest.subList(1, rest.size()));
                if (child != null) {
                    copy.put(key, child);
                }
            }
            return AttributeValue.fromM(copy);
        }
        if (segment instanceof Integer index && parent.type() == AttributeValue.Type.L && index < parent.l().size()) {
            List<AttributeValue> copy = new ArrayList<>(parent.l());
            if (rest.size() == 1) {
                copy.remove((int) index);
            } else {
                AttributeValue child = without(copy.get(index), rest.subList(1, rest.size()));
                if (child != null) {
                    copy.set(index, child);
                }
            }
            return AttributeValue.fromL(copy);
        }
        return null;
    }

    // ---- tokens ----------------------------------------------------------------------------------------------------

    private enum Kind { IDENT, NAME, VALUE, NUMBER, SYMBOL, END }

    private record Token(Kind kind, String text) {
    }

    private static List<Token> tokenize(String s) {
        if (s == null || s.isBlank()) {
            throw invalid("Invalid expression: The expression can not be empty;");
        }
        List<Token> result = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '#' || c == ':') {
                int start = i++;
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
                    i++;
                }
                if (i == start + 1) {
                    throw invalid("Invalid expression: Syntax error; token: \"" + c + "\"");
                }
                result.add(new Token(c == '#' ? Kind.NAME : Kind.VALUE, s.substring(start, i)));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
                    i++;
                }
                result.add(new Token(Kind.IDENT, s.substring(start, i)));
            } else if (Character.isDigit(c)) {
                int start = i;
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
                result.add(new Token(Kind.NUMBER, s.substring(start, i)));
            } else if (s.startsWith("<>", i) || s.startsWith("<=", i) || s.startsWith(">=", i)) {
                result.add(new Token(Kind.SYMBOL, s.substring(i, i + 2)));
                i += 2;
            } else if ("()[],.=<>+-".indexOf(c) >= 0) {
                result.add(new Token(Kind.SYMBOL, String.valueOf(c)));
                i++;
            } else {
                throw invalid("Invalid expression: Syntax error; token: \"" + c + "\"");
            }
        }
        result.add(new Token(Kind.END, ""));
        return result;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private boolean peekIs(int offset, String symbol) {
        int index = pos + offset;
        return index < tokens.size() && tokens.get(index).kind == Kind.SYMBOL && tokens.get(index).text.equals(symbol);
    }

    private Token next() {
        Token t = tokens.get(pos);
        if (t.kind != Kind.END) {
            pos++;
        }
        return t;
    }

    private boolean atEnd() {
        return peek().kind == Kind.END;
    }

    private boolean accept(String symbol) {
        if (peekIs(0, symbol)) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(String symbol) {
        if (!accept(symbol)) {
            throw invalid("Invalid expression: Syntax error; expected '" + symbol + "', found '" + peek().text + "'");
        }
    }

    private boolean acceptKeyword(String keyword) {
        Token t = peek();
        if (t.kind == Kind.IDENT && t.text.equalsIgnoreCase(keyword)) {
            pos++;
            return true;
        }
        return false;
    }

    private void expectKeyword(String keyword) {
        if (!acceptKeyword(keyword)) {
            throw invalid("Invalid expression: Syntax error; expected " + keyword);
        }
    }

    private void expectEnd() {
        if (!atEnd()) {
            throw invalid("Invalid expression: Syntax error; token: \"" + peek().text + "\"");
        }
    }

    private static DynamoDbError invalid(String message) {
        return DynamoDbError.validation(message);
    }
}
