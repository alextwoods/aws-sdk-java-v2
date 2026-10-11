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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class StubbedResponseTest {

    @Test
    void cborTestCaseBodyIsBase64Decoded() {
        StubbedResponse r = StubbedResponse.fromTestCase(200, Collections.singletonMap("smithy-protocol", "rpc-v2-cbor"),
                                                         "oA==", "application/cbor");
        assertThat(r.body()).containsExactly(0xa0);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers()).containsEntry("smithy-protocol", "rpc-v2-cbor");
    }

    @Test
    void otherTestCaseBodiesAreUtf8() {
        StubbedResponse r = StubbedResponse.fromTestCase(400, Collections.emptyMap(), "{\"__type\":\"Err\"}",
                                                         "application/json");
        assertThat(new String(r.body(), StandardCharsets.UTF_8)).isEqualTo("{\"__type\":\"Err\"}");
    }

    @Test
    void absentBodyIsEmpty() {
        assertThat(StubbedResponse.fromTestCase(204, Collections.emptyMap(), null, null).body()).isEmpty();
        assertThat(StubbedResponse.create(204, Collections.emptyMap(), null).body()).isEmpty();
    }

    @Test
    void bodyIsDefensivelyCopied() {
        byte[] body = {1, 2};
        StubbedResponse r = StubbedResponse.create(200, Collections.emptyMap(), body);
        body[0] = 9;
        r.body()[1] = 9;
        assertThat(r.body()).containsExactly(1, 2);
    }
}
