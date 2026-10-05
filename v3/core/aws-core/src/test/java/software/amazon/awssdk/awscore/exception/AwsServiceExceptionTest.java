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

package software.amazon.awssdk.awscore.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AwsServiceExceptionTest {

    private static final AwsErrorDetails DETAILS = AwsErrorDetails.builder().errorCode("NotFound")
                                                                  .errorMessage("from details")
                                                                  .serviceName("Widgets").build();

    @Test
    void messageFollowsTheV2Format() {
        AwsServiceException exception = AwsServiceException.builder().message("gone").awsErrorDetails(DETAILS)
                                                            .statusCode(404).requestId("r1").numAttempts(2).build();
        assertEquals("gone (Service: Widgets, Status Code: 404, Request ID: r1) (SDK Attempt Count: 2)",
                     exception.getMessage());
        assertEquals("gone", exception.rawMessage());
    }

    @Test
    void messageFallsBackToTheErrorDetails() {
        AwsServiceException exception = AwsServiceException.builder().awsErrorDetails(DETAILS).statusCode(404)
                                                            .requestId("r1").extendedRequestId("x1").build();
        assertEquals("from details (Service: Widgets, Status Code: 404, Request ID: r1, Extended Request ID: x1)",
                     exception.getMessage());
        assertEquals("m", AwsServiceException.builder().message("m").build().getMessage());
    }

    @Test
    void toBuilderKeepsEveryProperty() {
        AwsServiceException exception = AwsServiceException.builder().message("m").awsErrorDetails(DETAILS)
                                                            .statusCode(500).requestId("r").build();
        AwsServiceException copy = exception.toBuilder().build();
        assertEquals(DETAILS, copy.awsErrorDetails());
        assertEquals(500, copy.statusCode());
        assertEquals("r", copy.requestId());
        assertEquals(exception.getMessage(), copy.getMessage());
        assertEquals(DETAILS, DETAILS.toBuilder().build());
        assertEquals("AwsErrorDetails(errorMessage=from details, errorCode=NotFound, serviceName=Widgets)",
                     DETAILS.toString());
    }
}
