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

package software.amazon.awssdk.benchmark.harness;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

/**
 * The V3 arm's forked client process: {@link ClientDriver} (command line, verification call, warmup/steady-state
 * gate, measurement, counters and result record) over the V3 {@link WorkloadProvider} fixtures. Reference arms run
 * the same driver with their own SDK's workloads (see {@link PerformanceReferences}).
 */
public final class ClientMain {

    /** The record schema identifier, shared by every arm. */
    public static final String SCHEMA = ClientDriver.SCHEMA;

    private ClientMain() {
    }

    public static void main(String[] argv) throws Exception {
        ClientDriver.main(argv, new V3Workloads());
    }

    /** The V3 workloads of {@link WorkloadProvider}. */
    static final class V3Workloads implements ClientDriver.WorkloadSource {

        @Override
        public String clientSdk() {
            return "aws-sdk-java-v3";
        }

        private static WorkloadProvider provider(BenchmarkCell cell) {
            return WorkloadProvider.forCell(cell).orElseThrow(
                () -> new IllegalStateException("no workload fixture for cell " + cell));
        }

        @Override
        public String fixtureName(CellId cell) {
            return provider(BenchmarkCell.parse(cell.id())).name();
        }

        @Override
        public long encodedBytesPerOperation(CellId cell) {
            BenchmarkCell c = BenchmarkCell.parse(cell.id());
            return provider(c).fixture(c).encodedBytes();
        }

        @Override
        public int requestsPerOperation(CellId cell) {
            BenchmarkCell c = BenchmarkCell.parse(cell.id());
            return provider(c).fixture(c).requestsPerOperation();
        }

        @Override
        public CellWorkload create(CellId cell, URI endpoint, Map<String, Path> models) {
            BenchmarkCell c = BenchmarkCell.parse(cell.id());
            return provider(c).create(c, endpoint, new WorkloadProvider.Options(models));
        }
    }
}
