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

package software.amazon.awssdk.benchmark.reference.racecar;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.benchmark.harness.CellId;
import software.amazon.awssdk.benchmark.harness.CellWorkload;
import software.amazon.awssdk.benchmark.harness.ClientDriver;
import software.amazon.awssdk.benchmark.harness.DynamoDbPutItemFixture;
import software.amazon.awssdk.benchmark.harness.PayloadClass;
import software.amazon.awssdk.benchmark.reference.v2.V2DynamoDbWorkloads;
import software.amazon.awssdk.benchmark.reference.v2.V2FiniteWorkloads;
import software.amazon.awssdk.benchmark.reference.v2.V2Transports;
import software.amazon.awssdk.core.util.VersionInfo;

/**
 * The Racecar Performance Reference arm's forked client process: the harness's shared {@link ClientDriver} over the
 * {@code feature/poc/racecar} clients built from commit {@value #RACECAR_COMMIT} and installed as
 * {@value #EXPECTED_VERSION}. Racecar is a V2 fork with V2's client API, so the workloads are the V2 reference's own
 * ({@link V2DynamoDbWorkloads}, {@link V2FiniteWorkloads}, {@link V2Transports}), compiled against the Racecar jars.
 *
 * <p>Applicability ({@link #PROTOCOLS}, {@link #PATHS}, {@link #KINDS}, {@link #TRANSPORTS}; equal to the harness's
 * {@code PerformanceReferences.RACECAR_APPLICABILITY}, which {@code RacecarReferenceApplicabilityTest} checks): what
 * the build script builds. The finite cells (every payload class, outcome and concurrency) of awsJson1.0 (DynamoDB),
 * awsJson1.1 (Kinesis), restJson1 (Lambda), awsQuery (SNS), ec2Query (EC2) and rpcv2Cbor (CloudWatch) on the
 * generated sync and async clients over every Racecar transport a cell names. Not served: restXml (no Racecar
 * Route 53 workload), the Dynamic Client paths (Racecar has none), {@code apache5-async} (Racecar's
 * {@code apache5-client} has only the sync {@code Apache5HttpClient}), and the streaming and Event Stream kinds.
 *
 * <p>Isolation: before building anything it checks that the class path's SDK is exactly {@value #EXPECTED_VERSION}
 * (a version neither released V2 nor V3 carries) and that no V3 class resolves ({@link #V3_ONLY_CLASS}); a violation
 * fails the run (record {@code error}, exit 3), never measures the wrong SDK.
 */
public final class RacecarReferenceMain {

    public static final String RACECAR_COMMIT = "ef9112895aca3057dd3a01b1731fc97ae0cdbe6e";
    /** The build script's installed version: 2.54.18-RACECAR- and the commit's first 12 characters. */
    public static final String EXPECTED_VERSION = "2.54.18-RACECAR-ef9112895aca";
    /** A class only V3 has; resolvable here means a V3 jar leaked onto the reference class path. */
    static final String V3_ONLY_CLASS = "software.amazon.awssdk.http.HttpTransportMode";

    public static final Set<String> PROTOCOLS = Set.of("awsJson1_0", "awsJson1_1", "restJson1", "awsQuery", "ec2Query",
                                                        "rpcv2Cbor");
    public static final Set<String> PATHS = Set.of("generated-sync", "generated-async");
    public static final Set<String> KINDS = Set.of("finite");
    public static final Set<String> TRANSPORTS = V2Transports.IDS;

    private RacecarReferenceMain() {
    }

    public static void main(String[] argv) throws Exception {
        ClientDriver.main(argv, new Source());
    }

    /** Throws unless this class path runs exactly the Racecar build {@value #EXPECTED_VERSION} and no V3 class. */
    static void checkIsolation() {
        if (!EXPECTED_VERSION.equals(VersionInfo.SDK_VERSION)) {
            throw new IllegalStateException("reference class path runs SDK " + VersionInfo.SDK_VERSION
                                            + ", not Racecar " + EXPECTED_VERSION);
        }
        try {
            Class.forName(V3_ONLY_CLASS, false, RacecarReferenceMain.class.getClassLoader());
        } catch (ClassNotFoundException expected) {
            return;
        }
        throw new IllegalStateException("V3 class " + V3_ONLY_CLASS + " is on the Racecar reference class path");
    }

    /** Whether this reference serves {@code cell}. */
    public static boolean appliesTo(CellId cell) {
        return PROTOCOLS.contains(cell.protocol()) && PATHS.contains(cell.path()) && KINDS.contains(cell.kind())
               && TRANSPORTS.contains(cell.transport())
               && V2Transports.isAsync(cell.transport()) == "generated-async".equals(cell.path());
    }

    static void requireApplicable(CellId cell) {
        if (!appliesTo(cell)) {
            throw new IllegalStateException("the Racecar " + EXPECTED_VERSION + " reference has no fixture for cell "
                                            + cell);
        }
    }

    static final class Source implements ClientDriver.WorkloadSource {
        @Override
        public String clientSdk() {
            return "aws-sdk-java-v2-racecar " + VersionInfo.SDK_VERSION;
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
