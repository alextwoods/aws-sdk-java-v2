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

package software.amazon.awssdk.protocol.smithytests.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpMessageTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestsTrait;

/** A client-applicable {@code httpRequestTests}/{@code httpResponseTests} case for a Supported_Protocol. */
final class SmithyCase {

    final String id;
    final String protocol;
    /** request, response, or error. */
    final String kind;
    /** Operation name for request and response cases; error shape name for error cases. */
    final String shapeName;
    final String documentation;
    final ObjectNode params;
    final Set<String> features;
    /** True for a case defined in the SDK-owned model rather than the upstream protocol test models. */
    final boolean sdkOwned;
    /** The test case itself, for {@link ContentCheck}. */
    final HttpMessageTestCase testCase;

    private SmithyCase(HttpMessageTestCase testCase, String kind, String shapeName, Set<String> features,
                       boolean sdkOwned) {
        this.id = testCase.getId();
        this.protocol = testCase.getProtocol().toString();
        this.kind = kind;
        this.shapeName = shapeName;
        this.documentation = doc(testCase);
        this.params = testCase.getParams();
        this.features = features;
        this.sdkOwned = sdkOwned;
        this.testCase = testCase;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Collects every client-applicable case of every Supported_Protocol in the model.
     *
     * @param sdkOwnedNamespacePrefix shapes whose namespace starts with this prefix are SDK-owned
     */
    static List<SmithyCase> loadAll(Model model, String sdkOwnedNamespacePrefix) {
        SmithyShapes shapes = new SmithyShapes(model);
        List<SmithyCase> out = new ArrayList<>();
        for (OperationShape op : model.getOperationShapes()) {
            ShapeView.OperationView view = shapes.operation(op);
            boolean sdkOwned = isSdkOwned(op, sdkOwnedNamespacePrefix);
            op.getTrait(HttpRequestTestsTrait.class).ifPresent(t -> {
                for (HttpRequestTestCase c : t.getTestCases()) {
                    if (applies(c)) {
                        out.add(new SmithyCase(c, "request", op.getId().getName(), FeatureExtractor.request(
                            view, c.getParams(), bindings(c), requestFacts(c)), sdkOwned));
                    }
                }
            });
            op.getTrait(HttpResponseTestsTrait.class).ifPresent(t -> {
                for (HttpResponseTestCase c : t.getTestCases()) {
                    if (applies(c)) {
                        out.add(new SmithyCase(c, "response", op.getId().getName(), FeatureExtractor.response(
                            view, c.getParams(), bindings(c), responseFacts(c)), sdkOwned));
                    }
                }
            });
        }
        for (StructureShape error : model.getStructureShapesWithTrait(HttpResponseTestsTrait.class)) {
            if (!error.hasTrait(ErrorTrait.class)) {
                continue;
            }
            ShapeView view = shapes.shape(error.getId());
            boolean sdkOwned = isSdkOwned(error, sdkOwnedNamespacePrefix);
            for (HttpResponseTestCase c : error.expectTrait(HttpResponseTestsTrait.class).getTestCases()) {
                if (applies(c)) {
                    out.add(new SmithyCase(c, "error", error.getId().getName(), FeatureExtractor.error(
                        view, c.getParams(), bindings(c), responseFacts(c)), sdkOwned));
                }
            }
        }
        out.sort((a, b) -> a.id.compareTo(b.id));
        return out;
    }

    private static boolean isSdkOwned(Shape shape, String prefix) {
        return prefix != null && shape.getId().getNamespace().startsWith(prefix);
    }

    private static boolean applies(HttpMessageTestCase c) {
        return Protocols.SUPPORTED.contains(c.getProtocol().toString())
               && c.getAppliesTo().map(a -> a == AppliesTo.CLIENT).orElse(true);
    }

    private static boolean bindings(HttpMessageTestCase c) {
        return Protocols.hasHttpBindings(c.getProtocol().toString());
    }

    private static String doc(HttpMessageTestCase c) {
        return LegacyCase.normalize(c.getDocumentation().orElse(""));
    }

    private static FeatureExtractor.RequestFacts requestFacts(HttpRequestTestCase c) {
        FeatureExtractor.RequestFacts facts = new FeatureExtractor.RequestFacts();
        facts.method = c.getMethod();
        facts.assertedHeaders.addAll(c.getHeaders().keySet());
        facts.assertedHeaders.addAll(c.getRequireHeaders());
        facts.forbiddenHeaders.addAll(c.getForbidHeaders());
        facts.assertsHost = c.getHost().isPresent() || c.getResolvedHost().isPresent();
        facts.body = c.getBody().orElse(null);
        return facts;
    }

    private static FeatureExtractor.ResponseFacts responseFacts(HttpResponseTestCase c) {
        FeatureExtractor.ResponseFacts facts = new FeatureExtractor.ResponseFacts();
        facts.statusCode = c.getCode();
        for (Map.Entry<String, String> e : c.getHeaders().entrySet()) {
            facts.headers.put(e.getKey(), e.getValue());
        }
        boolean cbor = c.getBodyMediaType().map(m -> m.contains("cbor")).orElse(false);
        if (cbor) {
            facts.binary = c.getBody().map(b -> !b.isEmpty()).orElse(false);
        } else {
            facts.body = c.getBody().orElse(null);
        }
        return facts;
    }
}
