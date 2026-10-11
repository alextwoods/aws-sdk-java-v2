# Disabled customizations (Requirement 33.7)

Every `customization.config` entry that the Codegen_Pipeline skips for a Prototype_Service_Set service, through the `disabledCustomizations` key or because the customization failed to apply (`WARN customization <name> skipped for <service>: <reason>`), is listed here with its service and reason.

| Service | Customization | Reason |
|---|---|---|
| — | — | — |

**None.** No prototype service has a `disabledCustomizations` entry, and generating the seven modules (task 1.31) logged no skipped customization. Every prototype customization applies, as `LegacyRouteParityTest` confirms by comparing the generated code with the legacy C2J route:

- SQS: `batchManagerSupported`, `customClientContextParams`, `enableGenerateCompiledEndpointRules`, `interceptors`, `verifiedSimpleMethods`
- Secrets Manager: `enableGenerateCompiledEndpointRules`, `verifiedSimpleMethods`
- Account: `enableGenerateCompiledEndpointRules`
- Route 53: `deprecatedShapes`, `enableGenerateCompiledEndpointRules`, `interceptors` (`Route53IdInterceptor`), `verifiedSimpleMethods`
- SNS: `enableGenerateCompiledEndpointRules`, `shapeModifiers` (`emitPropertyName`), `verifiedSimpleMethods`
- CloudWatch: `enableGenerateCompiledEndpointRules`, `excludedSimpleMethods`, `verifiedSimpleMethods`
- DynamoDB: `calculateCrc32FromCompressedData`, `customRetryStrategy`, `enableEndpointDiscoveryMethodRequired`, `enableEndpointProviderUriCaching`, `enableGenerateCompiledEndpointRules`, `generateDirectUnionConstructors`, `shapeModifiers` (`alternateBeanPropertyName`), `verifiedSimpleMethods`
- DynamoDB Streams (in the `dynamodb` module): the DynamoDB list without `customRetryStrategy`, `enableEndpointDiscoveryMethodRequired` and `enableEndpointProviderUriCaching`, plus `sdkModeledExceptionBaseClassName` and `shareModelConfig`

Secrets Manager and Account are generated from native Smithy models; their `customization.config` (in `src/main/resources/codegen-resources`) is still applied.
