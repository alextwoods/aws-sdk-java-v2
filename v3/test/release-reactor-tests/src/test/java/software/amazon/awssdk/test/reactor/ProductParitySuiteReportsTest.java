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

package software.amazon.awssdk.test.reactor;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;

/**
 * The product-parity verification gate (task 17.1): every required suite of tasks 12-16 (dynamic isolation and
 * equivalence, fleet, Parity Manifest and packaging-stage launch gate, high-level libraries, migration corpus,
 * excluded-API scans, metadata/telemetry/logging/redaction) ran in this Maven session, executed tests, and passed.
 * Enabled only by this module's product-parity-gate profile ({@code -Dv3.aggregate=product-parity}), which also
 * enables {@link CoreRuntimeSuiteReportsTest} and orders the build of every module of both suite lists before this
 * one.
 */
@EnabledIfSystemProperty(named = "v3.productParityGate", matches = "true")
class ProductParitySuiteReportsTest {
    static final String SUITE_LIST = "product-parity-suites.txt";

    @Test
    void everyRequiredProductParitySuiteRanAndPassedInThisBuild() {
        CoreRuntimeSuiteReportsTest.assertRanAndPassed("product-parity", requiredSuites());
    }

    /** The required product-parity suites, read from the checked-in suite list on the test classpath. */
    static List<Suite> requiredSuites() {
        return CoreRuntimeSuiteReportsTest.suites(SUITE_LIST);
    }
}
