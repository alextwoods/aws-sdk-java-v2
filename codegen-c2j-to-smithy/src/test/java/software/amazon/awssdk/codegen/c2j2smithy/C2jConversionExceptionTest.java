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

package software.amazon.awssdk.codegen.c2j2smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;

class C2jConversionExceptionTest {

    @Test
    void constructor_withServiceAndName_exposesBothAndNamesThemInMessage() {
        IllegalStateException cause = new IllegalStateException("boom");

        C2jConversionException e = new C2jConversionException("sqs", "Message$Body", "bad thing", cause);

        assertThat(e.service()).isEqualTo("sqs");
        assertThat(e.c2jName()).isEqualTo("Message$Body");
        assertThat(e.getMessage()).isEqualTo("Unable to convert C2J model of service sqs: Message$Body: bad thing");
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_nullServiceOrName_throwsNullPointerException() {
        assertThatThrownBy(() -> new C2jConversionException(null, "X", "r")).isInstanceOf(NullPointerException.class)
                                                                          .hasMessageContaining("service");
        assertThatThrownBy(() -> new C2jConversionException("s", null, "r")).isInstanceOf(NullPointerException.class)
                                                                          .hasMessageContaining("c2jName");
    }

    @Test
    void convert_unsupportedShapeType_throwsNamingShape() {
        C2jConversionException e = convertFailure("{'metadata': {'serviceId': 'Svc'}, 'shapes': {'Weird': {'type': "
                                                  + "'set'}}}");

        assertThat(e.service()).isEqualTo("svc");
        assertThat(e.c2jName()).isEqualTo("Weird");
        assertThat(e.getMessage()).contains("Unsupported C2J shape type 'set'");
    }

    @Test
    void convert_unsupportedLocation_throwsNamingMember() {
        C2jConversionException e = convertFailure("{'metadata': {'serviceId': 'Svc'}, 'shapes': {'In': {'type': "
                                                  + "'structure', 'members': {'A': {'shape': 'S', 'location': 'cookie'}}},"
                                                  + " 'S': {'type': 'string'}}}");

        assertThat(e.c2jName()).isEqualTo("In$A");
        assertThat(e.getMessage()).contains("Unsupported C2J location 'cookie'");
    }

    @Test
    void convert_unknownMemberTimestampFormat_throwsNamingMember() {
        C2jConversionException e = convertFailure("{'metadata': {'serviceId': 'Svc'}, 'shapes': {'In': {'type': "
                                                  + "'structure', 'members': {'T': {'shape': 'S', 'timestampFormat': "
                                                  + "'julian'}}}, 'S': {'type': 'timestamp'}}}");

        assertThat(e.c2jName()).isEqualTo("In$T");
        assertThat(e.getMessage()).contains("Unknown C2J timestampFormat 'julian'");
    }

    @Test
    void convert_unknownShapeTimestampFormat_throwsNamingShape() {
        C2jConversionException e = convertFailure("{'metadata': {'serviceId': 'Svc'}, 'shapes': {'S': {'type': "
                                                  + "'timestamp', 'timestampFormat': 'julian'}}}");

        assertThat(e.c2jName()).isEqualTo("S");
    }

    @Test
    void convert_invalidShapeName_wrapsSmithyFailureNamingShape() {
        C2jConversionException e = convertFailure("{'metadata': {'serviceId': 'Svc'}, 'shapes': {'Bad-Name': {'type': "
                                                  + "'string'}}}");

        assertThat(e.c2jName()).isEqualTo("Bad-Name");
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    void convert_operationWithInvalidInputReference_throwsNamingOperation() {
        C2jConversionException e = convertFailure("{'metadata': {'serviceId': 'Svc'}, 'operations': {'DoIt': {'input': "
                                                  + "{'shape': 'not valid'}}}}");

        assertThat(e.c2jName()).isEqualTo("DoIt");
        assertThat(e.getCause()).isNotNull();
    }

    private static C2jConversionException convertFailure(String json) {
        C2jConversionException e = catchThrowableOfType(() -> TestModels.convertJson("svc", json),
                                                        C2jConversionException.class);
        assertThat(e).isNotNull();
        return e;
    }
}
