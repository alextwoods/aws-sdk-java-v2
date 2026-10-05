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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;

/**
 * The core-runtime verification gate (task 11.1): every required suite of tasks 6-10 ran in this Maven session, executed
 * tests, and passed. Enabled only by this module's core-runtime-gate profile ({@code -Dv3.aggregate=core-runtime}),
 * which also orders the build of every suite-holding module before this one; see {@link CoreRuntimeSuiteReports} for
 * how report freshness is determined.
 */
@EnabledIfSystemProperty(named = "v3.coreRuntimeGate", matches = "true")
class CoreRuntimeSuiteReportsTest {
    static final String SUITE_LIST = "core-runtime-suites.txt";

    @Test
    void everyRequiredCoreRuntimeSuiteRanAndPassedInThisBuild() {
        Path v3Root = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
        Instant buildStart = CoreRuntimeSuiteReports.parseBuildStart(System.getProperty("v3.buildStartTime"));
        List<Suite> suites = requiredSuites();
        List<String> problems = CoreRuntimeSuiteReports.problems(v3Root, suites, buildStart);
        assertEquals(List.of(), problems, () -> problems.size() + " of " + suites.size()
                                                + " required core-runtime suites did not run and pass in this build "
                                                + "(started " + buildStart + "):\n  " + String.join("\n  ", problems));
    }

    /** The required suites, read from the checked-in suite list on the test classpath. */
    static List<Suite> requiredSuites() {
        URL resource = CoreRuntimeSuiteReportsTest.class.getClassLoader().getResource(SUITE_LIST);
        if (resource == null) {
            throw new IllegalStateException(SUITE_LIST + " is not on the test classpath");
        }
        try {
            return CoreRuntimeSuiteReports.parse(Path.of(resource.toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to locate " + resource, e);
        }
    }
}
