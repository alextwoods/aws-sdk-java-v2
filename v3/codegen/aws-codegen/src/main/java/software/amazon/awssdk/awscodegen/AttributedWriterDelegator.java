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

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.Shape;

/**
 * The context's {@link WriterDelegator}. When the file manifest is a {@link StagedFileManifest} with a
 * {@link ContributionLedger}, every writer checkout is claimed for the contributor in scope (writers claim on creation
 * and on every write, and the delegator writes a separator to every reused writer), {@link #useShapeWriter} attributes
 * the checkout to the shape, and {@link #flushWriters()} writes the claimed files without claiming them again.
 * Otherwise it behaves exactly like Smithy's delegator.
 */
final class AttributedWriterDelegator extends WriterDelegator<AwsJavaWriter> {

    private final ContributionLedger ledger;

    private AttributedWriterDelegator(FileManifest fileManifest, SymbolProvider symbolProvider,
                                      ContributionLedger ledger, Map<String, Set<String>> packageTypeNames) {
        super(fileManifest, symbolProvider,
              (file, namespace) -> new AwsJavaWriter(file, namespace, claim((StagedFileManifest) fileManifest, file),
                                                     packageTypeNames.getOrDefault(namespace, Set.of())));
        this.ledger = ledger;
    }

    /**
     * @param packageTypeNames simple names of the generated types by Java package, which shadow {@code java.lang}
     *        types of the same name in the files of that package
     */
    static WriterDelegator<AwsJavaWriter> create(FileManifest fileManifest, SymbolProvider symbolProvider,
                                                 Map<String, Set<String>> packageTypeNames) {
        if (fileManifest instanceof StagedFileManifest staged && staged.ledger() != null) {
            return new AttributedWriterDelegator(fileManifest, symbolProvider, staged.ledger(), packageTypeNames);
        }
        return new WriterDelegator<>(fileManifest, symbolProvider,
                                     (file, namespace) -> new AwsJavaWriter(
                                         file, namespace, () -> {
                                         }, packageTypeNames.getOrDefault(namespace, Set.of())));
    }

    /** Claims {@code file} whenever the contribution in scope differs from the one that last used the writer. */
    private static Runnable claim(StagedFileManifest manifest, String file) {
        Path path = Path.of(file);
        ContributionLedger ledger = manifest.ledger();
        ContributionLedger.Contribution[] last = new ContributionLedger.Contribution[1];
        return () -> {
            ContributionLedger.Contribution current = ledger.current();
            if (current != last[0]) {
                last[0] = current;
                manifest.claim(path);
            }
        };
    }

    @Override
    public void useShapeWriter(Shape shape, Consumer<AwsJavaWriter> writerConsumer) {
        ledger.withinShape(shape.getId(), () -> super.useShapeWriter(shape, writerConsumer));
    }

    @Override
    public void flushWriters() {
        ledger.flushing(super::flushWriters);
    }
}
