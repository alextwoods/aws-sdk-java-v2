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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import software.amazon.awssdk.eventnotifications.s3.model.EventHoldDuration;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ObjectRetentionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ReplicationEventData;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.S3ObjectAnnotation;
import software.amazon.awssdk.eventnotifications.s3.model.TransitionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;

/**
 * Random event notifications for the property tests. Every field is independently present or absent ({@code null}),
 * collections may be empty or hold {@code null} elements, and strings include escapes, control and non-ASCII
 * characters (never unpaired surrogates, which are not valid text).
 *
 * <p>{@link Shape#JSON_FAITHFUL} limits values to those the S3 message JSON represents exactly, so that
 * {@code fromJson(toJson(n))} equals {@code n}: the writer omits an empty tag map and an event hold duration without
 * a unit. {@link Shape#V2_BASELINE} further leaves out the object retention data the V2 baseline release does not
 * have, and the values the V2 writer fails on (an absent bucket owner, a {@code null} annotation and glacier data
 * without restore data), so V2 and V3 can be compared on every generated value.
 */
public final class RandomNotifications {
    /** Which values to generate. */
    public enum Shape {
        /** Every value the model can hold. */
        ANY,
        /** Values that survive a JSON round trip unchanged. */
        JSON_FAITHFUL,
        /** JSON-faithful values that the V2 baseline release also reads and writes. */
        V2_BASELINE
    }

    private static final String[] STRINGS = {
        "", "a", "us-west-2", "ObjectCreated:Put", "aws:s3", "2.1", "HappyFace.jpg", "red+flower.jpg",
        "quote\"backslash\\slash/", "tab\tnewline\ncr\rbs\bff\f", "\u0000\u0001\u001f\u007f", "caf\u00e9 \u4e2d\u6587",
        "emoji \ud83d\ude00", "x-amz-id-2", "Records", "null", "{\"json\":[1]}"
    };

    private final Random random;
    private final Shape shape;

    private RandomNotifications(Random random, Shape shape) {
        this.random = random;
        this.shape = shape;
    }

    /** Arbitrary notifications of {@code shape}. */
    public static Arbitrary<S3EventNotification> notifications(Shape shape) {
        return Arbitraries.randomValue(random -> new RandomNotifications(random, shape).notification());
    }

    S3EventNotification notification() {
        return new S3EventNotification(present() ? list(this::record) : null);
    }

    private S3EventNotificationRecord record() {
        S3EventNotificationRecord rec = new S3EventNotificationRecord();
        rec.setEventVersion(string());
        rec.setEventSource(string());
        rec.setAwsRegion(string());
        rec.setEventTime(present() ? instant() : null);
        rec.setEventName(string());
        rec.setUserIdentity(present() ? new UserIdentity(string()) : null);
        rec.setRequestParameters(present() ? new RequestParameters(string()) : null);
        rec.setResponseElements(present() ? new ResponseElements(string(), string()) : null);
        rec.setS3(present() ? s3() : null);
        rec.setGlacierEventData(present() ? glacier() : null);
        rec.setReplicationEventData(present() ? new ReplicationEventData(string(), string(), string(), string(),
                                                                        string(), string(), string()) : null);
        rec.setIntelligentTieringEventData(present() ? new IntelligentTieringEventData(string()) : null);
        rec.setLifecycleEventData(present() ? new LifecycleEventData(present() ? new TransitionEventData(string())
                                                                               : null) : null);
        if (shape != Shape.V2_BASELINE) {
            rec.setObjectRetentionEventData(present() ? retention() : null);
        }
        return rec;
    }

    private GlacierEventData glacier() {
        boolean restore = shape == Shape.V2_BASELINE || present();
        return new GlacierEventData(restore ? new RestoreEventData(present() ? format(instant()) : null, string())
                                            : null);
    }

    private ObjectRetentionEventData retention() {
        EventHoldDuration duration = null;
        if (present()) {
            String unit = shape == Shape.ANY ? string() : nonNullString();
            duration = new EventHoldDuration(unit, present() ? random.nextLong() : null);
        }
        return new ObjectRetentionEventData(string(), string(), string(), duration);
    }

    private S3 s3() {
        List<S3ObjectAnnotation> annotations = null;
        if (present()) {
            annotations = list(() -> shape == Shape.V2_BASELINE || present()
                                     ? new S3ObjectAnnotation(string(), present() ? random.nextLong() : null, string())
                                     : null);
            if (shape == Shape.V2_BASELINE) {
                annotations.removeIf(a -> a == null);
            }
        }
        S3Bucket bucket = present() ? bucket() : null;
        S3Object object = present() ? new S3Object(string(), present() ? random.nextLong() : null, string(), string(),
                                                   string(), present() ? random.nextBoolean() : null) : null;
        return new S3(string(), bucket, object, string(), annotations);
    }

    private S3Bucket bucket() {
        Map<String, String> tags = null;
        if (present()) {
            tags = new LinkedHashMap<>();
            int size = random.nextInt(4) + (shape == Shape.ANY ? 0 : 1);
            for (int i = 0; i < size; i++) {
                tags.put("aws:" + nonNullString() + i, string());
            }
        }
        UserIdentity owner = shape == Shape.V2_BASELINE || present() ? new UserIdentity(string()) : null;
        if (shape != Shape.ANY && owner != null && owner.getPrincipalId() == null) {
            // The writer writes an owner without a principal ID as null, so it reads back as an absent owner.
            owner = shape == Shape.V2_BASELINE ? new UserIdentity(nonNullString()) : null;
        }
        return new S3Bucket(string(), owner, string(), tags);
    }

    private <T> List<T> list(Supplier<T> element) {
        int size = random.nextInt(4);
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(present() ? element.get() : null);
        }
        return list;
    }

    private boolean present() {
        return random.nextInt(4) != 0;
    }

    private String string() {
        return present() ? nonNullString() : null;
    }

    private String nonNullString() {
        if (random.nextInt(3) == 0) {
            StringBuilder s = new StringBuilder();
            int length = random.nextInt(8);
            for (int i = 0; i < length; i++) {
                int c;
                do {
                    c = random.nextInt(0x3000);
                } while (Character.isSurrogate((char) c));
                s.append((char) c);
            }
            return s.toString();
        }
        return STRINGS[random.nextInt(STRINGS.length)];
    }

    private Instant instant() {
        // 1970-01-01 to 2100-01-01, with millisecond, microsecond or nanosecond precision.
        long seconds = (long) (random.nextDouble() * 4_102_444_800L);
        int nanos = switch (random.nextInt(4)) {
            case 0 -> 0;
            case 1 -> random.nextInt(1000) * 1_000_000;
            case 2 -> random.nextInt(1_000_000) * 1000;
            default -> random.nextInt(1_000_000_000);
        };
        return Instant.ofEpochSecond(seconds, nanos);
    }

    private static String format(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
