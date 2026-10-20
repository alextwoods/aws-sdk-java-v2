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

package software.amazon.awssdk.schemaserde.sourcecompat.fixtures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.document.DocumentVisitor;

/**
 * The Legacy_Document ({@code software.amazon.awssdk.core.document.Document}), written against the Baseline_Build public
 * API (Requirement 16.5, 16.8). None of the seven prototype services has a document member, so the fixture uses the
 * Document API directly.
 */
public final class LegacyDocumentFixture {
    private LegacyDocumentFixture() {
    }

    public static Document build() {
        Document list = Document.listBuilder()
                                .addString("a")
                                .addNumber(2)
                                .addBoolean(true)
                                .addNull()
                                .build();
        return Document.mapBuilder()
                       .putString("name", "fixture")
                       .putNumber("int", 42)
                       .putNumber("long", 9_000_000_000L)
                       .putNumber("decimal", new BigDecimal("1.25"))
                       .putNumber("text", "3.5")
                       .putBoolean("flag", false)
                       .putNull("nothing")
                       .putDocument("list", list)
                       .putMap("nested", b -> b.putString("k", "v"))
                       .putList("numbers", b -> b.addNumber(1).addNumber(2.5))
                       .build();
    }

    public static List<Document> factories() {
        return Arrays.asList(Document.fromString("s"),
                             Document.fromNumber(1),
                             Document.fromNumber(2L),
                             Document.fromNumber(3.5f),
                             Document.fromNumber(4.5d),
                             Document.fromNumber(new BigDecimal("5.5")),
                             Document.fromNumber(java.math.BigInteger.TEN),
                             Document.fromNumber(SdkNumber.fromInteger(6)),
                             Document.fromNumber("7"),
                             Document.fromBoolean(true),
                             Document.fromNull(),
                             Document.fromList(Arrays.asList(Document.fromString("x"))),
                             Document.fromMap(java.util.Collections.singletonMap("k", Document.fromString("v"))));
    }

    /**
     * Reads a document through the type checks, accessors, {@code unwrap} and a visitor.
     */
    public static String describe(Document document) {
        Map<String, Document> map = document.asMap();
        List<String> parts = new ArrayList<>();
        parts.add(document.isMap() + " " + map.size());
        parts.add(map.get("name").asString());
        parts.add(String.valueOf(map.get("int").asNumber().intValue()));
        parts.add(String.valueOf(map.get("long").asNumber().longValue()));
        parts.add(map.get("decimal").asNumber().bigDecimalValue().toPlainString());
        parts.add(String.valueOf(map.get("text").asNumber().doubleValue()));
        parts.add(String.valueOf(map.get("flag").asBoolean()));
        parts.add(String.valueOf(map.get("nothing").isNull()));
        parts.add(map.get("list").asList().stream().map(LegacyDocumentFixture::kind).collect(Collectors.joining(",")));
        parts.add(map.get("nested").asMap().get("k").asString());
        parts.add(String.valueOf(map.get("numbers").unwrap()));
        parts.add(document.accept(new KindVisitor()));
        return String.join(" ", parts);
    }

    private static String kind(Document document) {
        if (document.isString()) {
            return "string";
        }
        if (document.isNumber()) {
            return "number";
        }
        if (document.isBoolean()) {
            return "boolean";
        }
        if (document.isNull()) {
            return "null";
        }
        return document.isList() ? "list" : "map";
    }

    private static final class KindVisitor implements DocumentVisitor<String> {
        @Override
        public String visitNull() {
            return "null";
        }

        @Override
        public String visitBoolean(Boolean document) {
            return "boolean";
        }

        @Override
        public String visitString(String document) {
            return "string";
        }

        @Override
        public String visitNumber(SdkNumber document) {
            return "number";
        }

        @Override
        public String visitMap(Map<String, Document> documentMap) {
            return "map of " + documentMap.size();
        }

        @Override
        public String visitList(List<Document> documentList) {
            return "list of " + documentList.size();
        }
    }
}
