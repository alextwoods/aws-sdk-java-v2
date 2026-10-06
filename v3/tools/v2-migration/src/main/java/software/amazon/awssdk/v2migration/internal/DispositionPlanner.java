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

package software.amazon.awssdk.v2migration.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationDiagnostic;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.SourceLocation;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Turns recipe answers into exactly one disposition per item and applies the accepted edits. The rules (see
 * {@link software.amazon.awssdk.v2migration.recipe.MigrationRecipe}) guarantee that unresolved constructs are left
 * byte-for-byte unchanged:
 * <ol>
 *   <li>Each item gets a provisional disposition from its answers, or from its manifest classification without one.
 *       A transformation is accepted only if each edit targets an editable file, stays within the file, and either
 *       stays within the item's own range or is an insertion, and the item's edits do not overlap each other.</li>
 *   <li>Until nothing changes: a transformation whose edit touches the text of another unresolved item (overlaps it,
 *       or inserts strictly inside it) becomes unresolved, and transformations whose edits overlap each other (or
 *       insert strictly inside each other's replaced ranges) all become unresolved. Unresolving only grows the set of
 *       protected ranges, so this terminates.</li>
 *   <li>The remaining edits are non-overlapping by construction; identical insertions are applied once, and edits are
 *       applied in (offset, item, edit) order. The result is then checked: every unresolved range must appear
 *       unchanged in the output.</li>
 * </ol>
 * Items, answers and edits are processed in a fixed order, so the same input always yields the same output.
 */
public final class DispositionPlanner {

    /** One recipe's answer. */
    public record Answer(String recipe, RecipeOutcome outcome) {
    }

    /** One item, its manifest entry (or {@code null}) and every applicable answer in recipe-ID order. */
    public record Proposal(MigrationItem item, ManifestEntry entry, List<Answer> answers) {
    }

    /** The disposition of every item, in item order, and the new text of every changed file. */
    public record Plan(List<ItemResult> results, Map<String, String> changed) {
    }

    private DispositionPlanner() {
    }

    /** Plans {@code proposals} (in item order) against the original text of every editable file. */
    public static Plan plan(List<Proposal> proposals, Map<String, String> editableTexts) {
        List<Decision> decisions = new ArrayList<>();
        for (int i = 0; i < proposals.size(); i++) {
            decisions.add(decide(i, proposals.get(i), editableTexts));
        }
        resolveConflicts(decisions);
        Map<String, String> changed = apply(decisions, editableTexts);
        verifyUnresolvedUnchanged(decisions, editableTexts, changed);
        List<ItemResult> results = decisions.stream().map(DispositionPlanner::result).toList();
        return new Plan(results, changed);
    }

    /** A mutable provisional decision. */
    private static final class Decision {
        final int order;
        final MigrationItem item;
        final ManifestEntry entry;
        Disposition disposition;
        String recipe;
        String reason;
        List<TextEdit> edits = List.of();

        Decision(int order, MigrationItem item, ManifestEntry entry) {
            this.order = order;
            this.item = item;
            this.entry = entry;
        }

        void unresolve(String why) {
            disposition = Disposition.UNRESOLVED;
            reason = recipe == null ? why : "Recipe " + recipe + " proposed a transformation, but " + why;
            edits = List.of();
        }
    }

    private static Decision decide(int order, Proposal proposal, Map<String, String> texts) {
        Decision d = new Decision(order, proposal.item(), proposal.entry());
        List<Answer> unsafe = of(proposal, RecipeOutcome.Unsafe.class);
        List<Answer> transforms = of(proposal, RecipeOutcome.Transform.class);
        List<Answer> preserves = of(proposal, RecipeOutcome.Preserve.class);
        if (!unsafe.isEmpty()) {
            d.disposition = Disposition.UNRESOLVED;
            d.recipe = unsafe.get(0).recipe();
            d.reason = unsafe.stream().map(a -> "Recipe " + a.recipe() + " judged the transformation unsafe: "
                                                + ((RecipeOutcome.Unsafe) a.outcome()).reason())
                             .collect(Collectors.joining(" "));
        } else if (transforms.size() > 1 || (transforms.size() == 1 && !preserves.isEmpty())) {
            d.disposition = Disposition.UNRESOLVED;
            d.reason = "Recipes disagree on this construct ("
                       + proposal.answers().stream().map(a -> a.recipe() + ": " + verb(a.outcome()))
                                 .collect(Collectors.joining(", "))
                       + "), so it is left unchanged.";
        } else if (transforms.size() == 1) {
            RecipeOutcome.Transform t = (RecipeOutcome.Transform) transforms.get(0).outcome();
            d.recipe = transforms.get(0).recipe();
            String invalid = invalidEdits(d.item, t.edits(), texts);
            if (invalid != null) {
                d.unresolve(invalid);
            } else {
                d.disposition = Disposition.TRANSFORMED;
                d.reason = t.summary();
                d.edits = t.edits();
            }
        } else if (!preserves.isEmpty()) {
            d.disposition = Disposition.PRESERVED;
            d.recipe = preserves.get(0).recipe();
            d.reason = ((RecipeOutcome.Preserve) preserves.get(0).outcome()).rationale();
        } else {
            byManifest(d);
        }
        return d;
    }

    private static List<Answer> of(Proposal proposal, Class<? extends RecipeOutcome> type) {
        return proposal.answers().stream().filter(a -> type.isInstance(a.outcome())).toList();
    }

    private static String verb(RecipeOutcome outcome) {
        if (outcome instanceof RecipeOutcome.Transform) {
            return "transform";
        }
        if (outcome instanceof RecipeOutcome.Preserve) {
            return "preserve";
        }
        return outcome instanceof RecipeOutcome.Unsafe ? "unsafe" : "not applicable";
    }

    /** The disposition when no recipe answered. */
    private static void byManifest(Decision d) {
        ManifestEntry e = d.entry;
        if (e == null) {
            d.disposition = Disposition.UNRESOLVED;
            d.reason = d.item.detail() != null
                       ? d.item.detail() + "."
                       : "No Parity Manifest entry matches this construct, so it is not part of the Launch Parity "
                         + "Baseline's public surface and its V3 compatibility is unknown.";
            return;
        }
        switch (e.classification()) {
            case PRESERVED -> {
                d.disposition = Disposition.PRESERVED;
                d.reason = "PRESERVED in the Parity Manifest.";
            }
            case ADAPTED -> {
                d.disposition = Disposition.UNRESOLVED;
                d.reason = e.migrationKind() == MigrationKind.RECIPE
                           ? "ADAPTED in the Parity Manifest with an automated recipe, but no registered recipe "
                             + "transformed this construct; apply the migration manually."
                           : "ADAPTED in the Parity Manifest; the change requires manual adaptation.";
            }
            case EXCLUDED -> {
                d.disposition = Disposition.UNRESOLVED;
                d.reason = "EXCLUDED from V3 by the Parity Manifest; "
                           + (e.replacement() != null ? "migrate to the replacement." : "no V3 replacement exists.");
            }
            default -> throw new IllegalStateException(e.classification().name());
        }
    }

    /** Why a transformation's edits are structurally invalid, or {@code null} if they are valid. */
    private static String invalidEdits(MigrationItem item, List<TextEdit> edits, Map<String, String> texts) {
        SourceLocation own = item.location();
        for (TextEdit e : edits) {
            String text = texts.get(e.path());
            if (text == null) {
                return "its edit targets " + e.path() + ", which the tool may not edit.";
            }
            if (e.endOffset() > text.length()) {
                return "its edit [" + e.startOffset() + ", " + e.endOffset() + ") lies outside " + e.path() + ".";
            }
            if (!e.isInsertion() && !(e.path().equals(own.path()) && e.startOffset() >= own.startOffset()
                                      && e.endOffset() <= own.endOffset())) {
                return "its edit [" + e.startOffset() + ", " + e.endOffset() + ") of " + e.path()
                       + " replaces text outside the item's own range.";
            }
        }
        for (int i = 0; i < edits.size(); i++) {
            for (int j = i + 1; j < edits.size(); j++) {
                if (conflict(edits.get(i), edits.get(j))) {
                    return "its own edits overlap each other.";
                }
            }
        }
        return null;
    }

    /** Fixed point of the cross-item rules. */
    private static void resolveConflicts(List<Decision> decisions) {
        boolean changed = true;
        while (changed) {
            changed = false;
            Map<String, List<Decision>> unresolvedByPath = new LinkedHashMap<>();
            for (Decision d : decisions) {
                if (d.disposition == Disposition.UNRESOLVED && !d.item.location().isPosition()) {
                    unresolvedByPath.computeIfAbsent(d.item.location().path(), p -> new ArrayList<>()).add(d);
                }
            }
            for (Decision d : decisions) {
                if (d.disposition != Disposition.TRANSFORMED) {
                    continue;
                }
                Decision blocker = protectedBy(d, unresolvedByPath);
                if (blocker != null) {
                    d.unresolve("its edit would change the unresolved construct at " + blocker.item.location()
                                + " (" + blocker.item.construct() + "), which must be left unchanged.");
                    changed = true;
                }
            }
            Map<String, List<Owned>> editsByPath = new TreeMap<>();
            for (Decision d : decisions) {
                if (d.disposition == Disposition.TRANSFORMED) {
                    for (int i = 0; i < d.edits.size(); i++) {
                        TextEdit e = d.edits.get(i);
                        editsByPath.computeIfAbsent(e.path(), p -> new ArrayList<>()).add(new Owned(d, i, e));
                    }
                }
            }
            for (List<Owned> edits : editsByPath.values()) {
                for (int i = 0; i < edits.size(); i++) {
                    for (int j = i + 1; j < edits.size(); j++) {
                        Owned a = edits.get(i);
                        Owned b = edits.get(j);
                        if (a.owner != b.owner && a.owner.disposition == Disposition.TRANSFORMED
                            && b.owner.disposition == Disposition.TRANSFORMED && conflict(a.edit, b.edit)) {
                            a.owner.unresolve("its edits overlap those of the construct at " + b.owner.item.location()
                                              + " (" + b.owner.item.construct() + ").");
                            b.owner.unresolve("its edits overlap those of the construct at " + a.owner.item.location()
                                              + " (" + a.owner.item.construct() + ").");
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    /** The first other unresolved item whose text an edit of {@code d} would change. */
    private static Decision protectedBy(Decision d, Map<String, List<Decision>> unresolvedByPath) {
        for (TextEdit e : d.edits) {
            for (Decision u : unresolvedByPath.getOrDefault(e.path(), List.of())) {
                if (u == d) {
                    continue;
                }
                SourceLocation p = u.item.location();
                boolean touches = e.isInsertion() ? p.strictlyContains(e.path(), e.startOffset())
                                                  : p.overlaps(e.path(), e.startOffset(), e.endOffset());
                if (touches) {
                    return u;
                }
            }
        }
        return null;
    }

    /** True if two edits of one file cannot both be applied unambiguously. */
    private static boolean conflict(TextEdit a, TextEdit b) {
        if (!a.path().equals(b.path())) {
            return false;
        }
        if (a.isInsertion() && b.isInsertion()) {
            return false;
        }
        if (a.isInsertion()) {
            return b.startOffset() < a.startOffset() && a.startOffset() < b.endOffset();
        }
        if (b.isInsertion()) {
            return a.startOffset() < b.startOffset() && b.startOffset() < a.endOffset();
        }
        return a.startOffset() < b.endOffset() && b.startOffset() < a.endOffset();
    }

    private record Owned(Decision owner, int index, TextEdit edit) {
    }

    private static Map<String, String> apply(List<Decision> decisions, Map<String, String> texts) {
        Map<String, List<Owned>> byPath = new TreeMap<>();
        for (Decision d : decisions) {
            if (d.disposition == Disposition.TRANSFORMED) {
                for (int i = 0; i < d.edits.size(); i++) {
                    TextEdit e = d.edits.get(i);
                    byPath.computeIfAbsent(e.path(), p -> new ArrayList<>()).add(new Owned(d, i, e));
                }
            }
        }
        Map<String, String> changed = new TreeMap<>();
        byPath.forEach((path, edits) -> {
            String original = texts.get(path);
            StringBuilder out = new StringBuilder(original.length());
            int position = 0;
            for (TextEdit e : ordered(edits)) {
                if (e.startOffset() < position) {
                    throw new IllegalStateException("Overlapping edits survived planning in " + path + ": " + e);
                }
                out.append(original, position, e.startOffset()).append(e.replacement());
                position = e.endOffset();
            }
            out.append(original, position, original.length());
            String result = out.toString();
            if (!result.equals(original)) {
                changed.put(path, result);
            }
        });
        return changed;
    }

    /** The edits of one file in application order, identical insertions (same offset and text) kept once. */
    private static List<TextEdit> ordered(List<Owned> edits) {
        List<Owned> sorted = new ArrayList<>(edits);
        sorted.sort(Comparator.comparingInt((Owned o) -> o.edit.startOffset())
                              .thenComparingInt(o -> o.edit.endOffset())
                              .thenComparingInt(o -> o.owner.order)
                              .thenComparingInt(o -> o.index));
        List<TextEdit> result = new ArrayList<>();
        Set<String> insertions = new HashSet<>();
        for (Owned o : sorted) {
            if (!o.edit.isInsertion() || insertions.add(o.edit.startOffset() + ":" + o.edit.replacement())) {
                result.add(o.edit);
            }
        }
        return result;
    }

    /** The non-destructive guarantee: every unresolved range is present, unchanged, in the output. */
    private static void verifyUnresolvedUnchanged(List<Decision> decisions, Map<String, String> texts,
                                                  Map<String, String> changed) {
        Map<String, List<Owned>> applied = new TreeMap<>();
        for (Decision d : decisions) {
            if (d.disposition == Disposition.TRANSFORMED) {
                for (int i = 0; i < d.edits.size(); i++) {
                    TextEdit e = d.edits.get(i);
                    applied.computeIfAbsent(e.path(), p -> new ArrayList<>()).add(new Owned(d, i, e));
                }
            }
        }
        for (Decision d : decisions) {
            SourceLocation loc = d.item.location();
            if (d.disposition != Disposition.UNRESOLVED || loc.isPosition() || !changed.containsKey(loc.path())) {
                continue;
            }
            int shift = 0;
            for (TextEdit e : ordered(applied.getOrDefault(loc.path(), List.of()))) {
                if (e.endOffset() <= loc.startOffset()) {
                    shift += e.replacement().length() - (e.endOffset() - e.startOffset());
                }
            }
            String original = texts.get(loc.path());
            String output = changed.get(loc.path());
            int length = loc.endOffset() - loc.startOffset();
            int start = loc.startOffset() + shift;
            if (start < 0 || start + length > output.length()
                || !output.regionMatches(start, original, loc.startOffset(), length)) {
                throw new IllegalStateException("The unresolved construct at " + loc + " was changed");
            }
        }
    }

    private static ItemResult result(Decision d) {
        MigrationDiagnostic diagnostic = null;
        if (d.disposition == Disposition.UNRESOLVED) {
            ManifestEntry e = d.entry;
            diagnostic = new MigrationDiagnostic(d.item.location(), d.item.construct(), e == null ? d.item.id() : e.id(),
                                                 e == null ? null : e.classification(),
                                                 e == null ? null : e.rationale(),
                                                 e == null ? null : e.replacement(),
                                                 e == null ? null : e.migrationKind(),
                                                 e == null ? null : e.migration(),
                                                 e == null ? List.of() : e.differences(),
                                                 d.reason);
        }
        return new ItemResult(d.item, d.disposition, d.entry, d.recipe, d.reason, d.edits, diagnostic);
    }
}
