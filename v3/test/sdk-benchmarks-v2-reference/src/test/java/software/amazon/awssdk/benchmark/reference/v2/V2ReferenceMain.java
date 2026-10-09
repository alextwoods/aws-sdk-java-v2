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

package software.amazon.awssdk.benchmark.reference.v2;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.benchmark.harness.CellId;
import software.amazon.awssdk.benchmark.harness.CellWorkload;
import software.amazon.awssdk.benchmark.harness.ClientDriver;
import software.amazon.awssdk.benchmark.harness.DynamoDbPutItemFixture;
import software.amazon.awssdk.benchmark.harness.PayloadClass;
import software.amazon.awssdk.core.util.VersionInfo;

/**
 * The Launch Parity Baseline arm's forked client process: the harness's shared {@link ClientDriver} (the V3 arm's
 * command line, verification call, steady-state warmup gate, measurement, counters and result record) over the
 * released V2 clients of the V3 harness's finite fixtures ({@link V2DynamoDbWorkloads}, {@link V2FiniteWorkloads}).
 *
 * <p>Applicability ({@link #PROTOCOLS}, {@link #PATHS}, {@link #KINDS}, {@link #TRANSPORTS}; equal to the harness's
 * {@code PerformanceReferences.V2_APPLICABILITY}, which {@code V2ReferenceApplicabilityTest} checks): the finite cells
 * (every payload class, outcome and concurrency of the matrix) of awsJson1.0 (DynamoDB {@code PutItem}), awsJson1.1
 * (Kinesis {@code PutRecord}), restJson1 (Lambda {@code UpdateFunctionConfiguration}), awsQuery (SNS
 * {@code Publish}), ec2Query (EC2 {@code ImportKeyPair}) and rpcv2Cbor (CloudWatch {@code PutDashboard}) on the
 * generated sync and async clients over every V2 transport whose class the cell names. Any other cell is refused
 * before anything is built. Not served: restXml (the fixture's Route 53 client is not available at
 * {@value #EXPECTED_VERSION} to this build), the Dynamic Client paths (V2 has none), {@code apache5-async} (V2 has no
 * Apache 5 async client), and the streaming and Event Stream kinds.
 *
 * <p>Isolation: before building anything it checks that the class path's SDK is exactly V2
 * {@value #EXPECTED_VERSION} and that no V3 class is resolvable ({@link #V3_ONLY_CLASS}); a violation fails the run
 * (record {@code error}, exit 3), never measures the wrong SDK.
 */
public final class V2ReferenceMain {

    public static final String EXPECTED_VERSION = "2.54.0";
    /** A class only V3 has; resolvable here means a V3 jar leaked onto the reference class path. */
    static final String V3_ONLY_CLASS = "software.amazon.awssdk.http.HttpTransportMode";

    public static final Set<String> PROTOCOLS = Set.of("awsJson1_0", "awsJson1_1", "restJson1", "awsQuery", "ec2Query",
                                                        "rpcv2Cbor");
    public static final Set<String> PATHS = Set.of("generated-sync", "generated-async");
    public static final Set<String> KINDS = Set.of("finite");
    public static final Set<String> TRANSPORTS = V2Transports.IDS;

    private V2ReferenceMain() {
    }

    public static void main(String[] argv) throws Exception {
        ClientDriver.main(argv, new Source());
    }

    /** Throws unless this class path runs exactly V2 {@value #EXPECTED_VERSION} and no V3 class. */
    static void checkIsolation() {
        if (!EXPECTED_VERSION.equals(VersionInfo.SDK_VERSION)) {
            throw new IllegalStateException("reference class path runs SDK " + VersionInfo.SDK_VERSION + ", not V2 "
                                            + EXPECTED_VERSION);
        }
        try {
            Class.forName(V3_ONLY_CLASS, false, V2ReferenceMain.class.getClassLoader());
        } catch (ClassNotFoundException expected) {
            return;
        }
        throw new IllegalStateException("V3 class " + V3_ONLY_CLASS + " is on the V2 reference class path");
    }

    /** Whether this reference serves {@code cell}. */
    public static boolean appliesTo(CellId cell) {
        return PROTOCOLS.contains(cell.protocol()) && PATHS.contains(cell.path()) && KINDS.contains(cell.kind())
               && TRANSPORTS.contains(cell.transport())
               && V2Transports.isAsync(cell.transport()) == "generated-async".equals(cell.path());
    }

    static void requireApplicable(CellId cell) {
        if (!appliesTo(cell)) {
            throw new IllegalStateException("the V2 " + EXPECTED_VERSION + " reference has no fixture for cell "
                                            + cell);
        }
    }

    static final class Source implements ClientDriver.WorkloadSource {
        @Override
        public String clientSdk() {
            return "aws-sdk-java-v2 " + VersionInfo.SDK_VERSION;
        }

        @Override
        public String fixtureName(CellId cell) {
            requireApplicable(cell);
            return isDynamoDb(cell) ? "dynamodb-putitem" : V2FiniteWorkloads.fixtureName(cell.protocol());
        }

        @Override
        public long encodedBytesPerOperation(CellId cell) {
            requireApplicable(cell);
            return isDynamoDb(cell) ? new DynamoDbPutItemFixture(PayloadClass.fromId(cell.payload())).encodedBytes()
                                    : V2FiniteWorkloads.encodedBytes(cell);
        }

        @Override
        public int requestsPerOperation(CellId cell) {
            requireApplicable(cell);
            return 1;
        }

        @Override
        public CellWorkload create(CellId cell, URI endpoint, Map<String, Path> models) {
            requireApplicable(cell);
            checkIsolation();
            return isDynamoDb(cell) ? V2DynamoDbWorkloads.create(cell, endpoint)
                                    : V2FiniteWorkloads.create(cell, endpoint);
        }

        private static boolean isDynamoDb(CellId cell) {
            return "awsJson1_0".equals(cell.protocol());
        }
    }
}
