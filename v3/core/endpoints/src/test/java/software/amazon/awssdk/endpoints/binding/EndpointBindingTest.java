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

package software.amazon.awssdk.endpoints.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointBuiltIn;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointPlan;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.regions.Region;

class EndpointBindingTest {
    /** Every parameter echoes into the URL path so each binding's effect is visible. */
    private static final EndpointRuleSet RULES = EndpointRuleSet.fromJson("""
        {"version": "1.0",
         "parameters": {
           "Region": {"type": "String", "builtIn": "AWS::Region"},
           "Endpoint": {"type": "String", "builtIn": "SDK::Endpoint"},
           "Name": {"type": "String", "builtIn": "AWS::Auth::AccountId", "default": "default"},
           "Flag": {"type": "Boolean", "default": false},
           "Names": {"type": "stringArray"}
         },
         "rules": [
           {"conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]}], "type": "endpoint",
            "endpoint": {"url": "{Endpoint}/{Name}"}},
           {"conditions": [{"fn": "isSet", "argv": [{"ref": "Region"}]}], "type": "endpoint",
            "endpoint": {"url": "https://svc.{Region}.amazonaws.com/{Name}"}},
           {"conditions": [], "type": "error", "error": "Missing Region"}
         ]}
        """);

    private static final EndpointBinding BINDING = EndpointBinding.builder("GetThing")
                                                                  .staticContextParam("Flag", true)
                                                                  .contextParam("Name", "ThingName")
                                                                  .operationContextParam("Names", "Things[*].Name")
                                                                  .build();

    private static EndpointClientValues client(Object region) {
        return EndpointClientValues.builder().builtIn(EndpointBuiltIn.REGION, region).build();
    }

    @Test
    void theMostSpecificSourceWinsForEachParameter() {
        TestStruct input = TestStruct.of(Map.of("ThingName", "from-context",
                                                "Things", List.of(Map.of("Name", "a"), Map.of("Name", "b"))),
                                         "ThingName", "Things");
        EndpointClientValues client = EndpointClientValues.builder()
                                                          .builtIn(EndpointBuiltIn.REGION, Region.EU_WEST_1)
                                                          .builtIn(EndpointBuiltIn.ACCOUNT_ID, "from-builtin")
                                                          .clientContextParam("Name", "from-client")
                                                          .clientContextParam("Flag", false)
                                                          .build();
        EndpointParameters bound = BINDING.bind(RULES, input, client);
        assertEquals(Map.of("Region", "eu-west-1", "Name", "from-context", "Flag", true, "Names", List.of("a", "b")),
                     bound.asMap());

        TestStruct noName = TestStruct.of(Map.of(), "ThingName", "Things");
        assertEquals("from-client", BINDING.bind(RULES, noName, client).value("Name"));
        EndpointClientValues noClientParam = client.toBuilder().clientContextParam("Name", null).build();
        assertEquals("from-builtin", BINDING.bind(RULES, noName, noClientParam).value("Name"));
        // Nothing bound: the rule-set default applies during evaluation.
        Endpoint endpoint = BINDING.resolve(RULES, noName, client(Region.US_WEST_2));
        assertEquals("https://svc.us-west-2.amazonaws.com/default", endpoint.url().toString());
    }

    @Test
    void builtInValuesAreNormalizedToRuleSetTypes() {
        EndpointClientValues client = EndpointClientValues.builder()
                                                          .builtIn(EndpointBuiltIn.ENDPOINT,
                                                                   URI.create("https://override.test"))
                                                          .build();
        Endpoint endpoint = EndpointBinding.none().resolve(RULES, null, client);
        assertEquals("https://override.test/default", endpoint.url().toString());
        assertEquals("us-east-1", EndpointBinding.none().bind(RULES, null, client(Region.US_EAST_1)).value("Region"));
    }

    @Test
    void aValueOfTheWrongTypeFailsWithTheOperationAndParameter() {
        TestStruct input = TestStruct.of(Map.of("ThingName", 42), "ThingName", "Things");
        EndpointResolutionException e = assertThrows(EndpointResolutionException.class,
                                                      () -> BINDING.bind(RULES, input, client("us-east-1")));
        assertTrue(e.getMessage().contains("Name") && e.getMessage().contains("GetThing"), e.getMessage());
    }

    @Test
    void hostPrefixesAreAppliedFromHostLabelMembers() {
        EndpointBinding binding = EndpointBinding.builder("PutData").hostPrefix("{AccountId}.data.").build();
        TestStruct input = TestStruct.of(Map.of("AccountId", "123456789012"), "AccountId");
        EndpointPlan plan = new EndpointPlan(RULES, binding);
        assertEquals("https://123456789012.data.svc.us-east-1.amazonaws.com/default",
                     plan.resolve(input, client("us-east-1")).url().toString());

        EndpointClientValues disabled = client("us-east-1").toBuilder().hostPrefixInjectionDisabled(true).build();
        assertEquals("https://svc.us-east-1.amazonaws.com/default", plan.resolve(input, disabled).url().toString());

        Map<String, Object> hostile = new HashMap<>();
        hostile.put("AccountId", "evil.com/");
        assertThrows(EndpointResolutionException.class,
                     () -> plan.resolve(TestStruct.of(hostile, "AccountId"), client("us-east-1")));
        assertThrows(EndpointResolutionException.class,
                     () -> plan.resolve(TestStruct.of(Map.of(), "AccountId"), client("us-east-1")));
    }

    @Test
    void discoveryIdsComeFromTheIdentifierMembers() {
        EndpointBinding binding = EndpointBinding.builder("Query").discovery(true, List.of("Table")).build();
        EndpointBinding.Discovery discovery = binding.discovery().orElseThrow();
        assertTrue(discovery.required());
        assertEquals(Map.of("Table", "t"), discovery.ids(TestStruct.of(Map.of("Table", "t"), "Table")));
        assertEquals(Map.of(), discovery.ids(TestStruct.of(Map.of(), "Table")));
        assertSame(EndpointBinding.none(), EndpointBinding.none());
        assertTrue(EndpointBinding.none().hostPrefix().isEmpty());
    }
}
