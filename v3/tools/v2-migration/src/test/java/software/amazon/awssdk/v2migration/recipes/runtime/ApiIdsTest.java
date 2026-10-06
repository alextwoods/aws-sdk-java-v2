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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/** Manifest ID parsing and descriptor mapping. */
class ApiIdsTest {

    @Test
    void member_methodId_parsesOwnerNameParametersAndResult() {
        ApiIds.Member m = ApiIds.member("api:a.b.Outer$Inner#run([ILjava/lang/String;J)[La/b/C;").orElseThrow();

        assertEquals("a.b.Outer$Inner", m.owner());
        assertEquals("run", m.name());
        assertEquals(List.of("[I", "Ljava/lang/String;", "J"), m.parameters());
        assertEquals("[La/b/C;", m.result());
        assertFalse(m.field());
        assertEquals("Outer.Inner#run(int[], java.lang.String, long)", m.describe());
    }

    @Test
    void member_fieldAndTypeIds_areDistinguished() {
        ApiIds.Member f = ApiIds.member("api:a.B#NAME:Ljava/lang/String;").orElseThrow();

        assertTrue(f.field());
        assertEquals("Ljava/lang/String;", f.result());
        assertTrue(ApiIds.member("api:a.B").isEmpty());
        assertEquals("a.B", ApiIds.type("api:a.B").orElseThrow());
        assertEquals("a.B", ApiIds.owner("api:a.B#c()V").orElseThrow());
    }

    @Test
    void mapDescriptor_recipeReplacedType_mapsTheClassAndArrayComponent() {
        MigrationManifest manifest = RuntimeFixtures.manifest(List.of(
            RuntimeFixtures.recipe("api:a.Old", "api:b.New")));

        assertEquals("[[Lb/New;", Replacements.mapDescriptor(manifest, "[[La/Old;"));
        assertTrue(Replacements.compatible(manifest, "La/Old;", "Lb/New;"));
        assertFalse(Replacements.compatible(manifest, "La/Other;", "Lb/New;"));
        assertEquals("I", Replacements.mapDescriptor(manifest, "I"));
    }
}
