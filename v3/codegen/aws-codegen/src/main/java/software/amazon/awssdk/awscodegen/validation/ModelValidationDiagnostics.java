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

package software.amazon.awssdk.awscodegen.validation;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Converts the {@code ERROR} and {@code DANGER} events of Smithy model assembly into {@link DiagnosticCode#INVALID_MODEL}
 * diagnostics. Lower severities (including the warnings Smithy emits for retained unknown traits) never fail
 * generation.
 */
public final class ModelValidationDiagnostics {

    private ModelValidationDiagnostics() {
    }

    /**
     * Returns the sorted failing diagnostics of {@code result}, or a single diagnostic if assembly produced no model
     * without reporting why.
     *
     * @param service absolute shape ID of the service being generated
     * @param modelDirectory the model directory; event locations under it are reported relative to it
     */
    public static List<Diagnostic> from(String service, Path modelDirectory, ValidatedResult<Model> result) {
        Optional<Model> model = result.getResult();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ValidationEvent event : result.getValidationEvents()) {
            if (event.getSeverity().compareTo(Severity.DANGER) < 0) {
                continue;
            }
            String element = "Smithy validation event " + event.getId()
                              + event.getShapeId().map(id -> " on " + id).orElse("");
            diagnostics.add(Diagnostic.of(DiagnosticCode.INVALID_MODEL, service,
                                          event.getShapeId().flatMap(id -> operationOf(model, id)).orElse(null),
                                          source(modelDirectory, event.getSourceLocation()), element,
                                          event.getSeverity() + ": " + event.getMessage()));
        }
        if (diagnostics.isEmpty() && model.isEmpty()) {
            diagnostics.add(Diagnostic.forService(DiagnosticCode.INVALID_MODEL, service, "Smithy model assembly",
                                                  "Smithy model assembly produced no model."));
        }
        return diagnostics.stream().sorted().toList();
    }

    /** The operation an event concerns, when the event's shape is an operation. */
    private static Optional<String> operationOf(Optional<Model> model, ShapeId id) {
        return model.flatMap(m -> m.getShape(id))
                    .filter(Shape::isOperationShape)
                    .map(shape -> shape.getId().toString());
    }

    private static String source(Path modelDirectory, SourceLocation location) {
        if (location == null || location == SourceLocation.NONE || location.getFilename().isEmpty()
            || "N/A".equals(location.getFilename())) {
            return null;
        }
        String file = location.getFilename();
        try {
            Path path = Path.of(file).toAbsolutePath().normalize();
            Path root = modelDirectory.toAbsolutePath().normalize();
            if (path.startsWith(root)) {
                file = ModelSourceValidator.portable(root.relativize(path));
            }
        } catch (InvalidPathException e) {
            // Not a file-system path (for example a jar: URL of a trait definition); report it verbatim.
        }
        return file + ":" + location.getLine() + ":" + location.getColumn();
    }
}
