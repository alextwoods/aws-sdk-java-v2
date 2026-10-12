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

## Customizations that phase 6 must support

Customizations of services outside the Prototype_Service_Set that change a member's wire form. Phase 6 (generate all clients) must keep each wire form, or list the customization in the table above with a reason.

- Budgets (`services/budgets`, awsJson1_1): `shapeModifiers` `emitAsType: bigdecimal` on `Spend.Amount`, a C2J string shape (`NumericValue`). The generated member is a `java.math.BigDecimal`, serialized as a JSON string (`"Amount": "1177.61699999"`) and read from a JSON number (`"Amount": 1177.61699999`) without losing precision. The legacy protocol tests json-core-input.json#1 and json-core-output.json#1 (awsJson1_1) covered this through `BigDecimalMember`; they are retired as `[customization]` in `test/smithy-protocol-tests/legacy-disposition.csv` because no Supported_Protocol defines the behavior, so phase 6 needs its own test for it.
