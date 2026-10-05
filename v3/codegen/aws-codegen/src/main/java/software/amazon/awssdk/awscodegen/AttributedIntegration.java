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

import java.util.List;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

/**
 * Runs every hook of one integration inside a {@link ContributionLedger} scope naming it, and records the model
 * changes and symbol-provider decorations it makes, so conflicts between integrations can be reported by identifier
 * and shape (Requirement 3.7).
 *
 * <p>Only {@code CodegenDirector} sees these wrappers; {@link AwsCodegenContext#integrations()} exposes the original
 * integration instances.
 */
final class AttributedIntegration implements AwsCodegenIntegration {

    private final AwsCodegenIntegration delegate;
    private final ContributionLedger ledger;

    private AttributedIntegration(AwsCodegenIntegration delegate, ContributionLedger ledger) {
        this.delegate = delegate;
        this.ledger = ledger;
    }

    /** Wraps each of {@code integrations}, keeping their order. */
    static List<AwsCodegenIntegration> wrap(List<AwsCodegenIntegration> integrations, ContributionLedger ledger) {
        return integrations.stream()
                           .<AwsCodegenIntegration>map(i -> new AttributedIntegration(i, ledger))
                           .toList();
    }

    /** The original integrations behind {@code integrations}, in the same order. */
    static List<AwsCodegenIntegration> unwrap(List<AwsCodegenIntegration> integrations) {
        return integrations.stream()
                           .map(i -> i instanceof AttributedIntegration attributed ? attributed.delegate : i)
                           .toList();
    }

    @Override
    public String identifier() {
        return delegate.identifier();
    }

    @Override
    public int order() {
        return delegate.order();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public byte priority() {
        return delegate.priority();
    }

    @Override
    public List<String> runBefore() {
        return delegate.runBefore();
    }

    @Override
    public List<String> runAfter() {
        return delegate.runAfter();
    }

    @Override
    public void configure(AwsCodegenSettings settings, ObjectNode integrationSettings) {
        ledger.within(identifier(), null, () -> delegate.configure(settings, integrationSettings));
    }

    @Override
    public Model preprocessModel(Model model, AwsCodegenSettings settings) {
        Model result = ledger.within(identifier(), null, () -> delegate.preprocessModel(model, settings));
        ledger.modelChange(identifier(), model, result);
        return result;
    }

    @Override
    public SymbolProvider decorateSymbolProvider(Model model, AwsCodegenSettings settings,
                                                 SymbolProvider symbolProvider) {
        SymbolProvider result = ledger.within(identifier(), null,
                                              () -> delegate.decorateSymbolProvider(model, settings, symbolProvider));
        ledger.symbolLayer(identifier(), symbolProvider, result);
        return result;
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, AwsJavaWriter>> interceptors(
        AwsCodegenContext codegenContext) {
        return ledger.within(identifier(), null, () -> delegate.interceptors(codegenContext));
    }

    @Override
    public void customize(AwsCodegenContext codegenContext) {
        ledger.within(identifier(), null, () -> delegate.customize(codegenContext));
    }
}
