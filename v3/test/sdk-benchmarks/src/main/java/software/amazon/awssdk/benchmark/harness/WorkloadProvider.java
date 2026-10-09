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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Supplies the fixture (mock-service side) and workload (client side) of the cells it covers. New protocols, services,
 * streaming and Event Stream fixtures are added as providers in {@link #PROVIDERS}; the cell matrix does not change.
 */
public interface WorkloadProvider {

    /** Every registered provider, in lookup order. */
    List<WorkloadProvider> PROVIDERS = List.of(new DynamoDbWorkloads(), new KinesisWorkloads(),
                                                new LambdaWorkloads(), new Route53Workloads(), new SnsWorkloads(),
                                                new Ec2Workloads(), new CloudWatchWorkloads(),
                                                new MediaStoreDataWorkloads(), new S3Workloads(),
                                                new LambdaEventStreamWorkloads(), new S3SelectWorkloads(),
                                                new KinesisEventStreamWorkloads());

    /** A short name recorded with every result, e.g. {@code dynamodb-putitem}. */
    String name();

    boolean supports(BenchmarkCell cell);

    ServiceFixture fixture(BenchmarkCell cell);

    CellWorkload create(BenchmarkCell cell, URI endpoint, Options options);

    /**
     * Client-process options a provider may need.
     *
     * @param models model files by service name (e.g. {@code dynamodb}), for the Dynamic Client paths
     */
    record Options(Map<String, Path> models) {
        public Options {
            models = Map.copyOf(models);
        }
    }

    /** The first provider supporting {@code cell}, or empty if the cell has no fixture yet. */
    static Optional<WorkloadProvider> forCell(BenchmarkCell cell) {
        return PROVIDERS.stream().filter(p -> p.supports(cell)).findFirst();
    }
}
