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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

class ContributionLedgerTest {
    private static final String SERVICE = "example#Service";
    private static final ShapeId WIDGET = ShapeId.from("example#Widget");
    private static final ShapeId NAME = ShapeId.from("example#Widget$name");

    @TempDir
    Path temp;

    @Test
    void changedAspectsSeparateTraitsDefinitionsAndMembers() {
        Model base = widgetModel();
        Model documented = replace(base, widget(base).toBuilder().addTrait(new DocumentationTrait("docs")).build());
        assertEquals(Map.of(WIDGET, Set.of("trait smithy.api#documentation")),
                     ContributionLedger.changedAspects(base, documented));

        MemberShape sensitiveName = member(base).toBuilder().addTrait(new SensitiveTrait()).build();
        Model memberTrait = replace(base, widget(base).toBuilder().addMember(sensitiveName).build());
        assertEquals(Map.of(NAME, Set.of("trait smithy.api#sensitive")),
                     ContributionLedger.changedAspects(base, memberTrait),
                     "a member trait change is not a definition change of its container");

        Model retargeted = replace(base, widget(base).toBuilder()
                                                     .addMember(member(base).toBuilder()
                                                                            .target("smithy.api#Integer").build())
                                                     .build());
        assertEquals(Set.of(ContributionLedger.DEFINITION), ContributionLedger.changedAspects(base, retargeted)
                                                                               .get(NAME));

        Model removed = ModelTransformer.create().removeShapes(base, List.of(widget(base)));
        assertEquals(Set.of(ContributionLedger.DEFINITION),
                     ContributionLedger.changedAspects(base, removed).get(WIDGET));
        assertEquals(Map.of(), ContributionLedger.changedAspects(base, base));
    }

    @Test
    void sameTraitChangedByTwoIntegrationsConflictsButDifferentTraitsCompose() {
        Model base = widgetModel();
        Model a = replace(base, widget(base).toBuilder().addTrait(new DocumentationTrait("a")).build());
        Model b = replace(a, widget(a).toBuilder().addTrait(new DocumentationTrait("b")).build());
        Model c = replace(b, widget(b).toBuilder().addTrait(new SensitiveTrait()).build());
        ContributionLedger ledger = new ContributionLedger(SERVICE);

        ledger.modelChange("aws.a", base, a);
        ledger.modelChange("aws.b", a, b);
        ledger.modelChange("aws.c", b, c);
        ledger.modelChange("aws.same", c, replace(c, widget(c).toBuilder().build()));

        List<Diagnostic> conflicts = ledger.conflicts();
        assertEquals(1, conflicts.size(), conflicts.toString());
        Diagnostic conflict = conflicts.get(0);
        assertEquals(DiagnosticCode.INTEGRATION_MODEL_CONFLICT, conflict.code());
        assertEquals(List.of("aws.a", "aws.b"), conflict.integrations());
        assertEquals(Optional.of(WIDGET.toString()), conflict.shape());
        assertEquals("trait smithy.api#documentation", conflict.element());
    }

    @Test
    void definitionChangeConflictsWithAnyOtherChangeOfTheShape() {
        Model base = widgetModel();
        Model documented = replace(base, widget(base).toBuilder().addTrait(new DocumentationTrait("a")).build());
        Model removed = ModelTransformer.create().removeShapes(documented, List.of(widget(documented)));
        ContributionLedger ledger = new ContributionLedger(SERVICE);

        ledger.modelChange("aws.docs", base, documented);
        ledger.modelChange("aws.remove", documented, removed);

        List<Diagnostic> widgetConflicts = ledger.conflicts().stream()
                                                 .filter(d -> d.shape().equals(Optional.of(WIDGET.toString())))
                                                 .toList();
        assertEquals(1, widgetConflicts.size(), ledger.conflicts().toString());
        assertEquals(List.of("aws.docs", "aws.remove"), widgetConflicts.get(0).integrations());
    }

    @Test
    void outputClaimedByTwoContributorsConflictsWithTheRenderedShape() {
        ContributionLedger ledger = new ContributionLedger(SERVICE);
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"), ledger);
        ledger.within(AwsCodegen.GENERATOR_IDENTIFIER, WIDGET, () -> manifest.claim(Path.of("a/Widget.java")));
        ledger.within("aws.extra", null, () -> manifest.writeFile("a/Widget.java", "other"));
        ledger.within("aws.extra", null, () -> manifest.writeFile("a/Own.java", "own"));

        List<Diagnostic> conflicts = ledger.conflicts();
        assertEquals(1, conflicts.size(), conflicts.toString());
        assertEquals(DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, conflicts.get(0).code());
        assertEquals(List.of(AwsCodegen.GENERATOR_IDENTIFIER, "aws.extra"), conflicts.get(0).integrations());
        assertEquals(Optional.of(WIDGET.toString()), conflicts.get(0).shape());
        assertEquals("output a/Widget.java", conflicts.get(0).element());
    }

    @Test
    void secondWriteByOneContributorKeepsTheFirstContentAndBlocksPublication() {
        ContributionLedger ledger = new ContributionLedger(SERVICE);
        Path output = temp.resolve("out");
        StagedFileManifest manifest = new StagedFileManifest(output, ledger);
        ledger.within("aws.twice", null, () -> {
            manifest.writeFile("a/A.java", "first");
            manifest.writeFile("a/./A.java", "second");
        });

        assertEquals("first", new String(manifest.content(Path.of("a/A.java")), StandardCharsets.UTF_8));
        AwsCodegenException e = assertThrows(AwsCodegenException.class, manifest::publish);
        Diagnostic duplicate = e.diagnostics().get(0);
        assertEquals(DiagnosticCode.DUPLICATE_GENERATED_FILE, duplicate.code());
        assertEquals(List.of("aws.twice"), duplicate.integrations());
        assertEquals(Optional.of(SERVICE), duplicate.shape(), "an unattributed file is reported for the service");
        assertTrue(duplicate.message().contains("written more than once"), duplicate.message());
        assertFalse(Files.exists(output), "nothing is published while the ledger holds a conflict");
    }

    @Test
    void writesDuringAFlushAreNotClaimedAgain() {
        ContributionLedger ledger = new ContributionLedger(SERVICE);
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"), ledger);
        ledger.within("aws.writer", null, () -> manifest.claim(Path.of("A.java")));
        ledger.flushing(() -> manifest.writeFile("A.java", "content"));

        assertEquals(List.of(), ledger.conflicts());
        assertEquals(List.of("aws.writer"), ledger.contributors(temp.resolve("out/A.java").toAbsolutePath()));
    }

    private static Model widgetModel() {
        return Model.assembler()
                    .addUnparsedModel("widget.smithy", "$version: \"2\"\nnamespace example\n"
                                                       + "structure Widget { name: String }\n")
                    .assemble()
                    .unwrap();
    }

    private static StructureShape widget(Model model) {
        return model.expectShape(WIDGET, StructureShape.class);
    }

    private static MemberShape member(Model model) {
        return model.expectShape(NAME, MemberShape.class);
    }

    private static Model replace(Model model, StructureShape shape) {
        return ModelTransformer.create().replaceShapes(model, List.of(shape));
    }
}
