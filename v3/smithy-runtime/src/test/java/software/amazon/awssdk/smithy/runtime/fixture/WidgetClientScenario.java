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

package software.amazon.awssdk.smithy.runtime.fixture;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;
import software.amazon.smithy.java.cbor.Rpcv2CborCodec;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.ValidationError;
import software.amazon.smithy.java.core.schema.Validator;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.java.xml.XmlCodec;

/**
 * Exercises everything a generated service client uses from the Smithy Java runtime boundary: shape identity, runtime
 * traits, {@code Schema}, {@code SerializableStruct}, builders, {@code Document}, the type registry, constraint
 * validation, and the JSON, CBOR and XML codecs through a static operation descriptor.
 *
 * <p>The scenario is loaded by an isolating class loader whose parent is the platform class loader, so it may only
 * reference JDK types and classes on the {@code smithy-runtime} runtime classpath. It returns a deterministic
 * transcript and throws {@link IllegalStateException} on any functional mismatch.
 */
public final class WidgetClientScenario implements Supplier<String> {

    @Override
    public String get() {
        Widget widget = Widget.builder()
                              .widgetId("widget-1")
                              .count(42)
                              .createdAt(Instant.ofEpochSecond(1_700_000_000L))
                              .secret("hunter2")
                              .payload(ByteBuffer.wrap(new byte[] {0, 1, 2, (byte) 0xFF}))
                              .tags(List.of("red", "blue"))
                              .attributes(Map.of("size", "large"))
                              .build();

        StringJoiner transcript = new StringJoiner("\n");

        Map<String, Codec> codecs = Map.of(
            "json", JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build(),
            "cbor", Rpcv2CborCodec.builder().build(),
            "xml", XmlCodec.builder().build());
        for (String name : List.of("json", "cbor", "xml")) {
            WidgetClient client = new WidgetClient(codecs.get(name));
            byte[] wire = client.wireBytes(widget);
            Widget response = client.putWidget(widget);
            check(widget.equals(response), name + " round trip changed the value: " + response);
            String rendered = name.equals("cbor") ? Base64.getEncoder().encodeToString(wire)
                                                  : new String(wire, StandardCharsets.UTF_8);
            transcript.add(name + "=" + rendered);
        }

        Document document = Document.of(widget);
        check(Widget.ID.equals(document.discriminator()), "document discriminator: " + document.discriminator());
        SerializableStruct fromRegistry = WidgetService.TYPE_REGISTRY.deserialize(document);
        check(widget.equals(fromRegistry), "type registry round trip changed the value: " + fromRegistry);
        check(widget.equals(document.asShape(Widget.builder())), "document round trip changed the value");
        transcript.add("document.discriminator=" + document.discriminator());
        transcript.add("document.members=" + document.getMemberNames());

        Schema widgetId = Widget.SCHEMA.member("widgetId");
        check(widgetId.hasTrait(TraitKey.REQUIRED_TRAIT), "widgetId must carry @required");
        transcript.add("traits.widgetId.jsonName=" + widgetId.expectTrait(TraitKey.JSON_NAME_TRAIT).getValue());
        transcript.add("traits.widgetId.length.max=" + widgetId.expectTrait(TraitKey.LENGTH_TRAIT).getMax().orElseThrow());
        transcript.add("traits.createdAt.timestampFormat="
                       + Widget.SCHEMA.member("createdAt").expectTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT).getValue());

        Validator validator = Validator.builder().build();
        check(validator.validate(widget).isEmpty(), "valid widget failed validation");
        Widget invalid = Widget.builder().widgetId("").count(5000).build();
        List<String> errorPaths = new ArrayList<>();
        for (ValidationError error : validator.validate(invalid)) {
            errorPaths.add(error.path());
        }
        transcript.add("validation.invalid=" + errorPaths);

        transcript.add("toString=" + widget);
        transcript.add("operation=" + WidgetService.PUT_WIDGET.schema().id()
                       + " service=" + WidgetService.PUT_WIDGET.service().schema().id());
        return transcript.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
