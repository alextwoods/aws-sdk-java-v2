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

package software.amazon.awssdk.awscodegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.waiters.Acceptor;
import software.amazon.smithy.waiters.Matcher;
import software.amazon.smithy.waiters.PathMatcher;
import software.amazon.smithy.waiters.WaitableTrait;
import software.amazon.smithy.waiters.Waiter;

/**
 * Render plan of a service's waiters (Requirements 7.9, 8.7): the public {@code <Service>Waiter} and
 * {@code <Service>AsyncWaiter} interfaces and their package-private default implementations in the service's
 * {@code waiters} package, with one {@code waitUntil<Name>} family per {@code smithy.waiters#waitable} waiter.
 *
 * <p>Acceptor paths are compiled here from the canonical JMESPath to a small typed expression tree over the
 * generated getters; the supported subset is field access, sub-expressions, list, filter and map-value projections,
 * flattening, multi-select lists, {@code length}, {@code contains}, comparisons, {@code &&}, {@code ||}, {@code !},
 * literals and the current node. A path outside the subset fails generation.
 *
 * @param syncClient the sync client interface
 * @param asyncClient the async client interface, or {@code null}
 * @param syncWaiter the sync waiter interface
 * @param defaultSyncWaiter the sync waiter implementation
 * @param asyncWaiter the async waiter interface, or {@code null}
 * @param defaultAsyncWaiter the async waiter implementation, or {@code null}
 * @param waiters one entry per waiter, sorted by operation shape ID then waiter name
 */
record WaiterPlan(Symbol syncClient, Symbol asyncClient, Symbol syncWaiter, Symbol defaultSyncWaiter,
                  Symbol asyncWaiter, Symbol defaultAsyncWaiter, List<WaiterMethod> waiters) {
    /** Shape ID of the synthetic list a list projection yields while resolving the shape of a waiter path. */
    private static final ShapeId PROJECTED_LIST = ShapeId.from("smithy.synthetic#ProjectedList");


    WaiterPlan {
        waiters = List.copyOf(waiters);
    }

    /**
     * One modeled waiter.
     *
     * @param name the waiter name
     * @param method the {@code waitUntil<Name>} method
     * @param operationName the polled operation's name
     * @param operationMethod the client's operation method
     * @param input the input type
     * @param output the output type
     * @param minDelaySeconds the modeled minimum delay
     * @param maxDelaySeconds the modeled maximum delay
     * @param acceptors the acceptors, in model order
     */
    record WaiterMethod(String name, String method, String operationName, String operationMethod, Symbol input,
                        Symbol output, int minDelaySeconds, int maxDelaySeconds, List<AcceptorPlan> acceptors) {
        WaiterMethod {
            acceptors = List.copyOf(acceptors);
        }
    }

    /**
     * One acceptor.
     *
     * @param state {@code SUCCESS}, {@code FAILURE} or {@code RETRY}
     * @param onException whether the acceptor matches failed polls
     * @param path the matched path, or {@code null} for {@code success} and {@code errorType} matchers
     * @param comparator the path comparator ({@code stringEquals}, ...), or {@code null}
     * @param expected the expected value, or {@code null}
     * @param success the {@code success} matcher's value, or {@code null}
     * @param errorType the {@code errorType} matcher's value, or {@code null}
     * @param errorClass the modeled exception of {@code errorType}, or {@code null}
     */
    record AcceptorPlan(String state, boolean onException, Expr path, String comparator, String expected,
                        Boolean success, String errorType, Symbol errorClass) {
    }

    /** A compiled acceptor path. */
    sealed interface Expr permits Root, Field, Project, Flatten, Length, Compare, Literal, Logic, Filter, Values,
                                  Contains, MultiSelect {
    }

    /**
     * The root value: {@code response}, or for {@code inputOutput} paths, {@code request} or {@code response}.
     *
     * @param variable the Java variable
     */
    record Root(String variable) implements Expr {
    }

    /**
     * A getter of a structure value.
     *
     * @param base the structure value
     * @param type the structure type
     * @param getter the getter
     */
    record Field(Expr base, Symbol type, String getter) implements Expr {
    }

    /**
     * A list projection.
     *
     * @param list the projected list
     * @param element the projection of each element, rooted at {@link Root} {@code element}
     */
    record Project(Expr list, Expr element) implements Expr {
    }

    /**
     * A flatten.
     *
     * @param list the flattened list
     */
    record Flatten(Expr list) implements Expr {
    }

    /**
     * {@code length}.
     *
     * @param value the measured value
     */
    record Length(Expr value) implements Expr {
    }

    /**
     * A comparison.
     *
     * @param left the left value
     * @param comparator the JMESPath comparator
     * @param right the right value
     */
    record Compare(Expr left, String comparator, Expr right) implements Expr {
    }

    /**
     * A literal.
     *
     * @param value a {@code String}, {@code Boolean}, {@code Long} or {@code Double}, or {@code null}
     */
    record Literal(Object value) implements Expr {
    }

    /**
     * A JMESPath {@code &&} ({@code and}), {@code ||} ({@code or}) or {@code !} ({@code not}, {@code right} null).
     *
     * @param operator {@code and}, {@code or} or {@code not}
     * @param left the left (or only) operand
     * @param right the right operand, or {@code null}
     */
    record Logic(String operator, Expr left, Expr right) implements Expr {
    }

    /**
     * A filter projection: the elements of {@code list} whose {@code condition} is truthy, then projected.
     *
     * @param list the filtered list
     * @param condition the condition, rooted at {@link Root} {@code element}
     * @param element the projection of each kept element, rooted at {@link Root} {@code element}
     */
    record Filter(Expr list, Expr condition, Expr element) implements Expr {
    }

    /**
     * The values of a modeled map, as a list: the left side of an object projection ({@code a.*.b}).
     *
     * @param map the map value
     */
    record Values(Expr map) implements Expr {
    }

    /**
     * JMESPath {@code contains(subject, search)}.
     *
     * @param subject the searched list or string
     * @param search the searched-for value
     */
    record Contains(Expr subject, Expr search) implements Expr {
    }

    /**
     * A JMESPath multi-select list ({@code [a, b]}): each item evaluated against {@code base}, or {@code null} when
     * {@code base} is {@code null}.
     *
     * @param base the current value
     * @param items the selected items, rooted at {@link Root} {@code element}
     */
    record MultiSelect(Expr base, List<Expr> items) implements Expr {
        MultiSelect {
            items = List.copyOf(items);
        }
    }

    static WaiterPlan of(AwsCodegenContext context, ClientPlan client) {
        Model model = context.model();
        List<WaiterMethod> waiters = new ArrayList<>();
        for (OperationShape operation : CodegenOrdering.byShapeId(
            TopDownIndex.of(model).getContainedOperations(context.service()))) {
            if (!waitable(operation)) {
                continue;
            }
            Map<String, Waiter> modeled = new TreeMap<>(operation.expectTrait(WaitableTrait.class).getWaiters());
            for (Map.Entry<String, Waiter> waiter : modeled.entrySet()) {
                waiters.add(waiter(context, operation, waiter.getKey(), waiter.getValue()));
            }
        }
        String pkg = context.settings().packageName() + ".waiters";
        String service = AwsSymbolProvider.serviceTypeName(context.service());
        boolean async = client.asyncClient() != null;
        return new WaiterPlan(client.syncClient(), client.asyncClient(), type(pkg, service + "Waiter"),
                              type(pkg, "Default" + service + "Waiter"),
                              async ? type(pkg, service + "AsyncWaiter") : null,
                              async ? type(pkg, "Default" + service + "AsyncWaiter") : null, waiters);
    }

    /** Whether {@code operation} has waiters that are generated: modeled, and polling an operation with input. */
    static boolean waitable(OperationShape operation) {
        return operation.hasTrait(WaitableTrait.class)
               && !operation.getInputShape().equals(ShapeId.from("smithy.api#Unit"));
    }

    /** The sync waiter interface of a service, or {@code null} when it has no generated waiter. */
    static Symbol syncWaiterType(AwsCodegenContext context) {
        return hasWaiters(context) ? type(context.settings().packageName() + ".waiters",
                                          AwsSymbolProvider.serviceTypeName(context.service()) + "Waiter") : null;
    }

    /** The async waiter interface of a service, or {@code null} when it has no generated waiter. */
    static Symbol asyncWaiterType(AwsCodegenContext context) {
        return hasWaiters(context) ? type(context.settings().packageName() + ".waiters",
                                          AwsSymbolProvider.serviceTypeName(context.service()) + "AsyncWaiter")
                                   : null;
    }

    private static boolean hasWaiters(AwsCodegenContext context) {
        return TopDownIndex.of(context.model()).getContainedOperations(context.service()).stream()
                           .anyMatch(WaiterPlan::waitable);
    }

    private static WaiterMethod waiter(AwsCodegenContext context, OperationShape operation, String name,
                                       Waiter waiter) {
        Model model = context.model();
        String operationName = operation.getId().getName(context.service());
        Shape input = model.expectShape(operation.getInputShape());
        Shape output = model.expectShape(operation.getOutputShape());
        List<AcceptorPlan> acceptors = new ArrayList<>();
        for (Acceptor acceptor : waiter.getAcceptors()) {
            String state = acceptor.getState().toString().toUpperCase(Locale.ROOT);
            Matcher<?> matcher = acceptor.getMatcher();
            String where = operation.getId() + " waiter " + name;
            if (matcher instanceof Matcher.OutputMember member) {
                PathMatcher path = member.getValue();
                acceptors.add(new AcceptorPlan(state, false,
                                               compile(context, parse(path, where), output, new Root("response"),
                                                       where),
                                               path.getComparator().toString(), path.getExpected(), null, null,
                                               null));
            } else if (matcher instanceof Matcher.InputOutputMember member) {
                PathMatcher path = member.getValue();
                acceptors.add(new AcceptorPlan(state, false, compileInputOutput(context, parse(path, where), input,
                                                                                output, where),
                                               path.getComparator().toString(), path.getExpected(), null, null,
                                               null));
            } else if (matcher instanceof Matcher.SuccessMember member) {
                acceptors.add(new AcceptorPlan(state, !member.getValue(), null, null, null, member.getValue(), null,
                                               null));
            } else if (matcher instanceof Matcher.ErrorTypeMember member) {
                acceptors.add(new AcceptorPlan(state, true, null, null, null, null, member.getValue(),
                                               errorClass(context, operation, member.getValue())));
            } else {
                throw new AwsCodegenException(where + ": unsupported acceptor matcher " + matcher.getMemberName());
            }
        }
        return new WaiterMethod(name, "waitUntil" + name, operationName, ClientPlan.methodName(operationName),
                                context.symbolProvider().toSymbol(input), context.symbolProvider().toSymbol(output),
                                waiter.getMinDelay(), waiter.getMaxDelay(), acceptors);
    }

    private static JmespathExpression parse(PathMatcher path, String where) {
        try {
            return JmespathExpression.parse(path.getPath());
        } catch (RuntimeException e) {
            throw new AwsCodegenException(where + ": invalid JMESPath " + path.getPath(), e);
        }
    }

    /** The modeled error an {@code errorType} names (by shape name or absolute ID), or {@code null}. */
    private static Symbol errorClass(AwsCodegenContext context, OperationShape operation, String errorType) {
        List<ShapeId> errors = new ArrayList<>(operation.getErrors());
        errors.addAll(context.service().getErrors());
        for (ShapeId error : errors) {
            if (error.toString().equals(errorType) || error.getName().equals(errorType)) {
                return context.symbolProvider().toSymbol(context.model().expectShape(error));
            }
        }
        return null;
    }

    private static Expr compileInputOutput(AwsCodegenContext context, JmespathExpression expression, Shape input,
                                           Shape output, String where) {
        // The root of an inputOutput path is the object {input, output}: its first field selects the root.
        if (expression instanceof FieldExpression field) {
            return root(field.getName(), where);
        }
        if (expression instanceof Subexpression sub && sub.getLeft() instanceof FieldExpression field) {
            boolean isInput = "input".equals(field.getName());
            return compile(context, sub.getRight(), isInput ? input : output, root(field.getName(), where), where);
        }
        if (expression instanceof ComparatorExpression comparison) {
            return new Compare(compileInputOutput(context, comparison.getLeft(), input, output, where),
                               comparison.getComparator().toString(),
                               compileInputOutput(context, comparison.getRight(), input, output, where));
        }
        if (expression instanceof LiteralExpression literal) {
            return literal(literal, where);
        }
        if (expression instanceof FunctionExpression function && "length".equals(function.getName())
            && function.getArguments().size() == 1) {
            return new Length(compileInputOutput(context, function.getArguments().get(0), input, output, where));
        }
        throw unsupported(expression, where);
    }

    private static Root root(String name, String where) {
        return switch (name) {
            case "input" -> new Root("request");
            case "output" -> new Root("response");
            default -> throw new AwsCodegenException(where + ": an inputOutput path starts with input or output");
        };
    }

    /** {@code expression} applied to {@code base}, whose shape is {@code shape} ({@code null} when unknown). */
    private static Expr compile(AwsCodegenContext context, JmespathExpression expression, Shape shape, Expr base,
                                String where) {
        Model model = context.model();
        if (expression instanceof CurrentExpression) {
            return base;
        }
        if (expression instanceof FieldExpression field) {
            if (shape == null || !shape.isStructureShape() || shape.getMember(field.getName()).isEmpty()) {
                throw new AwsCodegenException(where + ": no member " + field.getName() + " in "
                                              + (shape == null ? "a projected value" : shape.getId()));
            }
            MemberShape member = shape.getMember(field.getName()).get();
            return new Field(base, context.symbolProvider().toSymbol(shape), getter(context, member));
        }
        if (expression instanceof Subexpression sub) {
            Expr left = compile(context, sub.getLeft(), shape, base, where);
            return compile(context, sub.getRight(), shapeOf(model, sub.getLeft(), shape), left, where);
        }
        if (expression instanceof FlattenExpression flatten) {
            return new Flatten(compile(context, flatten.getExpression(), shape, base, where));
        }
        if (expression instanceof ObjectProjectionExpression projection) {
            // A wildcard over the values of a map (a.*.b), as in SES's VerificationAttributes.*.VerificationStatus.
            // A wildcard over the members of a structure would need its member values in model order; no
            // canonical AWS waiter uses one, so it stays outside the subset.
            Shape object = shapeOf(model, projection.getLeft(), shape);
            if (!(object instanceof MapShape map)) {
                throw unsupported(expression, where);
            }
            Expr values = new Values(compile(context, projection.getLeft(), shape, base, where));
            return new Project(values, compile(context, projection.getRight(),
                                               model.expectShape(map.getValue().getTarget()), new Root("element"),
                                               where));
        }
        if (expression instanceof MultiSelectListExpression select) {
            List<Expr> items = new ArrayList<>();
            for (JmespathExpression item : select.getExpressions()) {
                items.add(compile(context, item, shape, new Root("element"), where));
            }
            return new MultiSelect(base, items);
        }
        if (expression instanceof FunctionExpression function && "contains".equals(function.getName())
            && function.getArguments().size() == 2) {
            return new Contains(compile(context, function.getArguments().get(0), shape, base, where),
                                compile(context, function.getArguments().get(1), shape, base, where));
        }
        if (expression instanceof ProjectionExpression projection) {
            Expr list = compile(context, projection.getLeft(), shape, base, where);
            Shape listShape = shapeOf(model, projection.getLeft(), shape);
            Shape element = listShape instanceof ListShape l ? model.expectShape(l.getMember().getTarget()) : null;
            return new Project(list, compile(context, projection.getRight(), element, new Root("element"), where));
        }
        if (expression instanceof FunctionExpression function && "length".equals(function.getName())
            && function.getArguments().size() == 1) {
            return new Length(compile(context, function.getArguments().get(0), shape, base, where));
        }
        if (expression instanceof ComparatorExpression comparison) {
            return new Compare(compile(context, comparison.getLeft(), shape, base, where),
                               comparison.getComparator().toString(),
                               compile(context, comparison.getRight(), shape, base, where));
        }
        if (expression instanceof LiteralExpression literal) {
            return literal(literal, where);
        }
        if (expression instanceof AndExpression and) {
            return new Logic("and", compile(context, and.getLeft(), shape, base, where),
                             compile(context, and.getRight(), shape, base, where));
        }
        if (expression instanceof OrExpression or) {
            return new Logic("or", compile(context, or.getLeft(), shape, base, where),
                             compile(context, or.getRight(), shape, base, where));
        }
        if (expression instanceof NotExpression not) {
            return new Logic("not", compile(context, not.getExpression(), shape, base, where), null);
        }
        if (expression instanceof FilterProjectionExpression filter) {
            Expr list = compile(context, filter.getLeft(), shape, base, where);
            Shape listShape = shapeOf(model, filter.getLeft(), shape);
            Shape element = listShape instanceof ListShape l ? model.expectShape(l.getMember().getTarget()) : null;
            return new Filter(list, compile(context, filter.getComparison(), element, new Root("element"), where),
                              compile(context, filter.getRight(), element, new Root("element"), where));
        }
        throw unsupported(expression, where);
    }

    /** The shape {@code expression} selects from {@code shape}, or {@code null} when it is not a plain path. */
    private static Shape shapeOf(Model model, JmespathExpression expression, Shape shape) {
        if (shape == null) {
            return null;
        }
        if (expression instanceof CurrentExpression) {
            return shape;
        }
        if (expression instanceof FieldExpression field) {
            return shape.getMember(field.getName()).map(m -> model.expectShape(m.getTarget())).orElse(null);
        }
        if (expression instanceof Subexpression sub) {
            return shapeOf(model, sub.getRight(), shapeOf(model, sub.getLeft(), shape));
        }
        if (expression instanceof FlattenExpression flatten) {
            Shape list = shapeOf(model, flatten.getExpression(), shape);
            if (list instanceof ListShape l && model.expectShape(l.getMember().getTarget()) instanceof ListShape) {
                return model.expectShape(l.getMember().getTarget());
            }
            return list;
        }
        if (expression instanceof ObjectProjectionExpression projection) {
            // A map value projection (a.*.b) yields a list of the right-hand shape, like a list projection.
            Shape object = shapeOf(model, projection.getLeft(), shape);
            Shape value = object instanceof MapShape map ? model.expectShape(map.getValue().getTarget()) : null;
            Shape projected = shapeOf(model, projection.getRight(), value);
            if (projected == null || model.getShape(projected.getId()).isEmpty()) {
                return null;
            }
            return ListShape.builder().id(PROJECTED_LIST).member(projected.getId()).build();
        }
        if (expression instanceof ProjectionExpression projection
            && !(expression instanceof FilterProjectionExpression)) {
            // A list projection (a[].b) yields a list of the right-hand shape, so a projection that follows it
            // (a[].b[].c, flattened) still resolves its members, as in EC2's Reservations[].Instances[].State.Name.
            Shape list = shapeOf(model, projection.getLeft(), shape);
            Shape element = list instanceof ListShape l ? model.expectShape(l.getMember().getTarget()) : null;
            Shape projected = shapeOf(model, projection.getRight(), element);
            if (projected == null || model.getShape(projected.getId()).isEmpty()) {
                return null;
            }
            return ListShape.builder().id(PROJECTED_LIST).member(projected.getId()).build();
        }
        return null;
    }

    private static String getter(AwsCodegenContext context, MemberShape member) {
        String name = context.symbolProvider().toMemberName(member);
        Shape target = context.model().expectShape(member.getTarget());
        if (isStringEnum(target)) {
            return name + "AsString";
        }
        if (target instanceof ListShape list && isStringEnum(context.model().expectShape(list.getMember()
                                                                                              .getTarget()))) {
            return name + "AsStrings";
        }
        return name;
    }

    private static boolean isStringEnum(Shape shape) {
        return shape.isEnumShape() || (shape.isStringShape() && shape.hasTrait(EnumTrait.class));
    }

    private static Literal literal(LiteralExpression literal, String where) {
        if (literal.isStringValue()) {
            return new Literal(literal.expectStringValue());
        }
        if (literal.isBooleanValue()) {
            return new Literal(literal.expectBooleanValue());
        }
        if (literal.isNumberValue()) {
            Number number = literal.expectNumberValue();
            return new Literal(number instanceof Double || number instanceof Float ? (Object) number.doubleValue()
                                                                                   : (Object) number.longValue());
        }
        if (literal.isNullValue()) {
            return new Literal(null);
        }
        throw new AwsCodegenException(where + ": unsupported JMESPath literal " + literal);
    }

    private static AwsCodegenException unsupported(JmespathExpression expression, String where) {
        return new AwsCodegenException(where + ": JMESPath " + expression.getClass().getSimpleName()
                                       + " is outside the supported waiter subset");
    }

    private static Symbol type(String pkg, String name) {
        return Symbol.builder().namespace(pkg, ".").name(name)
                     .definitionFile(pkg.replace('.', '/') + "/" + name + ".java").build();
    }
}
