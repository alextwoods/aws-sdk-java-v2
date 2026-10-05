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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One structured generation diagnostic: what is wrong ({@link #code()}), which service's Generated Service Package it
 * prevents ({@link #service()}), the operation it concerns when it is operation-specific, the model-directory file it
 * was found in when it is file-specific, the Smithy shape and the code-generation integrations involved when it is a
 * generation conflict, the exact legacy format or semantic element involved ({@link #element()}), and a
 * human-readable message.
 *
 * <p>Diagnostics are values: two diagnostics with the same fields are equal. Their natural order (service, source,
 * operation, shape, code, element, integrations, message) is total, so sorted diagnostic lists are deterministic for
 * identical inputs. Shape IDs are held as their absolute string form and sources as {@code /}-separated paths relative
 * to the model directory, so diagnostics never contain checkout-specific absolute paths for model-directory files.
 */
public final class Diagnostic implements Comparable<Diagnostic>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Comparator<Diagnostic> ORDER =
        Comparator.comparing(Diagnostic::service)
                  .thenComparing(d -> d.source, Comparator.nullsFirst(Comparator.naturalOrder()))
                  .thenComparing(d -> d.operation, Comparator.nullsFirst(Comparator.naturalOrder()))
                  .thenComparing(d -> d.shape, Comparator.nullsFirst(Comparator.naturalOrder()))
                  .thenComparing(Diagnostic::code)
                  .thenComparing(Diagnostic::element)
                  .thenComparing(d -> String.join("\n", d.integrations))
                  .thenComparing(Diagnostic::message);

    private final DiagnosticCode code;
    private final String service;
    private final String operation;
    private final String source;
    private final String shape;
    private final List<String> integrations;
    private final String element;
    private final String message;

    private Diagnostic(DiagnosticCode code, String service, String operation, String source, String shape,
                       List<String> integrations, String element, String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.service = Objects.requireNonNull(service, "service");
        this.operation = operation;
        this.source = source;
        this.shape = shape;
        this.integrations = List.copyOf(integrations);
        this.element = Objects.requireNonNull(element, "element");
        this.message = Objects.requireNonNull(message, "message");
    }

    /** A diagnostic about a model-directory file. */
    public static Diagnostic forSource(DiagnosticCode code, String service, String source, String element,
                                      String message) {
        return new Diagnostic(code, service, null, Objects.requireNonNull(source, "source"), null, List.of(), element,
                              message);
    }

    /** A diagnostic about the service as a whole. */
    public static Diagnostic forService(DiagnosticCode code, String service, String element, String message) {
        return new Diagnostic(code, service, null, null, null, List.of(), element, message);
    }

    /** A diagnostic about one operation of the service. */
    public static Diagnostic forOperation(DiagnosticCode code, String service, String operation, String element,
                                         String message) {
        return new Diagnostic(code, service, Objects.requireNonNull(operation, "operation"), null, null, List.of(),
                              element, message);
    }

    /**
     * A diagnostic about generated output or settings of one Smithy shape that was produced by the given
     * code-generation contributors: integration identifiers, or {@code aws-codegen} for the core generator.
     *
     * @param integrations the contributors involved, in the order to report; at least one
     */
    public static Diagnostic forGeneration(DiagnosticCode code, String service, String shape, List<String> integrations,
                                           String element, String message) {
        if (integrations.isEmpty()) {
            throw new IllegalArgumentException("A generation diagnostic names at least one contributor.");
        }
        return new Diagnostic(code, service, null, null, Objects.requireNonNull(shape, "shape"), integrations, element,
                              message);
    }

    /** A diagnostic with every optional field supplied explicitly ({@code null} when absent). */
    public static Diagnostic of(DiagnosticCode code, String service, String operation, String source, String element,
                                String message) {
        return new Diagnostic(code, service, operation, source, null, List.of(), element, message);
    }

    public DiagnosticCode code() {
        return code;
    }

    /** Absolute shape ID of the service whose Generated Service Package the problem prevents. */
    public String service() {
        return service;
    }

    /** Absolute shape ID of the affected operation, when the problem is operation-specific. */
    public Optional<String> operation() {
        return Optional.ofNullable(operation);
    }

    /** Model-directory-relative path of the offending file, when the problem is file-specific. */
    public Optional<String> source() {
        return Optional.ofNullable(source);
    }

    /** Absolute ID of the Smithy shape whose generated output or settings conflict, for generation diagnostics. */
    public Optional<String> shape() {
        return Optional.ofNullable(shape);
    }

    /**
     * Identifiers of the code-generation contributors involved (integration identifiers, or {@code aws-codegen} for
     * the core generator); empty for model-input diagnostics.
     */
    public List<String> integrations() {
        return integrations;
    }

    /** The exact legacy input format, Smithy validation event, missing/unsupported semantic element or conflict. */
    public String element() {
        return element;
    }

    public String message() {
        return message;
    }

    /** Single-line rendering used in exception messages and build logs. */
    public String render() {
        StringBuilder result = new StringBuilder().append('[').append(code).append("] service ").append(service);
        if (operation != null) {
            result.append(", operation ").append(operation);
        }
        if (source != null) {
            result.append(", source ").append(source);
        }
        if (shape != null) {
            result.append(", shape ").append(shape);
        }
        if (!integrations.isEmpty()) {
            result.append(", integrations ").append(integrations);
        }
        return result.append(", element ").append(element).append(": ").append(message).toString();
    }

    @Override
    public int compareTo(Diagnostic other) {
        return ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Diagnostic other)) {
            return false;
        }
        return code == other.code && service.equals(other.service) && Objects.equals(operation, other.operation)
               && Objects.equals(source, other.source) && Objects.equals(shape, other.shape)
               && integrations.equals(other.integrations) && element.equals(other.element)
               && message.equals(other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, service, operation, source, shape, integrations, element, message);
    }

    @Override
    public String toString() {
        return render();
    }
}
