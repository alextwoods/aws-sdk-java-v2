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
import software.amazon.awssdk.awscodegen.validation.Diagnostic;

/**
 * Thrown when {@link AwsCodegen} cannot generate a service. No generated tree is published when this is thrown.
 *
 * <p>Validation failures carry every structured {@link Diagnostic} found, in deterministic order; the message renders
 * all of them, one per line.
 */
public final class AwsCodegenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final List<Diagnostic> diagnostics;

    public AwsCodegenException(String message) {
        super(message);
        this.diagnostics = List.of();
    }

    public AwsCodegenException(String message, Throwable cause) {
        super(message, cause);
        this.diagnostics = List.of();
    }

    /**
     * A validation failure: {@code summary} followed by one rendered line per diagnostic.
     *
     * @param diagnostics at least one diagnostic, already in the order to report
     */
    public AwsCodegenException(String summary, List<Diagnostic> diagnostics) {
        super(summary + render(diagnostics));
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("A validation failure needs at least one diagnostic.");
        }
        this.diagnostics = List.copyOf(diagnostics);
    }

    /** The structured diagnostics of a validation failure; empty for other failures. */
    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    private static String render(List<Diagnostic> diagnostics) {
        StringBuilder result = new StringBuilder();
        diagnostics.forEach(d -> result.append("\n  ").append(d.render()));
        return result.toString();
    }
}
