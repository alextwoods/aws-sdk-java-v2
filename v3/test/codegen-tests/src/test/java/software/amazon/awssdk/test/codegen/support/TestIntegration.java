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

package software.amazon.awssdk.test.codegen.support;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.awscodegen.AwsCodegenContext;
import software.amazon.awssdk.awscodegen.AwsCodegenIntegration;
import software.amazon.awssdk.awscodegen.AwsCodegenSettings;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A configurable {@link AwsCodegenIntegration} for fixtures: an identifier, an order and optional
 * {@code preprocessModel}, {@code decorateSymbolProvider} and {@code customize} behavior. Instances are immutable;
 * each {@code with...} method returns a copy.
 */
public final class TestIntegration implements AwsCodegenIntegration {

    private final String identifier;
    private final int order;
    private final UnaryOperator<Model> preprocess;
    private final UnaryOperator<SymbolProvider> decorate;
    private final Consumer<AwsCodegenContext> customize;

    private TestIntegration(String identifier, int order, UnaryOperator<Model> preprocess,
                            UnaryOperator<SymbolProvider> decorate, Consumer<AwsCodegenContext> customize) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        this.order = order;
        this.preprocess = preprocess;
        this.decorate = decorate;
        this.customize = customize;
    }

    /** An integration that does nothing, ordered at 0. */
    public static TestIntegration named(String identifier) {
        return new TestIntegration(identifier, 0, UnaryOperator.identity(), UnaryOperator.identity(), c -> {
        });
    }

    public TestIntegration withOrder(int newOrder) {
        return new TestIntegration(identifier, newOrder, preprocess, decorate, customize);
    }

    public TestIntegration withPreprocess(UnaryOperator<Model> newPreprocess) {
        return new TestIntegration(identifier, order, newPreprocess, decorate, customize);
    }

    public TestIntegration withSymbolDecorator(UnaryOperator<SymbolProvider> newDecorate) {
        return new TestIntegration(identifier, order, preprocess, newDecorate, customize);
    }

    public TestIntegration withCustomize(Consumer<AwsCodegenContext> newCustomize) {
        return new TestIntegration(identifier, order, preprocess, decorate, newCustomize);
    }

    /**
     * A symbol decorator that maps the shape {@code target} to the symbol {@code rename} computes from the delegate's
     * symbol, and every other shape (and every member name) to the delegate's answer.
     */
    public static UnaryOperator<SymbolProvider> mapSymbol(ShapeId target, BiFunction<Shape, Symbol, Symbol> rename) {
        return delegate -> new SymbolProvider() {
            @Override
            public Symbol toSymbol(Shape shape) {
                Symbol symbol = delegate.toSymbol(shape);
                return shape.getId().equals(target) ? rename.apply(shape, symbol) : symbol;
            }

            @Override
            public String toMemberName(MemberShape shape) {
                return delegate.toMemberName(shape);
            }
        };
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public Model preprocessModel(Model model, AwsCodegenSettings settings) {
        return preprocess.apply(model);
    }

    @Override
    public SymbolProvider decorateSymbolProvider(Model model, AwsCodegenSettings settings,
                                                 SymbolProvider symbolProvider) {
        return decorate.apply(symbolProvider);
    }

    @Override
    public void customize(AwsCodegenContext context) {
        customize.accept(context);
    }

    @Override
    public String toString() {
        return "TestIntegration[" + identifier + ", order " + order + "]";
    }
}
