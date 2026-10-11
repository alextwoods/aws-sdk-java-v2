# C2J conversion gap reports

One `ConversionGapReport` per C2J model of the Prototype_Service_Set services that go through the C2J_Converter (OD-15, Requirement 17.10): SQS, Route 53, SNS, CloudWatch and DynamoDB (plus DynamoDB Streams, which is a second C2J model in the `dynamodb` module). Secrets Manager and Account are generated from native Smithy models (`services/<module>/model/model.json`), so the converter does not run for them and they have no report.

Each entry names a C2J property that has no standard Smithy or AWS trait, the services it affects, its generated-code impact, and how often it occurs. How the intermediate model does without each one is described in the `codegen-c2j-to-smithy` and `codegen` sections of `../porting-deviations.md`.

The files are copies of `services/<module>/target/c2j-conversion-gaps/<service>.json`, which the Codegen_Pipeline writes on every converter run. To refresh them, build the modules (`mvn clean install -pl :sqs,:route53,:sns,:cloudwatch,:dynamodb -P quick`) and copy the files here.
