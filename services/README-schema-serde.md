# Services on the schema-serde branch

On this branch the `services` reactor builds only the Prototype_Service_Set
(see `.kiro/specs/schema-decoupling-serde`, open decision OD-7 and Requirement 33):

| Module | Protocol |
|---|---|
| `sqs` | awsJson1_0 (awsQueryCompatible) |
| `secretsmanager` | awsJson1_1 |
| `account` | restJson1 |
| `route53` | restXml |
| `sns` | awsQuery |
| `cloudwatch` | rpcv2Cbor (also awsJson1_0, awsQuery) |
| `dynamodb` | awsJson1_0 (benchmarks) |

`secretsmanager` and `account` are generated from native Smithy models in `<module>/model/`
(copied from `aws-models/<service>/smithy/model.json`, OD-15); the other five go through the
C2J_Converter. The seven modules depend on the `schema` artifact for their generated Schema
classes (task 1.31). A non-prototype service built with the branch's codegen would need the same
dependency.

Every other service is listed in the `fleet-services` Maven profile in `services/pom.xml`,
which is off by default. The `aws-sdk-java` aggregator only depends on those services (and on
`s3-transfer-manager`, which needs `s3`) when the same profile is active.

## Non-prototype services cannot regenerate here until phase 6

Schema codegen makes the IntermediateModel Smithy-only and removes the legacy marshaller
generators. After that, the codegen on this branch can no longer generate non-prototype services
the way they are built today. Regenerating the whole fleet is phase 6 of the implementation
order, which the prototype does not deliver.

`-P fleet-services` restores the full module list, for example to inspect the reactor or to
build against the merge base. Do not expect non-prototype services to generate or compile with
the branch's codegen once the phase 1 codegen changes land.

```bash
# Default: prototype services only
mvn clean install -pl :sqs,:secretsmanager,:account,:route53,:sns,:cloudwatch,:dynamodb -P quick --am

# Full module list (not expected to regenerate on this branch)
mvn validate -P quick,fleet-services
```

## Known dependencies on non-prototype services

These modules outside `services/` still depend on non-prototype service artifacts (mostly `s3`,
`sts`, `sso` and `iam`). With the profile off, those artifacts are not built by the reactor and
must come from the local Maven repository:

- `services/sqs` (test scope: `iam`, `sts`)
- `services-custom/s3-transfer-manager` (`s3`)
- `v2-migration` (`sts`)
- `test/architecture-tests`, `test/auth-tests`, `test/http-client-benchmarks`,
  `test/module-path-tests`, `test/old-client-version-compatibility-test`, `test/s3-benchmarks`,
  `test/s3-tests`, `test/sdk-benchmarks`, `test/sdk-native-image-test`,
  `test/sdk-standard-benchmarks`, `test/stability-tests`, `test/tests-coverage-reporting`,
  `test/v2-migration-tests`

## Legacy protocol test modules

`test/protocol-tests` and `test/protocol-tests-core` (the C2J-based Legacy_Protocol_Tests) are no
longer in the root reactor; `test/smithy-protocol-tests` and `test/smithy-protocol-tests-core`
replace them (Requirement 32.9). Their sources stay on disk for the legacy disposition tool
(task 1.34). These modules still depend on the legacy artifacts, which must come from the local
Maven repository:

- `test/module-path-tests` (`protocol-tests`)
- `test/sdk-benchmarks` (`protocol-tests`)
- `test/sdk-standard-benchmarks` (`protocol-tests-core`)
