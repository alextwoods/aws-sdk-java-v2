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

package com.example.policy;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyWriter;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamStatement;

/** Builds a bucket access policy, writes it and reads it back; {@link #run} returns what happened. */
public final class PolicyApp {
    private PolicyApp() {
    }

    /** Runs the policy workflow; {@code endpoint} is unused (no service is called). */
    public static String run(String endpoint) {
        IamStatement read = IamStatement.builder()
                                        .sid("ReadReports")
                                        .effect(IamEffect.ALLOW)
                                        .addPrincipal(IamPrincipal.ALL)
                                        .addAction("s3:GetObject")
                                        .addResource("arn:aws:s3:::reports/*")
                                        .addCondition(IamConditionOperator.STRING_EQUALS, "aws:PrincipalOrgID",
                                                      "o-corpus")
                                        .build();
        IamStatement deny = IamStatement.builder()
                                        .sid("DenyDelete")
                                        .effect(IamEffect.DENY)
                                        .addPrincipal(IamPrincipal.ALL)
                                        .addAction("s3:DeleteObject")
                                        .addResource("arn:aws:s3:::reports/*")
                                        .build();
        IamPolicy policy = IamPolicy.builder().id("reports-access").addStatement(read).addStatement(deny).build();
        String json = policy.toJson();
        IamPolicy parsed = IamPolicy.fromJson(json);
        List<String> transcript = new ArrayList<>();
        transcript.add("json: " + json);
        transcript.add("round trip equal: " + parsed.equals(policy));
        transcript.add("statements: " + parsed.statements().size());
        transcript.add("first effect: " + parsed.statements().get(0).effect().value());
        transcript.add("pretty lines: " + policy.toJson(IamPolicyWriter.builder().prettyPrint(true).build())
                                                .split("\n").length);
        return String.join("\n", transcript);
    }
}
