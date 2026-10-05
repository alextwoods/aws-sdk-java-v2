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

package software.amazon.awssdk.protocols.restjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamCodec;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.eventstream.Event;
import software.amazon.smithy.protocoltests.traits.eventstream.EventStreamTestCase;
import software.amazon.smithy.protocoltests.traits.eventstream.EventStreamTestsTrait;
import software.amazon.smithy.protocoltests.traits.eventstream.EventType;

/**
 * Runs the official {@code smithy.test#eventStreamTests} client cases of {@code restJson1}
 * (smithy-aws-protocol-tests) against the event stream SERDE: request events are encoded from their params and
 * compared with the official frame (headers as a set, since header order is not significant and the vectors order
 * them as the params do; payload bytes exactly); response events are decoded from the official frame and compared
 * with their params, or must fail as expected. Cases whose subject is the initial HTTP
 * request/response rather than event framing are skipped with their reason.
 */
class RestJsonEventStreamConformanceTest {
    private static final String INITIAL_MESSAGE = "initial HTTP request/response cases exercise the operation's HTTP "
                                                  + "bindings and error runners, not event framing";
    private static final String REQUIRED_INITIAL = "the case checks that the initial HTTP response carries a "
                                                   + "@required httpHeader member, which is initial-response binding "
                                                   + "validation, not event framing";
    private static final java.util.Set<String> REQUIRED_INITIAL_CASES =
        java.util.Set.of("MissingRequiredInitialResponseOutput", "DuplexMissingRequiredInitialResponseOutput");
    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static Model model;

    @BeforeAll
    static void loadModels() {
        model = Model.assembler(RestJsonEventStreamConformanceTest.class.getClassLoader())
                     .discoverModels(RestJsonEventStreamConformanceTest.class.getClassLoader())
                     .assemble()
                     .unwrap();
    }

    @TestFactory
    Stream<DynamicTest> officialRestJsonEventStreamTests() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.findTrait(ProtocolIds.REST_JSON_1).isEmpty()) {
                continue;
            }
            DynamicService dynamic = new DynamicService(model, service.getId());
            for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
                Optional<EventStreamTestsTrait> trait = operation.getTrait(EventStreamTestsTrait.class);
                if (trait.isEmpty()) {
                    continue;
                }
                OperationSchemas schemas = dynamic.schemas(operation);
                for (EventStreamTestCase test : trait.get().getTestCases()) {
                    if (!test.getProtocol().equals(ProtocolIds.REST_JSON_1)
                        || test.getAppliesTo().filter(a -> a == AppliesTo.SERVER).isPresent()) {
                        continue;
                    }
                    tests.add(DynamicTest.dynamicTest(test.getId(), () -> run(dynamic, schemas, test)));
                }
            }
        }
        assertFalse(tests.isEmpty(), "no eventStreamTests found");
        return tests.stream();
    }

    private static void run(DynamicService dynamic, OperationSchemas schemas, EventStreamTestCase test) {
        if (test.getInitialRequest().isPresent() || test.getInitialResponse().isPresent()
            || test.getInitialRequestParams().isPresent() || test.getInitialResponseParams().isPresent()) {
            Assumptions.abort(INITIAL_MESSAGE);
        }
        if (REQUIRED_INITIAL_CASES.contains(test.getId())) {
            Assumptions.abort(REQUIRED_INITIAL);
        }
        boolean failure = test.getExpectation().getFailure().isPresent();
        for (Event event : test.getEvents()) {
            boolean request = event.getType() == EventType.REQUEST;
            Schema member = (request ? schemas.inputStreamMember() : schemas.outputStreamMember()).orElseThrow();
            EventStreamCodec codec = EventStreamCodec.of(new WireContext(ProtocolIds.REST_JSON_1, schemas.id()),
                                                         member, EventPayloadCodec.of(JSON, "application/json"),
                                                         null);
            byte[] expected = event.getBytes().orElseThrow();
            if (request) {
                SerializableStruct value = dynamic.value(dynamic.model().expectShape(member.memberTarget().id()),
                                                         event.getParams().orElseThrow());
                Message encoded = codec.encodeEvent(value);
                assertEquals(new MessageDecoder().decode(ByteBuffer.wrap(expected)), List.of(encoded),
                             test.getId() + ": encoded frame");
                assertEquals(expected.length, bytes(MessageEncoder.encode(encoded)).length, test.getId());
                continue;
            }
            List<Message> messages = new MessageDecoder().decode(ByteBuffer.wrap(expected));
            assertEquals(1, messages.size());
            if (failure) {
                RuntimeException e = assertThrows(RuntimeException.class,
                                                  () -> codec.decodeEvent(messages.get(0), null), test.getId());
                test.getExpectation().getFailure().get().getErrorId().ifPresent(errorId -> {
                    AwsServiceException error = assertInstanceOf(AwsServiceException.class, e);
                    Schema errorMember = member.memberTarget().member(error.awsErrorDetails().errorCode());
                    assertEquals(errorId, errorMember.memberTarget().id(), test.getId());
                });
                if (test.getExpectation().getFailure().get().getErrorId().isEmpty()
                    && !(e instanceof AwsServiceException) && !(e instanceof SdkProtocolException)) {
                    fail(test.getId() + ": unexpected failure type " + e.getClass().getName());
                }
                continue;
            }
            SerializableStruct decoded = codec.decodeEvent(messages.get(0), null);
            SerializableStruct params = dynamic.value(dynamic.model().expectShape(member.memberTarget().id()),
                                                      event.getParams().orElseThrow());
            assertEquals(DynamicService.canonical(params), DynamicService.canonical(decoded), test.getId());
            assertEquals(messages, List.of(codec.encodeEvent(decoded)), test.getId() + ": re-encoded frame");
        }
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }
}
