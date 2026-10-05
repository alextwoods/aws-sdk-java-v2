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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.eventnotifications.s3.RandomNotifications.Shape;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;

/**
 * Differential test of the V3 library against the released V2 artifact of the Parity Manifest's compatibility
 * baseline ({@code v3.v2Baseline.version}; behavior entry {@code behavior:s3-event-notifications.model},
 * Requirements 16.8, 17.1, 19.12). The V2 jars are loaded in an isolated class loader, because they declare the same
 * class names, and both implementations read the same inputs:
 *
 * <ul>
 *     <li>the JSON of random notifications that V2 can represent ({@link Shape#V2_BASELINE});</li>
 *     <li>that JSON after random mutations: values replaced by other JSON values of any type, members removed, and
 *         unknown members added;</li>
 *     <li>fixed edge inputs: empty input, comments, trailing content, wrong root types, numeric edge cases,
 *         duplicate names, a byte order mark and malformed JSON.</li>
 * </ul>
 *
 * <p>For every input the outcomes must agree: the same exception class (and, for argument errors, the same message),
 * or the same {@code toString()}, {@code toJson()} and {@code toJsonPretty()}. Malformed JSON fails in both with an
 * {@link UncheckedIOException}; the parser messages differ (V2's came from Jackson). The only accepted difference is
 * the documented V3 writer correction: where V2's {@code toJson} fails on a value read from JSON (a bucket without an
 * owner, a {@code null} annotation, glacier data without restore data), V3 writes it, and what it writes reads back
 * to the same JSON.
 */
class S3EventNotificationV2DifferentialTest {
    private static final ClassLoader V2 = v2ClassLoader();

    @Provide
    Arbitrary<S3EventNotification> v2Notification() {
        return RandomNotifications.notifications(Shape.V2_BASELINE);
    }

    @Property(tries = 300)
    void fromJsonAndToJson_ofV2RepresentableNotifications_matchV2Exactly(
        @ForAll("v2Notification") S3EventNotification notification) {
        for (String json : List.of(notification.toJson(), notification.toJsonPretty())) {
            Outcome v2 = v2(json);
            Outcome v3 = v3(json);
            assertThat(v2.readError).as("V2 reads %s", json).isNull();
            assertThat(v3).as("V3 vs V2 for %s", json).isEqualTo(v2);
        }
    }

    @Property(tries = 500)
    void fromJsonAndToJson_ofMutatedMessages_matchV2(@ForAll("v2Notification") S3EventNotification notification,
                                                    @ForAll long seed) {
        Random random = new Random(seed);
        Object tree = JsonTrees.parse(notification.toJson());
        int mutations = 1 + random.nextInt(3);
        for (int i = 0; i < mutations; i++) {
            tree = mutate(tree, random);
        }
        if (random.nextBoolean()) {
            tree = JsonTrees.withUnknownMembers(tree, random);
        }
        assertSameOutcome(JsonTrees.write(tree));
    }

    @Test
    void fromJson_ofEdgeInputs_matchesV2() {
        StringBuilder deep = new StringBuilder("{\"Records\":[{\"unknown\":");
        for (int i = 0; i < 1100; i++) {
            deep.append('[');
        }
        for (int i = 0; i < 1100; i++) {
            deep.append(']');
        }
        deep.append("}]}");
        List<String> inputs = List.of(
            "", "   \n\t", "{}", "null", "[]", "\"Records\"", "{\"Records\":null}", "{\"Records\":{}}",
            "{\"Records\":[]}", "{\"Records\":[null]}", "{\"Records\":[1]}", "{\"Records\":[[]]}",
            "// a comment\n{\"Records\":[]}", "/* a\ncomment */ {\"Records\" /* x */ : []}", "{\"Records\":[]} trailing",
            "{\"Records\":[]}{\"Records\":null}", "\uFEFF{\"Records\":[]}",
            "{\"Records\":[{\"s3\":{\"object\":{\"size\":1.5}}}]}", "{\"Records\":[{\"s3\":{\"object\":{\"size\":1e3}}}]}",
            "{\"Records\":[{\"s3\":{\"object\":{\"size\":-0}}}]}",
            "{\"Records\":[{\"s3\":{\"object\":{\"size\":99999999999999999999}}}]}",
            "{\"Records\":[{\"s3\":{\"object\":{\"size\":\"12\"}}}]}",
            "{\"Records\":[{\"s3\":{\"object\":{\"hasObjectAnnotation\":\"true\"}}}]}",
            "{\"Records\":[{\"eventTime\":\"1970-01-01T00:00:00+01:00\"}]}",
            "{\"Records\":[{\"eventTime\":\"not a time\"}]}", "{\"Records\":[{\"eventTime\":12}]}",
            "{\"Records\":[{\"eventName\":\"a\",\"eventName\":\"b\",\"awsRegion\":\"r\"}]}",
            "{\"Records\":[{\"s3\":{\"bucket\":{\"awsGeneratedTags\":{\"b\":\"1\",\"a\":null,\"b\":\"2\"}}}}]}",
            "{\"Records\":[{\"s3\":{\"bucket\":{\"awsGeneratedTags\":{\"t\":{\"nested\":[1,\"x\",{}]}}}}}]}",
            "{\"Records\":[{\"s3\":{\"objectAnnotation\":[null,{\"size\":3}]}}]}", "{\"Records\":[{\"s3\":{\"bucket\":{}}}]}",
            "{\"Records\":[{\"glacierEventData\":{}}]}", "{\"Records\":[{\"glacierEventData\":{\"restoreEventData\":7}}]}",
            "{\"Records\":[{\"lifecycleEventData\":{\"transitionEventData\":null}}]}",
            "{\"Records\":[{\"userIdentity\":{\"principalId\":\"\\u00e9\\ud83d\\ude00\\n\\/\"}}]}",
            "{\"Records\":[{\"awsRegion\":\"\u00e9\u4e2d\ud83d\ude00\"}]}",
            "{\"Records\":[],}", "{\"Records\":[01]}", "{\"Records\":[\"\t\"]}", "{\"Records\":[\"\\x\"]}",
            "{\"Records\":[tru]}", "{\"Records\":[NaN]}", "{'Records':[]}", "{\"Records\":[]", "{\"Records\" []}",
            deep.toString());
        for (String input : inputs) {
            assertSameOutcome(input);
        }
    }

    private static void assertSameOutcome(String json) {
        Outcome v2 = v2(json);
        Outcome v3 = v3(json);
        assertThat(v3.readError).as("read outcome of %s", json).isEqualTo(v2.readError);
        assertThat(v3.rendering).as("toString of %s", json).isEqualTo(v2.rendering);
        if (v2.writeError == null) {
            assertThat(v3.json).as("toJson of %s", json).isEqualTo(v2.json);
            assertThat(v3.jsonPretty).as("toJsonPretty of %s", json).isEqualTo(v2.jsonPretty);
            return;
        }
        // V2 failed to write a value it read: only the documented writer corrections may differ.
        S3EventNotification read = S3EventNotification.fromJson(json);
        assertThat(hasV2WriterDefect(read)).as("V2 write failure %s for %s", v2.writeError, json).isTrue();
        assertThat(v3.writeError).as("V3 writes %s", json).isNull();
        assertThat(S3EventNotification.fromJson(v3.json).toJson()).isEqualTo(v3.json);
    }

    /** The values V2's writer failed on: a bucket without an owner, a null annotation, glacier data without restore. */
    private static boolean hasV2WriterDefect(S3EventNotification notification) {
        if (notification.getRecords() == null) {
            return false;
        }
        for (S3EventNotificationRecord rec : notification.getRecords()) {
            if (rec == null) {
                continue;
            }
            if (rec.getGlacierEventData() != null && rec.getGlacierEventData().getRestoreEventData() == null) {
                return true;
            }
            S3 s3 = rec.getS3();
            if (s3 != null && s3.getBucket() != null && s3.getBucket().getOwnerIdentity() == null) {
                return true;
            }
            if (s3 != null && s3.getObjectAnnotation() != null && s3.getObjectAnnotation().contains(null)) {
                return true;
            }
        }
        return false;
    }

    /** The result of reading {@code json} and writing what was read, or the read and write failures. */
    private record Outcome(String readError, String rendering, String json, String jsonPretty, String writeError) {
    }

    private static Outcome v3(String json) {
        S3EventNotification notification;
        try {
            notification = S3EventNotification.fromJson(json);
        } catch (RuntimeException e) {
            return new Outcome(describe(e), null, null, null, null);
        }
        try {
            return new Outcome(null, notification.toString(), notification.toJson(), notification.toJsonPretty(), null);
        } catch (RuntimeException e) {
            return new Outcome(null, notification.toString(), null, null, describe(e));
        }
    }

    private static Outcome v2(String json) {
        try {
            Class<?> type = Class.forName("software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification", true,
                                          V2);
            Object notification;
            try {
                notification = invoke(type.getMethod("fromJson", String.class), null, json);
            } catch (RuntimeException e) {
                return new Outcome(describe(e), null, null, null, null);
            }
            String string = notification.toString();
            try {
                return new Outcome(null, string, (String) invoke(type.getMethod("toJson"), notification),
                                   (String) invoke(type.getMethod("toJsonPretty"), notification), null);
            } catch (RuntimeException e) {
                return new Outcome(null, string, null, null, describe(e));
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("The V2 baseline library cannot be invoked", e);
        }
    }

    private static Object invoke(Method method, Object target, Object... arguments) throws IllegalAccessException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new AssertionError(e.getCause());
        }
    }

    /** The exception class, plus the message for argument errors (whose V2 messages V3 keeps). */
    private static String describe(RuntimeException e) {
        if (e instanceof IllegalArgumentException || e instanceof DateTimeException) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
        return e.getClass().getName();
    }

    /** One random mutation: replace a value with a random JSON value, remove a member, or replace the root. */
    @SuppressWarnings("unchecked")
    private static Object mutate(Object tree, Random random) {
        List<Object> containers = new ArrayList<>();
        collect(tree, containers);
        if (containers.isEmpty() || random.nextInt(20) == 0) {
            return JsonTrees.randomValue(random, 2);
        }
        Object container = containers.get(random.nextInt(containers.size()));
        if (container instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                ((Map<String, Object>) map).put("added", JsonTrees.randomValue(random, 2));
                return tree;
            }
            List<String> keys = new ArrayList<>(((Map<String, Object>) map).keySet());
            String key = keys.get(random.nextInt(keys.size()));
            if (random.nextInt(4) == 0) {
                map.remove(key);
            } else {
                ((Map<String, Object>) map).put(key, JsonTrees.randomValue(random, 2));
            }
        } else {
            List<Object> list = (List<Object>) container;
            if (list.isEmpty()) {
                list.add(JsonTrees.randomValue(random, 2));
            } else {
                list.set(random.nextInt(list.size()), JsonTrees.randomValue(random, 2));
            }
        }
        return tree;
    }

    private static void collect(Object value, List<Object> containers) {
        if (value instanceof Map<?, ?> map) {
            containers.add(map);
            map.values().forEach(v -> collect(v, containers));
        } else if (value instanceof List<?> list) {
            containers.add(list);
            list.forEach(v -> collect(v, containers));
        }
    }

    private static ClassLoader v2ClassLoader() {
        Path dir = Path.of(System.getProperty("v3.v2Baseline.dir", "target/v2-baseline"));
        String version = System.getProperty("v3.v2Baseline.version");
        try (Stream<Path> jars = Files.list(dir)) {
            List<Path> paths = jars.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
            assertThat(paths).as("V2 baseline jars in %s", dir)
                             .anyMatch(p -> p.getFileName().toString().equals(
                                 "s3-event-notifications-" + version + ".jar"));
            List<URL> urls = new ArrayList<>();
            for (Path p : paths) {
                urls.add(p.toUri().toURL());
            }
            return new URLClassLoader("v2-baseline", urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
