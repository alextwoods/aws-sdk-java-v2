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
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.endpoints.rules.RuleSetParameter;

/**
 * Parses the canonical {@code smithy.rules#endpointRuleSet} document into an immutable rule tree. The document format
 * and the expression forms are those of the Smithy rules engine specification: expressions are {@code {"ref": ...}},
 * {@code {"fn": ..., "argv": [...]}} or literals (strings are templates); endpoint properties are literals, whose
 * objects are records.
 */
public final class RuleSetParser {

    /**
     * A parsed rule set.
     *
     * @param version the rule-set language version
     * @param parameters the parameters in declaration order
     * @param rules the top-level rules in order
     */
    public record ParsedRuleSet(String version, List<RuleSetParameter> parameters, List<Rule> rules) {
        public ParsedRuleSet {
            parameters = List.copyOf(parameters);
            rules = List.copyOf(rules);
        }
    }

    private RuleSetParser() {
    }

    /**
     * Parses the rule-set document {@code root}.
     *
     * @throws IllegalArgumentException if the document is not a valid rule set
     */
    public static ParsedRuleSet parse(Map<String, Object> root) {
        String version = root.get("version") instanceof String v ? v : "";
        List<RuleSetParameter> parameters = new ArrayList<>();
        Map<String, Object> parameterDocs = object(root.getOrDefault("parameters", Map.of()), "parameters");
        parameterDocs.forEach((name, doc) -> parameters.add(parameter(name, object(doc, "parameter " + name))));
        List<Rule> rules = new ArrayList<>();
        for (Object rule : list(root.get("rules"), "rules")) {
            rules.add(rule(rule));
        }
        return new ParsedRuleSet(version, parameters, rules);
    }

    private static RuleSetParameter parameter(String name, Map<String, Object> doc) {
        String typeName = string(doc.get("type"), "type of parameter " + name);
        RuleSetParameter.Type type = switch (typeName.toLowerCase(Locale.ROOT)) {
            case "string" -> RuleSetParameter.Type.STRING;
            case "boolean" -> RuleSetParameter.Type.BOOLEAN;
            case "stringarray" -> RuleSetParameter.Type.STRING_ARRAY;
            default -> throw new IllegalArgumentException("Parameter " + name + " has unsupported type " + typeName);
        };
        Object defaultValue = doc.get("default");
        if (defaultValue != null && !type.accepts(defaultValue)) {
            throw new IllegalArgumentException("The default of parameter " + name + " is not a " + typeName + ": "
                                               + defaultValue);
        }
        return new RuleSetParameter(name, type, doc.get("builtIn") instanceof String b ? b : null, defaultValue,
                                    Boolean.TRUE.equals(doc.get("required")),
                                    doc.get("documentation") instanceof String d ? d : null,
                                    doc.containsKey("deprecated"));
    }

    private static Rule rule(Object node) {
        Map<String, Object> doc = object(node, "rule");
        List<Rule.Condition> conditions = new ArrayList<>();
        for (Object condition : list(doc.getOrDefault("conditions", List.of()), "conditions")) {
            conditions.add(condition(object(condition, "condition")));
        }
        String type = string(doc.get("type"), "rule type");
        return switch (type) {
            case "endpoint" -> new Rule.EndpointRule(conditions, endpoint(object(doc.get("endpoint"), "endpoint")));
            case "error" -> new Rule.ErrorRule(conditions, expression(require(doc.get("error"), "error")));
            case "tree" -> {
                List<Rule> rules = new ArrayList<>();
                for (Object child : list(doc.get("rules"), "tree rules")) {
                    rules.add(rule(child));
                }
                yield new Rule.TreeRule(conditions, rules);
            }
            default -> throw new IllegalArgumentException("Unknown rule type " + type);
        };
    }

    private static Rule.Condition condition(Map<String, Object> doc) {
        Expr function = function(doc);
        Object assign = doc.get("assign");
        return new Rule.Condition(function, assign == null ? null : string(assign, "assign"));
    }

    private static Rule.EndpointTemplate endpoint(Map<String, Object> doc) {
        Expr url = expression(require(doc.get("url"), "endpoint url"));
        Map<String, List<Expr>> headers = new LinkedHashMap<>();
        object(doc.getOrDefault("headers", Map.of()), "headers").forEach((name, values) -> {
            List<Expr> expressions = new ArrayList<>();
            for (Object value : list(values, "values of header " + name)) {
                expressions.add(expression(value));
            }
            headers.put(name, List.copyOf(expressions));
        });
        Map<String, Expr> properties = new LinkedHashMap<>();
        object(doc.getOrDefault("properties", Map.of()), "properties")
            .forEach((name, value) -> properties.put(name, literal(value)));
        return new Rule.EndpointTemplate(url, Collections.unmodifiableMap(headers),
                                         Collections.unmodifiableMap(properties));
    }

    /** An expression: a reference, a function, or a literal. */
    static Expr expression(Object node) {
        if (node instanceof Map<?, ?> map) {
            boolean ref = map.containsKey("ref");
            boolean fn = map.containsKey("fn");
            if (ref == fn) {
                throw new IllegalArgumentException("Expected exactly one of `ref` or `fn` in " + node);
            }
            if (ref) {
                return new Expr.Ref(string(map.get("ref"), "ref"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) map;
            return function(function);
        }
        return literal(node);
    }

    /** A literal: strings are templates, arrays tuples and objects records. */
    static Expr literal(Object node) {
        if (node instanceof String s) {
            return template(s);
        }
        if (node instanceof Boolean || node instanceof Integer) {
            return new Expr.Literal(node);
        }
        if (node instanceof List<?> list) {
            List<Expr> elements = new ArrayList<>(list.size());
            for (Object element : list) {
                elements.add(literal(element));
            }
            return new Expr.TupleLiteral(List.copyOf(elements));
        }
        if (node instanceof Map<?, ?> map) {
            Map<String, Expr> members = new LinkedHashMap<>();
            map.forEach((key, value) -> members.put((String) key, literal(value)));
            return new Expr.RecordLiteral(Collections.unmodifiableMap(members));
        }
        throw new IllegalArgumentException("Unsupported literal " + node);
    }

    private static Expr function(Map<String, Object> doc) {
        String name = string(doc.get("fn"), "fn");
        List<Object> argv = new ArrayList<>(list(doc.getOrDefault("argv", List.of()), "argv of " + name));
        switch (name) {
            case "isSet" -> {
                arity(name, argv, 1);
                return new Expr.IsSet(expression(argv.get(0)));
            }
            case "not" -> {
                arity(name, argv, 1);
                return new Expr.Not(expression(argv.get(0)));
            }
            case "booleanEquals" -> {
                arity(name, argv, 2);
                return new Expr.BooleanEquals(expression(argv.get(0)), expression(argv.get(1)));
            }
            case "stringEquals" -> {
                arity(name, argv, 2);
                return new Expr.StringEquals(expression(argv.get(0)), expression(argv.get(1)));
            }
            case "getAttr" -> {
                arity(name, argv, 2);
                return new Expr.GetAttr(expression(argv.get(0)), path(string(argv.get(1), "getAttr path")));
            }
            case "coalesce" -> {
                if (argv.size() < 2) {
                    throw new IllegalArgumentException("coalesce expects at least 2 arguments");
                }
                return new Expr.Coalesce(argv.stream().map(RuleSetParser::expression).toList());
            }
            case "ite" -> {
                arity(name, argv, 3);
                return new Expr.Ite(expression(argv.get(0)), expression(argv.get(1)), expression(argv.get(2)));
            }
            default -> {
                LibraryFunction function = LibraryFunction.fromId(name).orElseThrow(
                    () -> new IllegalArgumentException("Unsupported endpoint rules function " + name));
                arity(name, argv, function.arity());
                return new Expr.Call(function, argv.stream().map(RuleSetParser::expression).toList());
            }
        }
    }

    /** A {@code getAttr} path such as {@code a.b[2]} or {@code resourceId[-1]}: keys and integer indexes. */
    static List<Object> path(String path) {
        List<Object> parts = new ArrayList<>();
        for (String component : path.split("\\.")) {
            int bracket = component.indexOf('[');
            if (bracket < 0) {
                parts.add(component);
                continue;
            }
            if (!component.endsWith("]")) {
                throw new IllegalArgumentException("Invalid getAttr path component " + component);
            }
            if (bracket > 0) {
                parts.add(component.substring(0, bracket));
            }
            try {
                parts.add(Integer.parseInt(component.substring(bracket + 1, component.length() - 1)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid getAttr index in " + component, e);
            }
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("A getAttr path may not be empty");
        }
        return parts;
    }

    /**
     * A string template: {@code {Name}} and {@code {Name#path}} are dynamic parts, and doubled braces are escaped
     * braces. A template without dynamic parts is a string literal.
     */
    static Expr template(String template) {
        List<Object> parts = new ArrayList<>();
        int start = -1;
        int depth = 0;
        int end = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.startsWith("{{", i) || template.startsWith("}}", i)) {
                i++;
                continue;
            }
            char c = template.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    if (end != i) {
                        parts.add(unescape(template.substring(end, i)));
                    }
                    start = i + 1;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("Unmatched `}` in template " + template);
                }
                if (depth == 0) {
                    parts.add(shortForm(template.substring(start, i)));
                }
                end = i + 1;
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("Unmatched `{` in template " + template);
        }
        if (end < template.length()) {
            parts.add(unescape(template.substring(end)));
        }
        if (parts.stream().allMatch(String.class::isInstance)) {
            // A static template's value is its source text, exactly as the canonical evaluator returns it.
            return new Expr.Literal(template);
        }
        return new Expr.Template(template, parts);
    }

    private static Expr shortForm(String shortForm) {
        int hash = shortForm.indexOf('#');
        if (hash < 0) {
            return new Expr.Ref(shortForm);
        }
        return new Expr.GetAttr(new Expr.Ref(shortForm.substring(0, hash)), path(shortForm.substring(hash + 1)));
    }

    private static String unescape(String literal) {
        return literal.replace("{{", "{").replace("}}", "}");
    }

    private static void arity(String name, List<Object> argv, int expected) {
        if (argv.size() != expected) {
            throw new IllegalArgumentException(name + " expects " + expected + " arguments but has " + argv.size());
        }
    }

    private static Object require(Object value, String what) {
        if (value == null) {
            throw new IllegalArgumentException("Missing " + what);
        }
        return value;
    }

    private static List<?> list(Object value, String what) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Invalid rule set: " + what + " must be an array");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String what) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Invalid rule set: " + what + " must be an object");
    }

    private static String string(Object value, String what) {
        if (value instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException("Invalid rule set: " + what + " must be a string");
    }
}
