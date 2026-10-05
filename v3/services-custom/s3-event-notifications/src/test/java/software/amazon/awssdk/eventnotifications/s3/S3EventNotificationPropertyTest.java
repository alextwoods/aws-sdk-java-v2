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

package software.amazon.awssdk.eventnotifications.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.eventnotifications.s3.RandomNotifications.Shape;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.smithy.runtime.document.ModeledDocument;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.services.s3.S3ServiceMetadata;

/**
 * Properties of the event notification values over random notifications ({@link RandomNotifications}).
 *
 * <ul>
 *     <li>Document round trip (Requirements 6.4, 6.9): a notification converted to a Document and constructed back
 *         through the composed S3 and event notification registry equals the original, with member presence,
 *         {@code null} collection elements, timestamps and order kept.</li>
 *     <li>JSON round trip (Requirement 16.8): reading the compact or pretty JSON of a notification, from a string, bytes
 *         or a stream, gives back an equal notification.</li>
 *     <li>Unknown fields (Requirement 16.8, V2 "Any extra fields will be ignored"): adding unknown members of any JSON
 *         type to every object of the message does not change what is read.</li>
 * </ul>
 */
class S3EventNotificationPropertyTest {
    private static final SchemaTypeRegistry COMBINED = SchemaTypeRegistry.compose(
        S3ServiceMetadata.typeRegistry(), S3EventNotificationMetadata.typeRegistry());

    @Provide
    Arbitrary<S3EventNotification> anyNotification() {
        return RandomNotifications.notifications(Shape.ANY);
    }

    @Provide
    Arbitrary<S3EventNotification> jsonFaithfulNotification() {
        return RandomNotifications.notifications(Shape.JSON_FAITHFUL);
    }

    @Property(tries = 300)
    void documentRoundTrip_throughComposedRegistry_returnsAnEqualNotification(
        @ForAll("anyNotification") S3EventNotification notification) {
        ModeledDocument document = SchemaDocuments.of(notification);

        assertThat(COMBINED.deserialize(document)).isEqualTo(notification);
        assertThat(SchemaDocuments.toShape(document, COMBINED.createBuilder(S3EventNotification.ID)))
            .isEqualTo(notification);
        assertThat(SchemaDocuments.equivalent(S3EventNotification.SCHEMA, document,
                                              SchemaDocuments.of((S3EventNotification) COMBINED.deserialize(document))))
            .isTrue();
    }

    @Property(tries = 300)
    void fromJson_ofToJson_returnsAnEqualNotification(
        @ForAll("jsonFaithfulNotification") S3EventNotification notification) {
        String compact = notification.toJson();
        String pretty = notification.toJsonPretty();

        assertThat(S3EventNotification.fromJson(compact)).isEqualTo(notification);
        assertThat(S3EventNotification.fromJson(pretty)).isEqualTo(notification);
        assertThat(S3EventNotification.fromJson(compact.getBytes(StandardCharsets.UTF_8))).isEqualTo(notification);
        assertThat(S3EventNotification.fromJson(new ByteArrayInputStream(pretty.getBytes(StandardCharsets.UTF_8))))
            .isEqualTo(notification);
        assertThat(S3EventNotification.fromJson(compact).toJson()).isEqualTo(compact);
    }

    @Property(tries = 300)
    void fromJson_withUnknownMembersInEveryObject_ignoresThem(
        @ForAll("jsonFaithfulNotification") S3EventNotification notification, @ForAll long seed) {
        Object tree = JsonTrees.parse(notification.toJson());
        String withUnknown = JsonTrees.write(JsonTrees.withUnknownMembers(tree, new Random(seed)));

        assertThat(S3EventNotification.fromJson(withUnknown)).isEqualTo(notification);
    }
}
