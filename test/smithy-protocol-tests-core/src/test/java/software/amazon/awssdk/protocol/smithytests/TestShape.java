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

package software.amazon.awssdk.protocol.smithytests;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;

/**
 * A hand-written {@link SdkPojo} shaped like a generated model class, for {@link ProtocolAsserts#outputEquals} tests.
 */
final class TestShape implements SdkPojo {
    private static final List<SdkField<?>> FIELDS = Arrays.asList(
        field("Name", MarshallingType.STRING, s -> s.name, (s, v) -> s.name = v),
        field("Data", MarshallingType.SDK_BYTES, s -> s.data, (s, v) -> s.data = v),
        field("Number", MarshallingType.DOUBLE, s -> s.number, (s, v) -> s.number = v),
        field("Items", MarshallingType.LIST, s -> s.items, (s, v) -> s.items = v),
        field("Attrs", MarshallingType.MAP, s -> s.attrs, (s, v) -> s.attrs = v),
        field("Nested", MarshallingType.SDK_POJO, s -> s.nested, (s, v) -> s.nested = (TestShape) v),
        field("Doc", MarshallingType.DOCUMENT, s -> s.doc, (s, v) -> s.doc = v),
        field("When", MarshallingType.INSTANT, s -> s.when, (s, v) -> s.when = v),
        field("Amount", MarshallingType.BIG_DECIMAL, s -> s.amount, (s, v) -> s.amount = v));

    String name;
    SdkBytes data;
    Double number;
    List<?> items;
    Map<String, ?> attrs;
    TestShape nested;
    Document doc;
    Instant when;
    BigDecimal amount;

    @SuppressWarnings("unchecked")
    private static <T> SdkField<T> field(String name, MarshallingType<? super T> type, Function<TestShape, T> getter,
                                         BiConsumer<TestShape, T> setter) {
        return SdkField.<T>builder((MarshallingType<T>) type)
                       .memberName(name)
                       .getter(o -> getter.apply((TestShape) o))
                       .setter((o, v) -> setter.accept((TestShape) o, v))
                       .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName(name).build())
                       .build();
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return FIELDS;
    }

    @Override
    public String toString() {
        return "TestShape(Name=" + name + ")";
    }
}
