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

package software.amazon.awssdk.v2migration.recipes.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The known launch gaps bundled from the high-level-library parity matrix. */
class KnownGapsTest {

    private static final Path MATRIX = Path.of("..", "parity-manifest", "src", "main", "resources", "software",
                                               "amazon", "awssdk", "tools", "paritymanifest",
                                               "high-level-library-matrix.json");

    @Test
    void bundled_matrix_isTheReleaseToolingMatrixUnchanged() throws Exception {
        try (InputStream in = KnownGaps.class.getResourceAsStream(KnownGaps.RESOURCE)) {
            assertNotNull(in, KnownGaps.RESOURCE);
            assertEquals(Files.readString(MATRIX, StandardCharsets.UTF_8),
                         new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void gapOf_matrixGlobs_matchV2EntryIdsByLibrary() {
        KnownGaps gaps = KnownGaps.bundled();

        assertEquals("dynamodb-enhanced",
                     gaps.gapOf("api:software.amazon.awssdk.services.dynamodb.model.AttributeValue$Builder#nul("
                                + "Ljava/lang/Boolean;)Lsoftware/amazon/awssdk/services/dynamodb/model/"
                                + "AttributeValue$Builder;").orElseThrow().library());
        assertEquals("s3-transfer-manager",
                     gaps.gapOf("api:software.amazon.awssdk.transfer.s3.model.PresignedDownloadRequest")
                         .orElseThrow().library());
        assertFalse(gaps.gapOf("api:software.amazon.awssdk.transfer.s3.S3TransferManager#uploadFile("
                               + "Lsoftware/amazon/awssdk/transfer/s3/model/UploadFileRequest;)"
                               + "Lsoftware/amazon/awssdk/transfer/s3/model/FileUpload;").isPresent());
    }

    @Test
    void parse_libraryWithoutGaps_hasNone() {
        KnownGaps gaps = KnownGaps.parse("{\"libraries\": [{\"name\": \"iam\", \"artifact\": \"a:b\"},"
                                         + " {\"name\": \"x\", \"knownGaps\": [{\"entries\": [\"api:a.B#*\"],"
                                         + " \"reason\": \"Not shipped.\"}]}]}");

        assertEquals(1, gaps.gaps().size());
        assertTrue(gaps.gapOf("api:a.B#c()V").isPresent());
        assertFalse(gaps.gapOf("api:a.B").isPresent());
    }
}
