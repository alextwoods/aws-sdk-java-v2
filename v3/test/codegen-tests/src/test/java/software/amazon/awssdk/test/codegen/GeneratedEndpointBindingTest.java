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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointBuiltIn;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointPlan;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.endpoints.binding.EndpointClientValues;
import software.amazon.awssdk.endpoints.rules.EndpointTestSuite;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.GeneratedEndpointVectors;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Requirements 13.5 and 19.2: a service with an endpoint rule set gets generated endpoint parameters, a provider,
 * its canonical rule set and one parameter binding per operation from the operation's {@code staticContextParams},
 * {@code contextParam}, {@code operationContextParams}, {@code endpoint} (host prefix) and
 * {@code clientDiscoveredEndpoint} traits; every {@code operationInputs} entry of its {@code endpointTests} resolves
 * through the generated binding to the expected endpoint or error.
 */
class GeneratedEndpointBindingTest {
    private static final String PACKAGE = "software.amazon.awssdk.services.widgets";

    private static final String TESTS = """
        {"version": "1.0", "testCases": [
          {"documentation": "regional", "params": {"Region": "us-east-1"},
           "expect": {"endpoint": {"url": "https://widgets.us-east-1.amazonaws.com",
                                   "properties": {"authSchemes": [{"name": "sigv4", "signingName": "widgets",
                                                                   "signingRegion": "us-east-1"}]}}},
           "operationInputs": [{"operationName": "PutWidget", "builtInParams": {"AWS::Region": "us-east-1"},
                                "operationParams": {"AccountId": "123", "TableName": "t"}}]},
          {"documentation": "static and context parameters",
           "params": {"Region": "us-west-2", "Kind": "get", "WidgetName": "w1"},
           "expect": {"endpoint": {"url": "https://get.widgets.us-west-2.amazonaws.com/w1"}},
           "operationInputs": [{"operationName": "GetWidget", "builtInParams": {"AWS::Region": "us-west-2"},
                                "operationParams": {"name": "w1"}}]},
          {"documentation": "operation context parameter", "params": {"Region": "eu-west-1", "Names": ["a", "b"]},
           "expect": {"endpoint": {"url": "https://widgets.eu-west-1.amazonaws.com/a"}},
           "operationInputs": [{"operationName": "ListWidgets", "builtInParams": {"AWS::Region": "eu-west-1"},
                                "operationParams": {"Filters": [{"Name": "a"}, {"Name": "b"}]}}]},
          {"documentation": "client context parameter", "params": {"Region": "us-east-1", "Stage": "beta"},
           "expect": {"endpoint": {"url": "https://widgets.us-east-1.amazonaws.com/beta"}},
           "operationInputs": [{"operationName": "PutWidget", "builtInParams": {"AWS::Region": "us-east-1"},
                                "clientParams": {"Stage": "beta"},
                                "operationParams": {"AccountId": "123", "TableName": "t"}}]},
          {"documentation": "account", "params": {"Region": "us-east-1", "AccountId": "111122223333"},
           "expect": {"endpoint": {"url": "https://111122223333.widgets.us-east-1.amazonaws.com"}},
           "operationInputs": [{"operationName": "PutWidget",
                                "builtInParams": {"AWS::Region": "us-east-1", "AWS::Auth::AccountId": "111122223333"},
                                "operationParams": {"AccountId": "123", "TableName": "t"}}]},
          {"documentation": "fips", "params": {"Region": "cn-north-1", "UseFIPS": true},
           "expect": {"endpoint": {"url": "https://widgets-fips.cn-north-1.amazonaws.com.cn"}},
           "operationInputs": [{"operationName": "ListWidgets",
                                "builtInParams": {"AWS::Region": "cn-north-1", "AWS::UseFIPS": true}}]},
          {"documentation": "missing region", "params": {},
           "expect": {"error": "Invalid Configuration: Missing Region"},
           "operationInputs": [{"operationName": "ListWidgets"}]},
          {"documentation": "endpoint override", "params": {"Endpoint": "https://custom.test"},
           "expect": {"endpoint": {"url": "https://custom.test"}},
           "operationInputs": [{"operationName": "GetWidget", "builtInParams": {"SDK::Endpoint": "https://custom.test"},
                                "operationParams": {"name": "w1"}}]}
        ]}
        """;

    private static final String MODEL = """
        $version: "2"
        namespace example.widgets

        use aws.api#clientDiscoveredEndpoint
        use aws.api#clientEndpointDiscovery
        use aws.api#clientEndpointDiscoveryId
        use smithy.rules#clientContextParams
        use smithy.rules#contextParam
        use smithy.rules#endpointRuleSet
        use smithy.rules#endpointTests
        use smithy.rules#operationContextParams
        use smithy.rules#staticContextParams

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "widgets")
        @clientEndpointDiscovery(operation: DescribeEndpoints, error: InvalidEndpointException)
        @clientContextParams(Stage: {type: "string", documentation: "The deployment stage."})
        @suppress(["RuleSetAwsBuiltIn.AWS::Auth::AccountId"])
        @endpointRuleSet({
          "version": "1.0",
          "parameters": {
            "Region": {"builtIn": "AWS::Region", "required": false, "type": "String", "documentation": "Region"},
            "UseFIPS": {"builtIn": "AWS::UseFIPS", "required": true, "default": false, "type": "Boolean",
                        "documentation": "FIPS"},
            "Endpoint": {"builtIn": "SDK::Endpoint", "required": false, "type": "String", "documentation": "Endpoint"},
            "AccountId": {"builtIn": "AWS::Auth::AccountId", "required": false, "type": "String",
                          "documentation": "Account"},
            "Stage": {"required": false, "type": "String", "documentation": "Stage"},
            "Kind": {"required": false, "type": "String", "documentation": "Kind"},
            "WidgetName": {"required": false, "type": "String", "documentation": "Widget"},
            "Names": {"required": false, "type": "stringArray", "documentation": "Names"}
          },
          "rules": [
            {"conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]}],
             "endpoint": {"url": {"ref": "Endpoint"}}, "type": "endpoint"},
            {"conditions": [{"fn": "isSet", "argv": [{"ref": "Region"}]}], "type": "tree", "rules": [
              {"conditions": [{"fn": "aws.partition", "argv": [{"ref": "Region"}], "assign": "PartitionResult"}],
               "type": "tree", "rules": [
                {"conditions": [{"fn": "booleanEquals", "argv": [{"ref": "UseFIPS"}, true]}],
                 "endpoint": {"url": "https://widgets-fips.{Region}.{PartitionResult#dnsSuffix}"}, "type": "endpoint"},
                {"conditions": [{"fn": "isSet", "argv": [{"ref": "Kind"}]},
                                {"fn": "isSet", "argv": [{"ref": "WidgetName"}]}],
                 "endpoint": {"url": "https://{Kind}.widgets.{Region}.{PartitionResult#dnsSuffix}/{WidgetName}"},
                 "type": "endpoint"},
                {"conditions": [{"fn": "isSet", "argv": [{"ref": "Names"}]},
                                {"fn": "getAttr", "argv": [{"ref": "Names"}, "[0]"], "assign": "First"}],
                 "endpoint": {"url": "https://widgets.{Region}.{PartitionResult#dnsSuffix}/{First}"},
                 "type": "endpoint"},
                {"conditions": [{"fn": "isSet", "argv": [{"ref": "Stage"}]}],
                 "endpoint": {"url": "https://widgets.{Region}.{PartitionResult#dnsSuffix}/{Stage}"},
                 "type": "endpoint"},
                {"conditions": [{"fn": "isSet", "argv": [{"ref": "AccountId"}]}],
                 "endpoint": {"url": "https://{AccountId}.widgets.{Region}.{PartitionResult#dnsSuffix}"},
                 "type": "endpoint"},
                {"conditions": [],
                 "endpoint": {"url": "https://widgets.{Region}.{PartitionResult#dnsSuffix}",
                              "properties": {"authSchemes": [{"name": "sigv4", "signingName": "widgets",
                                                              "signingRegion": "{Region}"}]}},
                 "type": "endpoint"}
              ]}
            ]},
            {"conditions": [], "error": "Invalid Configuration: Missing Region", "type": "error"}
          ]
        })
        @endpointTests(%s)
        service Widgets {
            version: "2024-01-01"
            operations: [GetWidget, ListWidgets, PutWidget, DescribeEndpoints]
        }

        @staticContextParams(Kind: {value: "get"})
        @readonly
        operation GetWidget {
            input := {
                @required
                @contextParam(name: "WidgetName")
                name: String
            }
        }

        @operationContextParams(Names: {path: "Filters[*].Name"})
        @readonly
        operation ListWidgets {
            input := {
                Filters: FilterList
            }
        }

        list FilterList {
            member: Filter
        }

        structure Filter {
            Name: String
        }

        @endpoint(hostPrefix: "{AccountId}.data.")
        @clientDiscoveredEndpoint(required: true)
        operation PutWidget {
            input := {
                @required
                @hostLabel
                AccountId: String

                @clientEndpointDiscoveryId
                @required
                TableName: String
            }
            errors: [InvalidEndpointException]
        }

        operation DescribeEndpoints {
            input := {
                Operation: String
                Identifiers: Identifiers
            }
            output := {
                @required
                Endpoints: Endpoints
            }
        }

        map Identifiers {
            key: String
            value: String
        }

        list Endpoints {
            member: DiscoveredEndpoint
        }

        structure DiscoveredEndpoint {
            @required
            Address: String

            @required
            CachePeriodInMinutes: Long
        }

        @error("client")
        @httpError(421)
        structure InvalidEndpointException {
            Message: String
        }
        """.formatted(TESTS);

    @TempDir
    static Path temp;

    private static URLClassLoader loader;
    private static GeneratedEndpointVectors vectors;
    private static AwsCodegenResult result;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("widgets.smithy"), MODEL, StandardCharsets.UTF_8);
        result = AwsCodegen.generate(new AwsCodegenRequest("example.widgets#Widgets", PACKAGE, models,
                                                           temp.resolve("out")), List.of());
        loader = CompiledSources.compile(result.sourceRoot(), temp.resolve("classes")).loader();
        vectors = new GeneratedEndpointVectors(loader, PACKAGE, "Widgets");
    }

    @AfterAll
    static void close() throws Exception {
        loader.close();
    }

    @Test
    void theEndpointTypesAreGeneratedInTheServiceEndpointsPackages() {
        String root = PACKAGE.replace('.', '/') + "/endpoints/";
        for (String file : List.of("WidgetsEndpointParams.java", "WidgetsEndpointProvider.java",
                                   "internal/WidgetsEndpointRules.java")) {
            assertTrue(result.generatedFiles().stream().anyMatch(p -> p.toString().replace('\\', '/')
                                                                         .equals(root + file)),
                       root + file + " was not generated: " + result.generatedFiles());
        }
    }

    @Test
    void everyOperationInputResolvesThroughTheGeneratedBinding() {
        EndpointTestSuite suite = EndpointTestSuite.fromJson(TESTS);
        assertEquals(8, GeneratedEndpointVectors.operationInputCount(suite));
        assertEquals(List.of(), vectors.run(suite));
        // The same vectors, evaluated on their params directly, pass against the generated rule set.
        assertEquals(List.of(), suite.run(vectors.ruleSet()));
    }

    @Test
    void theGeneratedBindingsCarryTheOperationTraits() {
        Map<String, EndpointBinding> bindings = vectors.bindings();
        assertEquals(List.of("DescribeEndpoints", "GetWidget", "ListWidgets", "PutWidget"),
                     List.copyOf(bindings.keySet()));
        EndpointBinding get = bindings.get("GetWidget");
        assertEquals(Map.of("Kind", "get"), get.staticContextParams());
        assertEquals(Map.of("WidgetName", "name"), get.contextParams());
        assertEquals("Filters[*].Name",
                     bindings.get("ListWidgets").operationContextParams().get("Names").expression());

        EndpointBinding put = bindings.get("PutWidget");
        assertEquals("{AccountId}.data.", put.hostPrefix().orElseThrow().template());
        assertEquals(new EndpointBinding.Discovery(true, List.of("TableName")), put.discovery().orElseThrow());
        assertTrue(bindings.get("DescribeEndpoints").discovery().isEmpty());

        EndpointTestSuite.OperationInput input = EndpointTestSuite.fromJson(TESTS).testCases().get(0)
                                                                  .operationInputs().get(0);
        EndpointPlan plan = new EndpointPlan(vectors.ruleSet(), put);
        Endpoint endpoint = plan.resolve(vectors.input(input), GeneratedEndpointVectors.clientValues(input));
        assertEquals("https://123.data.widgets.us-east-1.amazonaws.com", endpoint.url().toString());
        assertEquals(Map.of("TableName", "t"), put.discovery().orElseThrow().ids(vectors.input(input)));
    }

    @Test
    void theRulesClassRecordsClientContextParametersAndDiscovery() {
        String rules = PACKAGE + ".endpoints.internal.WidgetsEndpointRules";
        assertEquals(Map.of("Stage", "string"), CompiledSources.invokeStatic(loader, rules, "clientContextParams"));
        assertEquals(Optional.of(ShapeId.from("example.widgets#DescribeEndpoints")),
                     CompiledSources.invokeStatic(loader, rules, "discoveryOperation"));
        assertEquals(Optional.of(ShapeId.from("example.widgets#InvalidEndpointException")),
                     CompiledSources.invokeStatic(loader, rules, "discoveryError"));
    }

    @Test
    void theGeneratedParamsAndDefaultProviderResolveTypedParameters() throws Exception {
        Class<?> paramsType = Class.forName(PACKAGE + ".endpoints.WidgetsEndpointParams", true, loader);
        Class<?> providerType = Class.forName(PACKAGE + ".endpoints.WidgetsEndpointProvider", true, loader);
        assertEquals(Region.class, paramsType.getMethod("region").getReturnType());
        assertEquals(Boolean.class, paramsType.getMethod("useFips").getReturnType());
        assertEquals(List.class, paramsType.getMethod("names").getReturnType());

        Object builder = paramsType.getMethod("builder").invoke(null);
        Class<?> builderType = builder.getClass();
        builderType.getMethod("region", Region.class).invoke(builder, Region.US_WEST_2);
        builderType.getMethod("stage", String.class).invoke(builder, "gamma");
        Object params = builderType.getMethod("build").invoke(builder);
        assertEquals(Map.of("Region", "us-west-2", "Stage", "gamma"),
                     ((EndpointParameters)
                          paramsType.getMethod("toEndpointParameters").invoke(params)).asMap());
        assertEquals(params, paramsType.getMethod("toBuilder").invoke(params).getClass().getMethod("build")
                                       .invoke(paramsType.getMethod("toBuilder").invoke(params)));

        Object provider = providerType.getMethod("defaultProvider").invoke(null);
        Method resolve = providerType.getMethod("resolveEndpoint", paramsType);
        Endpoint endpoint = ((CompletableFuture<?>) resolve.invoke(provider, params)).thenApply(Endpoint.class::cast)
                                                                                    .join();
        assertEquals("https://widgets.us-west-2.amazonaws.com/gamma", endpoint.url().toString());

        Object empty = builderType.getMethod("build").invoke(paramsType.getMethod("builder").invoke(null));
        CompletableFuture<?> failed = (CompletableFuture<?>) resolve.invoke(provider, empty);
        CompletionException e = assertThrows(CompletionException.class, failed::join);
        EndpointResolutionException cause = assertInstanceOf(EndpointResolutionException.class, e.getCause());
        assertEquals(Optional.of("Invalid Configuration: Missing Region"), cause.ruleError());
    }

    @Test
    void hostPrefixInjectionCanBeDisabled() {
        EndpointTestSuite.OperationInput input = EndpointTestSuite.fromJson(TESTS).testCases().get(0)
                                                                  .operationInputs().get(0);
        EndpointClientValues disabled = GeneratedEndpointVectors.clientValues(input).toBuilder()
                                                                .hostPrefixInjectionDisabled(true)
                                                                .builtIn(EndpointBuiltIn.USE_FIPS, false)
                                                                .build();
        Endpoint endpoint = new EndpointPlan(vectors.ruleSet(), vectors.bindings().get("PutWidget"))
            .resolve(vectors.input(input), disabled);
        assertEquals("https://widgets.us-east-1.amazonaws.com", endpoint.url().toString());
    }
}
