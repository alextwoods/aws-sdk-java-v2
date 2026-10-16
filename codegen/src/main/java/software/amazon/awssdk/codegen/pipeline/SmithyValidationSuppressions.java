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

package software.amazon.awssdk.codegen.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.SmithyValidationSuppression;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * The {@code smithyValidationSuppressions} of one service's customization config: Smithy validation events of severity
 * {@code ERROR} or {@code DANGER} that stage 4 does not fail the service on (checkpoint 6 amendment to Requirement
 * 17.13).
 *
 * <p>They exist for the C2J test models of {@code test/codegen-generated-classes-test}, which exercise C2J forms on
 * purpose that a Smithy model cannot hold (an idempotency token on three members, a GET operation with a body, test
 * service IDs, ...). Smithy's own suppressions cannot be used: neither {@code @suppress} nor {@code metadata
 * suppressions} apply to {@code ERROR} events. The rules keep real services strictly validated and the list accurate:
 * <ul>
 *     <li>Only C2J models, and only when the caller allows suppressions
 *     ({@link ServiceInputs#allowSmithyValidationSuppressions()}; the Maven plugin's
 *     {@code allowSmithyValidationSuppressions} parameter, which only test modules set). Otherwise a configured
 *     suppression fails the service.</li>
 *     <li>Every entry needs an event ID and a non-blank reason; listed shapes must be absolute shape IDs.</li>
 *     <li>Events that mean the model did not load or its shape graph is broken ({@code Model}, {@code Target},
 *     {@code ResourceCycle}) cannot be suppressed: Smithy runs no other validator after them. Loading itself is checked
 *     before any suppression applies (see {@code CodegenPipeline}), and conversion failures (stage 3) are exceptions,
 *     not events.</li>
 *     <li>An entry that matches no {@code ERROR} or {@code DANGER} event is stale, and the pipeline fails the service
 *     on it.</li>
 *     <li>Each applied entry is reported to the {@link CodegenPipelineListener} as a warning with the service name,
 *     the number of events, and the reason.</li>
 * </ul>
 */
@SdkInternalApi
final class SmithyValidationSuppressions {
    /** Event IDs that cannot be suppressed: after them, Smithy validates nothing else. */
    static final List<String> UNSUPPRESSIBLE_IDS = Collections.unmodifiableList(Arrays.asList("Model", "Target",
                                                                                              "ResourceCycle"));

    private final String service;
    private final List<SmithyValidationSuppression> entries;

    private SmithyValidationSuppressions(String service, List<SmithyValidationSuppression> entries) {
        this.service = service;
        this.entries = entries;
    }

    /**
     * The suppressions of a service, checked against the rules above.
     *
     * @throws IllegalStateException if the service may not have suppressions, or an entry is invalid
     */
    static SmithyValidationSuppressions of(ServiceInputs inputs) {
        List<SmithyValidationSuppression> configured = inputs.customizationConfig().getSmithyValidationSuppressions();
        List<SmithyValidationSuppression> entries = configured == null ? Collections.emptyList()
                                                                       : new ArrayList<>(configured);
        String service = inputs.serviceName();
        if (entries.isEmpty()) {
            return new SmithyValidationSuppressions(service, entries);
        }
        if (inputs.source() != ServiceInputs.Source.C2J) {
            throw new IllegalStateException(
                "The customization config of " + service + " has smithyValidationSuppressions, which apply only to C2J "
                + "models. A native Smithy model is fixed, or uses Smithy's own @suppress trait or metadata "
                + "suppressions for events below ERROR.");
        }
        if (!inputs.allowSmithyValidationSuppressions()) {
            throw new IllegalStateException(
                "The customization config of " + service + " has " + entries.size() + " smithyValidationSuppressions, "
                + "but this module does not allow them. They are only for C2J test models: a test module opts in "
                + "with the allowSmithyValidationSuppressions parameter of the code generator Maven plugin. Fix the "
                + "model or the C2J_Converter instead.");
        }
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            problems.addAll(problems(i, entries.get(i)));
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Invalid smithyValidationSuppressions in the customization config of "
                                            + service + ":\n  " + String.join("\n  ", problems));
        }
        return new SmithyValidationSuppressions(service, entries);
    }

    private static List<String> problems(int index, SmithyValidationSuppression entry) {
        List<String> problems = new ArrayList<>();
        String label = "entry " + index + " (" + entry.getId() + ")";
        String id = entry.getId();
        if (StringUtils.isBlank(id) || id.chars().anyMatch(Character::isWhitespace)) {
            problems.add(label + ": the id must be a validation event ID");
        } else {
            for (String unsuppressible : UNSUPPRESSIBLE_IDS) {
                if (id.equals(unsuppressible) || id.startsWith(unsuppressible + ".")) {
                    problems.add(label + ": " + unsuppressible + " events mean the model did not load or its shapes "
                                 + "do not resolve, and cannot be suppressed");
                }
            }
        }
        if (StringUtils.isBlank(entry.getReason())) {
            problems.add(label + ": a reason is required");
        }
        if (entry.getShapes() != null) {
            for (String shape : entry.getShapes()) {
                if (shape == null || !isAbsoluteShapeId(shape)) {
                    problems.add(label + ": " + shape + " is not an absolute shape ID");
                }
            }
        }
        return problems;
    }

    private static boolean isAbsoluteShapeId(String shape) {
        try {
            ShapeId.from(shape);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Removes the {@code ERROR} and {@code DANGER} events that an entry matches, and reports each entry that matched to
     * the listener with the number of events it suppressed.
     *
     * @return the events that are not suppressed, and the stale entries: those that match no {@code ERROR} or
     * {@code DANGER} event
     */
    Applied apply(List<ValidationEvent> events, CodegenPipelineListener listener) {
        int[] matched = new int[entries.size()];
        List<ValidationEvent> remaining = new ArrayList<>(events.size());
        for (ValidationEvent event : events) {
            boolean suppressed = false;
            if (isFailure(event)) {
                for (int i = 0; i < entries.size(); i++) {
                    if (matches(entries.get(i), event)) {
                        matched[i]++;
                        suppressed = true;
                    }
                }
            }
            if (!suppressed) {
                remaining.add(event);
            }
        }
        List<String> stale = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            SmithyValidationSuppression entry = entries.get(i);
            if (matched[i] == 0) {
                stale.add(describe(entry));
            } else {
                listener.warning(service, "Suppressed " + matched[i] + " Smithy validation event(s) " + describe(entry)
                                          + " for " + service + ": " + entry.getReason());
            }
        }
        return new Applied(remaining, stale);
    }

    /**
     * The result of {@link #apply}.
     */
    static final class Applied {
        private final List<ValidationEvent> remaining;
        private final List<String> stale;

        private Applied(List<ValidationEvent> remaining, List<String> stale) {
            this.remaining = Collections.unmodifiableList(remaining);
            this.stale = Collections.unmodifiableList(stale);
        }

        /** The events no entry suppressed. */
        List<ValidationEvent> remaining() {
            return remaining;
        }

        /** The entries that matched no {@code ERROR} or {@code DANGER} event, as {@code <id> [<shapes>]}. */
        List<String> stale() {
            return stale;
        }
    }

    static boolean isFailure(ValidationEvent event) {
        return event.getSeverity() == Severity.ERROR || event.getSeverity() == Severity.DANGER;
    }

    private static boolean matches(SmithyValidationSuppression entry, ValidationEvent event) {
        if (!event.containsId(entry.getId())) {
            return false;
        }
        List<String> shapes = entry.getShapes();
        if (shapes == null || shapes.isEmpty()) {
            return true;
        }
        return event.getShapeId().map(id -> shapes.contains(id.toString())).orElse(false);
    }

    private static String describe(SmithyValidationSuppression entry) {
        List<String> shapes = entry.getShapes();
        if (shapes == null || shapes.isEmpty()) {
            return entry.getId();
        }
        return entry.getId() + " [" + String.join(", ", shapes) + "]";
    }
}
