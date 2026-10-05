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

package software.amazon.awssdk.endpoints.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointAuthScheme;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointResolutionException;

class EndpointRuleSetTest {
    /**
     * A regional rule set with an endpoint override, FIPS and dual-stack variants, a partition-derived auth scheme, a
     * header, escaped template braces and an error rule.
     */
    private static final String RULES = """
        {
          "version": "1.0",
          "parameters": {
            "Region": {"builtIn": "AWS::Region", "type": "String", "required": false},
            "UseFIPS": {"builtIn": "AWS::UseFIPS", "type": "Boolean", "required": true, "default": false},
            "UseDualStack": {"builtIn": "AWS::UseDualStack", "type": "Boolean", "required": true, "default": false},
            "Endpoint": {"builtIn": "SDK::Endpoint", "type": "String"},
            "Tags": {"type": "stringArray", "default": ["a"]}
          },
          "rules": [
            {"conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]}],
             "type": "endpoint", "endpoint": {"url": {"ref": "Endpoint"}}},
            {"conditions": [{"fn": "isSet", "argv": [{"ref": "Region"}]}], "type": "tree", "rules": [
              {"conditions": [{"fn": "aws.partition", "argv": [{"ref": "Region"}], "assign": "Partition"}],
               "type": "tree", "rules": [
                {"conditions": [{"fn": "booleanEquals", "argv": [{"ref": "UseFIPS"}, true]},
                                {"fn": "booleanEquals",
                                 "argv": [false, {"fn": "getAttr", "argv": [{"ref": "Partition"}, "supportsFIPS"]}]}],
                 "type": "error", "error": "FIPS is not supported in {Partition#name}"},
                {"conditions": [{"fn": "booleanEquals", "argv": [{"ref": "UseFIPS"}, true]}],
                 "type": "endpoint",
                 "endpoint": {"url": "https://svc-fips.{Region}.{Partition#dnsSuffix}",
                              "headers": {"x-tag": [{"fn": "getAttr", "argv": [{"ref": "Tags"}, "[0]"]}]}}},
                {"conditions": [{"fn": "booleanEquals", "argv": [{"ref": "UseDualStack"}, true]}],
                 "type": "endpoint",
                 "endpoint": {"url": "https://svc.{Region}.{Partition#dualStackDnsSuffix}"}},
                {"conditions": [], "type": "endpoint",
                 "endpoint": {"url": "https://svc.{Region}.{Partition#dnsSuffix}",
                              "properties": {"authSchemes": [{"name": "sigv4", "signingName": "svc",
                                                              "signingRegion": "{Region}",
                                                              "disableDoubleEncoding": true}],
                                             "literal": "{{not a template}}"}}}
              ]}
            ]},
            {"conditions": [], "type": "error", "error": "Invalid Configuration: Missing Region"}
          ]
        }
        """;

    private static final EndpointRuleSet RULE_SET = EndpointRuleSet.fromJson(RULES);

    private static Endpoint resolve(Object... nameValues) {
        EndpointParameters.Builder parameters = EndpointParameters.builder();
        for (int i = 0; i < nameValues.length; i += 2) {
            parameters.put((String) nameValues[i], nameValues[i + 1]);
        }
        return RULE_SET.resolve(parameters.build());
    }

    @Test
    void parametersAreParsedInDeclarationOrder() {
        assertEquals(List.of("Region", "UseFIPS", "UseDualStack", "Endpoint", "Tags"),
                     RULE_SET.parameters().stream().map(RuleSetParameter::name).toList());
        RuleSetParameter fips = RULE_SET.parameter("UseFIPS").orElseThrow();
        assertEquals(RuleSetParameter.Type.BOOLEAN, fips.type());
        assertEquals(Optional.of("AWS::UseFIPS"), fips.builtInId());
        assertEquals(false, fips.defaultValue());
        assertTrue(fips.required());
        assertEquals(RuleSetParameter.Type.STRING_ARRAY, RULE_SET.parameter("Tags").orElseThrow().type());
        assertEquals("1.0", RULE_SET.version());
    }

    @Test
    void regionalEndpointWithPartitionSuffixAndAuthScheme() {
        Endpoint endpoint = resolve("Region", "us-west-2");
        assertEquals("https://svc.us-west-2.amazonaws.com", endpoint.url().toString());
        EndpointAuthScheme scheme = endpoint.authSchemes().get(0);
        assertEquals("sigv4", scheme.name());
        assertEquals(Optional.of("svc"), scheme.signingName());
        assertEquals(Optional.of("us-west-2"), scheme.signingRegion());
        assertEquals(Optional.of(true), scheme.disableDoubleEncoding());
        assertEquals(Optional.empty(), scheme.disableNormalizePath());
        // A static template is the literal source text, as in the canonical evaluator.
        assertEquals("{{not a template}}", endpoint.properties().get("literal"));
    }

    @Test
    void partitionDimensionsFollowTheRegion() {
        assertEquals("https://svc.cn-north-1.amazonaws.com.cn", resolve("Region", "cn-north-1").url().toString());
        assertEquals("https://svc.eu-west-1.api.aws",
                     resolve("Region", "eu-west-1", "UseDualStack", true).url().toString());
        Endpoint fips = resolve("Region", "us-east-1", "UseFIPS", true);
        assertEquals("https://svc-fips.us-east-1.amazonaws.com", fips.url().toString());
        assertEquals(Map.of("x-tag", List.of("a")), fips.headers());
    }

    @Test
    void errorRulesReportTheirExactMessage() {
        EndpointResolutionException missing = assertThrows(EndpointResolutionException.class, () -> resolve());
        assertEquals(Optional.of("Invalid Configuration: Missing Region"), missing.ruleError());
        assertEquals(false, missing.retryable());
    }

    @Test
    void endpointOverrideWinsAndConditionAssignmentsAreScopedToTheirRule() {
        assertEquals("https://example.test", resolve("Endpoint", "https://example.test").url().toString());
        assertEquals("https://svc.us-east-1.amazonaws.com", resolve("Region", "us-east-1").url().toString());
    }

    @Test
    void wronglyTypedParameterValuesAreRejected() {
        EndpointResolutionException e = assertThrows(EndpointResolutionException.class,
                                                      () -> resolve("UseFIPS", "true"));
        assertTrue(e.getMessage().contains("UseFIPS"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> EndpointParameters.builder().put("X", 1));
    }

    @Test
    void aTreeRuleWithoutAMatchingRuleIsAnInvalidRuleSet() {
        EndpointRuleSet rules = EndpointRuleSet.fromJson("""
            {"version": "1.0", "parameters": {},
             "rules": [{"conditions": [], "type": "tree",
                        "rules": [{"conditions": [{"fn": "booleanEquals", "argv": [true, false]}],
                                   "type": "error", "error": "never"}]}]}
            """);
        EndpointResolutionException e = assertThrows(EndpointResolutionException.class,
                                                      () -> rules.resolve(EndpointParameters.empty()));
        assertEquals(Optional.empty(), e.ruleError());
    }

    @Test
    void invalidRuleSetsAreRejectedWhenParsed() {
        assertThrows(IllegalArgumentException.class, () -> EndpointRuleSet.fromJson(
            "{\"parameters\": {}, \"rules\": [{\"type\": \"endpoint\", \"conditions\": "
            + "[{\"fn\": \"noSuchFunction\", \"argv\": []}], \"endpoint\": {\"url\": \"https://x\"}}]}"));
        assertThrows(IllegalArgumentException.class, () -> EndpointRuleSet.fromJson(
            "{\"parameters\": {}, \"rules\": [{\"type\": \"endpoint\", \"conditions\": [], "
            + "\"endpoint\": {\"url\": \"https://{Region\"}}]}"));
        assertThrows(IllegalArgumentException.class, () -> EndpointRuleSet.fromJson(
            "{\"parameters\": {\"P\": {\"type\": \"Boolean\", \"default\": \"x\"}}, \"rules\": []}"));
    }

    @Test
    void chunkedRuleSetsParseAsTheWholeDocument() {
        int middle = RULES.length() / 2;
        EndpointRuleSet chunked = EndpointRuleSet.fromJsonChunks(RULES.substring(0, middle), RULES.substring(middle));
        assertEquals(RULE_SET.resolve(EndpointParameters.builder().put("Region", "us-west-2").build()),
                     chunked.resolve(EndpointParameters.builder().put("Region", "us-west-2").build()));
    }

    @Test
    void theTestSuiteRunnerComparesEndpointsAndErrorsExactly() {
        EndpointTestSuite suite = EndpointTestSuite.fromJson("""
            {"version": "1.0", "testCases": [
              {"documentation": "ok", "params": {"Region": "us-west-2"},
               "expect": {"endpoint": {"url": "https://svc.us-west-2.amazonaws.com",
                                       "properties": {"authSchemes": [{"name": "sigv4", "signingName": "svc",
                                                                       "signingRegion": "us-west-2",
                                                                       "disableDoubleEncoding": true}],
                                                      "literal": "{{not a template}}"}}}},
              {"documentation": "error", "params": {}, "expect": {"error": "Invalid Configuration: Missing Region"}},
              {"documentation": "wrong url", "params": {"Region": "us-west-2"},
               "expect": {"endpoint": {"url": "https://svc.us-west-2.amazonaws.com"}}},
              {"documentation": "wrong error", "params": {}, "expect": {"error": "other"}},
              {"documentation": "with inputs", "params": {"Region": "us-east-1"},
               "operationInputs": [{"operationName": "Op", "builtInParams": {"AWS::Region": "us-east-1"}}],
               "expect": {"error": "never"}}
            ]}
            """);
        List<String> failures = suite.run(RULE_SET);
        assertEquals(3, failures.size(), failures::toString);
        assertTrue(failures.get(0).startsWith("test case 2 (wrong url)"), failures.get(0));
        assertTrue(failures.get(1).startsWith("test case 3 (wrong error)"), failures.get(1));
        EndpointTestSuite.OperationInput input = suite.testCases().get(4).operationInputs().get(0);
        assertEquals("Op", input.operationName());
        assertEquals(Map.of("AWS::Region", "us-east-1"), input.builtInParams());
    }
}
