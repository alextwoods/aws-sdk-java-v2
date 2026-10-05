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

package software.amazon.awssdk.endpoints.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The path of a {@code smithy.rules#operationContextParams} binding: the JMESPath subset the Smithy rules engine
 * allows there (identifiers, sub-expressions, list indexes, wildcard projections {@code [*]}, flatten projections
 * {@code []}, multi-select lists and the {@code keys} function), parsed once and evaluated over an operation input.
 *
 * <p>Parsing and evaluation follow the JMESPath specification (binding powers and projection semantics of the
 * reference implementation), so {@code Items[*].[A.Name, B.Name][]} is the flattened list of every non-null name.
 * Fields of a structure are read through its schema ({@link SerializableStruct#getMemberValue}) by member name, and
 * fields of a map by key; a field of anything else is {@code null}. Instances are immutable and thread-safe.
 */
public final class OperationContextPath {
    private final String expression;
    private final Node root;

    private OperationContextPath(String expression, Node root) {
        this.expression = expression;
        this.root = root;
    }

    /**
     * Parses {@code expression}.
     *
     * @throws IllegalArgumentException if it is not in the supported JMESPath subset
     */
    public static OperationContextPath parse(String expression) {
        Objects.requireNonNull(expression, "expression");
        Parser parser = new Parser(expression);
        Node root = parser.expression(0);
        if (parser.peek().kind != Kind.EOF) {
            throw parser.error("unexpected " + parser.peek().text);
        }
        return new OperationContextPath(expression, root);
    }

    /** The path expression as modeled. */
    public String expression() {
        return expression;
    }

    /** Evaluates the path over {@code input} (a structure, map, list or scalar); {@code null} if nothing matches. */
    public Object evaluate(Object input) {
        return root.eval(input);
    }

    @Override
    public String toString() {
        return expression;
    }

    private sealed interface Node {
        Object eval(Object value);
    }

    private record Identity() implements Node {
        @Override
        public Object eval(Object value) {
            return value;
        }
    }

    private record Field(String name) implements Node {
        @Override
        public Object eval(Object value) {
            if (value instanceof SerializableStruct struct) {
                Schema member = struct.schema().member(name);
                return member == null ? null : struct.getMemberValue(member);
            }
            if (value instanceof Map<?, ?> map) {
                return map.get(name);
            }
            return null;
        }
    }

    private record Subexpression(Node left, Node right) implements Node {
        @Override
        public Object eval(Object value) {
            return right.eval(left.eval(value));
        }
    }

    private record Index(Node left, int index) implements Node {
        @Override
        public Object eval(Object value) {
            if (!(left.eval(value) instanceof List<?> list)) {
                return null;
            }
            int resolved = index >= 0 ? index : list.size() + index;
            return resolved >= 0 && resolved < list.size() ? list.get(resolved) : null;
        }
    }

    private record Projection(Node left, Node right) implements Node {
        @Override
        public Object eval(Object value) {
            if (!(left.eval(value) instanceof List<?> list)) {
                return null;
            }
            List<Object> result = new ArrayList<>(list.size());
            for (Object element : list) {
                Object projected = right.eval(element);
                if (projected != null) {
                    result.add(projected);
                }
            }
            return result;
        }
    }

    private record Flatten(Node value) implements Node {
        @Override
        public Object eval(Object input) {
            if (!(value.eval(input) instanceof List<?> list)) {
                return null;
            }
            List<Object> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element instanceof List<?> nested) {
                    result.addAll(nested);
                } else {
                    result.add(element);
                }
            }
            return result;
        }
    }

    private record MultiSelectList(List<Node> elements) implements Node {
        @Override
        public Object eval(Object value) {
            if (value == null) {
                return null;
            }
            List<Object> result = new ArrayList<>(elements.size());
            for (Node element : elements) {
                result.add(element.eval(value));
            }
            return result;
        }
    }

    private record Keys(Node argument) implements Node {
        @Override
        public Object eval(Object value) {
            Object target = argument.eval(value);
            if (target instanceof Map<?, ?> map) {
                List<Object> keys = new ArrayList<>(map.size());
                map.keySet().forEach(k -> keys.add(String.valueOf(k)));
                return keys;
            }
            if (target instanceof SerializableStruct struct) {
                List<Object> keys = new ArrayList<>();
                for (Schema member : struct.schema().members()) {
                    if (struct.getMemberValue(member) != null) {
                        keys.add(member.memberName());
                    }
                }
                return keys;
            }
            return null;
        }
    }

    private enum Kind {
        IDENTIFIER(0), DOT(40), STAR(20), LBRACKET(55), RBRACKET(0), FLATTEN(9), COMMA(0), LPAREN(0), RPAREN(0),
        CURRENT(0), NUMBER(0), EOF(0);

        final int bindingPower;

        Kind(int bindingPower) {
            this.bindingPower = bindingPower;
        }
    }

    private record Token(Kind kind, String text, int position) {
    }

    /** A Pratt parser with the binding powers of the JMESPath reference implementation. */
    private static final class Parser {
        /** Tokens with a binding power below this end a projection's right-hand side. */
        private static final int PROJECTION_STOP = 10;

        private final String source;
        private final List<Token> tokens;
        private int index;

        Parser(String source) {
            this.source = source;
            this.tokens = tokenize(source);
        }

        Node expression(int rightBindingPower) {
            Token token = next();
            Node left = nud(token);
            while (rightBindingPower < peek().kind.bindingPower) {
                left = led(next(), left);
            }
            return left;
        }

        private Node nud(Token token) {
            switch (token.kind) {
                case IDENTIFIER -> {
                    if (peek().kind == Kind.LPAREN) {
                        return function(token);
                    }
                    return new Field(token.text);
                }
                case CURRENT -> {
                    return new Identity();
                }
                case FLATTEN -> {
                    return new Projection(new Flatten(new Identity()), projectionRhs(Kind.FLATTEN.bindingPower));
                }
                case LBRACKET -> {
                    if (peek().kind == Kind.STAR && peekAt(1).kind == Kind.RBRACKET) {
                        next();
                        next();
                        return new Projection(new Identity(), projectionRhs(Kind.STAR.bindingPower));
                    }
                    if (peek().kind == Kind.NUMBER) {
                        Node index = new Index(new Identity(), Integer.parseInt(next().text));
                        expect(Kind.RBRACKET);
                        return index;
                    }
                    return multiSelectList();
                }
                default -> throw error("unexpected " + describe(token));
            }
        }

        private Node led(Token token, Node left) {
            switch (token.kind) {
                case DOT -> {
                    return new Subexpression(left, dotRhs(Kind.DOT.bindingPower));
                }
                case FLATTEN -> {
                    return new Projection(new Flatten(left), projectionRhs(Kind.FLATTEN.bindingPower));
                }
                case LBRACKET -> {
                    if (peek().kind == Kind.STAR && peekAt(1).kind == Kind.RBRACKET) {
                        next();
                        next();
                        return new Projection(left, projectionRhs(Kind.STAR.bindingPower));
                    }
                    if (peek().kind == Kind.NUMBER) {
                        Node indexed = new Index(left, Integer.parseInt(next().text));
                        expect(Kind.RBRACKET);
                        return indexed;
                    }
                    throw error("unsupported bracket expression");
                }
                default -> throw error("unexpected " + describe(token));
            }
        }

        private Node projectionRhs(int bindingPower) {
            Kind kind = peek().kind;
            if (kind.bindingPower < PROJECTION_STOP) {
                return new Identity();
            }
            if (kind == Kind.LBRACKET) {
                return expression(bindingPower);
            }
            if (kind == Kind.DOT) {
                next();
                return dotRhs(bindingPower);
            }
            throw error("unexpected " + describe(peek()) + " after a projection");
        }

        private Node dotRhs(int bindingPower) {
            Kind kind = peek().kind;
            if (kind == Kind.IDENTIFIER) {
                return expression(bindingPower);
            }
            if (kind == Kind.LBRACKET) {
                next();
                return multiSelectList();
            }
            throw error("expected an identifier or multi-select list after '.'");
        }

        private Node multiSelectList() {
            List<Node> elements = new ArrayList<>();
            while (true) {
                elements.add(expression(0));
                Token token = next();
                if (token.kind == Kind.RBRACKET) {
                    return new MultiSelectList(List.copyOf(elements));
                }
                if (token.kind != Kind.COMMA) {
                    throw error("expected ',' or ']' in a multi-select list");
                }
            }
        }

        private Node function(Token name) {
            if (!name.text.equals("keys")) {
                throw error("unsupported function " + name.text + " (only keys is allowed)");
            }
            expect(Kind.LPAREN);
            Node argument = expression(0);
            expect(Kind.RPAREN);
            return new Keys(argument);
        }

        private void expect(Kind kind) {
            Token token = next();
            if (token.kind != kind) {
                throw error("expected " + kind + " but found " + describe(token));
            }
        }

        Token peek() {
            return tokens.get(index);
        }

        private Token peekAt(int offset) {
            return tokens.get(Math.min(index + offset, tokens.size() - 1));
        }

        private Token next() {
            Token token = tokens.get(index);
            if (token.kind != Kind.EOF) {
                index++;
            }
            return token;
        }

        private static String describe(Token token) {
            return token.kind == Kind.EOF ? "end of expression" : "'" + token.text + "'";
        }

        IllegalArgumentException error(String problem) {
            return new IllegalArgumentException("Invalid operationContextParams path \"" + source + "\" at position "
                                                + peek().position + ": " + problem + ".");
        }

        private static List<Token> tokenize(String source) {
            List<Token> tokens = new ArrayList<>();
            int i = 0;
            while (i < source.length()) {
                char c = source.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                } else if (Character.isLetter(c) || c == '_') {
                    int start = i;
                    while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i))
                                                   || source.charAt(i) == '_')) {
                        i++;
                    }
                    tokens.add(new Token(Kind.IDENTIFIER, source.substring(start, i), start));
                } else if (c == '"') {
                    int end = source.indexOf('"', i + 1);
                    if (end < 0) {
                        throw new IllegalArgumentException("Invalid operationContextParams path \"" + source
                                                           + "\": unterminated quoted identifier.");
                    }
                    tokens.add(new Token(Kind.IDENTIFIER, source.substring(i + 1, end), i));
                    i = end + 1;
                } else if (c == '-' || Character.isDigit(c)) {
                    int start = i++;
                    while (i < source.length() && Character.isDigit(source.charAt(i))) {
                        i++;
                    }
                    tokens.add(new Token(Kind.NUMBER, source.substring(start, i), start));
                } else if (c == '[' && i + 1 < source.length() && source.charAt(i + 1) == ']') {
                    tokens.add(new Token(Kind.FLATTEN, "[]", i));
                    i += 2;
                } else {
                    Kind kind = switch (c) {
                        case '.' -> Kind.DOT;
                        case '*' -> Kind.STAR;
                        case '[' -> Kind.LBRACKET;
                        case ']' -> Kind.RBRACKET;
                        case ',' -> Kind.COMMA;
                        case '(' -> Kind.LPAREN;
                        case ')' -> Kind.RPAREN;
                        case '@' -> Kind.CURRENT;
                        default -> throw new IllegalArgumentException("Invalid operationContextParams path \""
                                                                      + source + "\": unexpected character '" + c
                                                                      + "' at position " + i + ".");
                    };
                    tokens.add(new Token(kind, String.valueOf(c), i));
                    i++;
                }
            }
            tokens.add(new Token(Kind.EOF, "", source.length()));
            return tokens;
        }
    }
}
