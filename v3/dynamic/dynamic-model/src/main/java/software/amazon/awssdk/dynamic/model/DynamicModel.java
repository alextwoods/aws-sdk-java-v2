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

package software.amazon.awssdk.dynamic.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A runtime Smithy model loaded from a Smithy JSON AST, and service selection over it (Requirements 14.1-14.5, 14.17).
 *
 * <p>The JSON AST is parsed and assembled the way {@code aws-codegen} assembles canonical models: together with the
 * Smithy prelude and the trait definitions available to the generator (AWS protocol, auth and API traits, protocol
 * traits, endpoint rules, AWS endpoints and waiters), with unknown traits allowed. A trait without a definition is
 * therefore preserved as a {@code DynamicTrait} with its exact trait ID, target and Smithy node value, and the same
 * traits are known or unknown in a runtime model as in generation. Every service, operation, shape, member and trait
 * of the JSON AST is in {@link #model()}. A model whose assembly reports error or danger events is rejected.
 *
 * <p>Service selection is exact: {@link #service()} selects the only service of the model, and
 * {@link #service(ShapeId)} the service with exactly the given shape ID. Each selected service is converted to
 * runtime schemas and {@link DynamicAwsOperation}s once and cached.
 *
 * <p>This class is the only V3 entry point to the Smithy model parser and assembler; it lives in the optional
 * {@code dynamic-model} artifact, which no generated service depends on (Requirement 14.18). Instances are immutable
 * apart from that cache and are safe for concurrent use.
 */
public final class DynamicModel {

    private final Model model;
    private final List<ShapeId> serviceIds;
    private final List<String> serviceNames;
    private final Map<ShapeId, DynamicService> services = new ConcurrentHashMap<>();

    private DynamicModel(Model model) {
        this.model = model;
        // Lexical (code point) order of the absolute IDs, not ShapeId's case-insensitive natural order.
        this.serviceIds = model.getServiceShapes().stream().map(Shape::getId)
                               .sorted(Comparator.comparing(ShapeId::toString)).toList();
        this.serviceNames = serviceIds.stream().map(ShapeId::toString).toList();
    }

    /**
     * Loads the Smithy JSON AST model file {@code path} (UTF-8).
     *
     * @throws DynamicModelException ({@link DynamicModelException.Reason#INVALID_MODEL}) if the file is not a valid
     *         Smithy JSON AST or its assembly reports error or danger events
     * @throws UncheckedIOException if the file cannot be read
     */
    public static DynamicModel load(Path path) {
        Objects.requireNonNull(path, "path");
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the runtime model " + path, e);
        }
        return fromJsonAst(content, path.toString());
    }

    /**
     * Parses the Smithy JSON AST {@code jsonAst}; {@code sourceName} names it in diagnostics.
     *
     * @throws DynamicModelException ({@link DynamicModelException.Reason#INVALID_MODEL}) if the text is not a valid
     *         Smithy JSON AST or its assembly reports error or danger events
     */
    public static DynamicModel fromJsonAst(String jsonAst, String sourceName) {
        Objects.requireNonNull(jsonAst, "jsonAst");
        Objects.requireNonNull(sourceName, "sourceName");
        Node document;
        try {
            document = Node.parse(jsonAst, sourceName);
        } catch (SourceException e) {
            throw DynamicModelException.invalidModel(sourceName, "not a JSON document: " + e.getMessage(), e);
        }
        if (!document.isObjectNode() || !document.expectObjectNode().getMember("smithy").isPresent()) {
            throw DynamicModelException.invalidModel(sourceName, "not a Smithy JSON AST (a JSON object with a "
                                                                 + "\"smithy\" version member)", null);
        }
        ClassLoader classLoader = DynamicModel.class.getClassLoader();
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader)
                                        .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                                        .addDocumentNode(document);
        ValidatedResult<Model> result;
        try {
            result = assembler.assemble();
        } catch (SourceException e) {
            throw DynamicModelException.invalidModel(sourceName, e.getMessage(), e);
        }
        List<String> problems = result.getValidationEvents().stream()
                                      .filter(event -> event.getSeverity() == Severity.ERROR
                                                       || event.getSeverity() == Severity.DANGER)
                                      .sorted()
                                      .map(ValidationEvent::toString)
                                      .toList();
        if (!problems.isEmpty() || result.getResult().isEmpty()) {
            throw DynamicModelException.invalidModel(sourceName, problems.size() + " error or danger event(s):\n"
                                                                 + String.join("\n", problems), null);
        }
        return new DynamicModel(result.getResult().get());
    }

    /** The assembled runtime model, with every shape and trait of the JSON AST. */
    public Model model() {
        return model;
    }

    /** The shape IDs of every service in the model, in lexical (code point) order of their absolute IDs. */
    public List<ShapeId> serviceIds() {
        return serviceIds;
    }

    /**
     * The only service of the model (Requirement 14.2).
     *
     * @throws DynamicModelException {@link DynamicModelException.Reason#NO_SERVICE} if the model has no service
     *         (Requirement 14.5), or {@link DynamicModelException.Reason#AMBIGUOUS_SERVICE} listing every service shape
     *         ID in lexical order if it has several (Requirement 14.4)
     */
    public DynamicService service() {
        if (serviceIds.isEmpty()) {
            throw DynamicModelException.noService();
        }
        if (serviceIds.size() > 1) {
            throw DynamicModelException.ambiguousService(serviceNames);
        }
        return service(serviceIds.get(0));
    }

    /**
     * The service with exactly the shape ID {@code serviceId} (Requirement 14.3).
     *
     * @throws DynamicModelException {@link DynamicModelException.Reason#NO_SERVICE} if the model has no service
     *         (Requirement 14.5), or {@link DynamicModelException.Reason#UNKNOWN_SERVICE} naming {@code serviceId} and
     *         listing every service shape ID in lexical order if it is not a service of the model
     */
    public DynamicService service(ShapeId serviceId) {
        Objects.requireNonNull(serviceId, "serviceId");
        if (serviceIds.isEmpty()) {
            throw DynamicModelException.noService();
        }
        if (!model.getShape(serviceId).flatMap(Shape::asServiceShape).isPresent()) {
            throw DynamicModelException.unknownService(serviceId, serviceNames);
        }
        return services.computeIfAbsent(serviceId, id -> DynamicService.of(model, id));
    }

    @Override
    public String toString() {
        return "DynamicModel" + serviceIds;
    }
}
