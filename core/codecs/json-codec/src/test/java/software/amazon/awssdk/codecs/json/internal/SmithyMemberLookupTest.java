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


package software.amazon.awssdk.codecs.json.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codecs.json.TestCollections.listOf;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.ShapeId;

/**
 * Tests the packed-long short-name fast path in {@link SmithyMemberLookup#lookup}.
 */
public class SmithyMemberLookupTest {

    private static SmithyMemberLookup lookupOf(String... memberNames) {
        SchemaBuilder builder = Schema.structureBuilder(ShapeId.from("test#S"));
        for (String n : memberNames) {
            builder.putMember(n, PreludeSchemas.STRING);
        }
        Schema schema = builder.build();
        return new SmithyMemberLookup(schema.members(), false);
    }

    private static Schema lookup(SmithyMemberLookup l, String name) {
        byte[] b = name.getBytes(StandardCharsets.UTF_8);
        // Embed the name in a larger buffer at a non-zero offset to catch offset bugs.
        byte[] buf = new byte[b.length + 5];
        System.arraycopy(b, 0, buf, 3, b.length);
        return l.lookup(buf, 3, 3 + b.length, /* expectedNext */ -1);
    }

    @Test
    public void matchesShortUnionDiscriminators() {
        // The DynamoDB AttributeValue union shape: all discriminators are <= 4 bytes.
        SmithyMemberLookup l = lookupOf("S", "N", "B", "SS", "NS", "BS", "M", "L", "NULL", "BOOL");
        for (String name : listOf("S", "N", "B", "SS", "NS", "BS", "M", "L", "NULL", "BOOL")) {
            assertThat(lookup(l, name)).as(name).isNotNull();
            assertThat(lookup(l, name).memberName()).isEqualTo(name);
        }
    }

    @Test
    public void differentLengthSamePrefixDoNotCollide() {
        // "S" and "SS" share a prefix but differ in length: folding length into the
        // packed key must keep them distinct.
        SmithyMemberLookup l = lookupOf("S", "SS");
        assertThat(lookup(l, "S").memberName()).isEqualTo("S");
        assertThat(lookup(l, "SS").memberName()).isEqualTo("SS");
    }

    @Test
    public void unknownShortNameReturnsNull() {
        SmithyMemberLookup l = lookupOf("S", "N");
        assertThat(lookup(l, "X")).isNull();
        assertThat(lookup(l, "NS")).isNull(); // not a member here
    }

    @Test
    public void leadingControlByteDoesNotSpuriouslyMatch() {
        // A 2-byte input {0x00, 'S'} must not match member "S" (1 byte) or any other.
        // Without folding length into the key, naive big-endian packing of {0x00,'S'}
        // equals the packing of {'S'} and would mis-dispatch.
        SmithyMemberLookup l = lookupOf("S", "N");
        byte[] buf = {0x00, (byte) 'S'};
        assertThat(l.lookup(buf, 0, 2, -1)).isNull();
    }

    @Test
    public void boundaryLengthsSevenAndEight() {
        // 7 bytes uses the packed path; 8 bytes falls back to FNV. Both must resolve.
        SmithyMemberLookup l = lookupOf("seven77", "eight888");
        assertThat(l.orderedPackedNames[0]).isEqualTo(0x2237376e65766573L);
        assertThat(l.orderedPackedNames[1]).isZero();
        assertThat(lookup(l, "seven77").memberName()).isEqualTo("seven77");
        assertThat(lookup(l, "eight888").memberName()).isEqualTo("eight888");
        assertThat(lookup(l, "seven78")).isNull();
        assertThat(lookup(l, "eight889")).isNull();
    }

    @Test
    public void longNamesUseFnvFallback() {
        SmithyMemberLookup l = lookupOf("TableName", "CapacityUnits", "ReadCapacityUnits");
        assertThat(lookup(l, "TableName").memberName()).isEqualTo("TableName");
        assertThat(lookup(l, "CapacityUnits").memberName()).isEqualTo("CapacityUnits");
        assertThat(lookup(l, "ReadCapacityUnits").memberName()).isEqualTo("ReadCapacityUnits");
        assertThat(lookup(l, "WriteCapacityUnits")).isNull();
    }

}
