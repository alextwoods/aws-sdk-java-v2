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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Checks that this module's skip list is well formed. Whether each entry names an existing case is checked by the
 * protocol test generator.
 */
class ProtocolTestSkipsFileTest {

    @Test
    void skipListLoads() {
        Path file = Paths.get(ProtocolTestSkips.fileName());
        assertThat(file).exists();
        assertThat(ProtocolTestSkips.load(file).entries()).isNotNull();
    }

    /**
     * The file serves every service of this module, and the generator checks each service's entries only against that
     * service's cases, so an entry without a service would have to name a case of all of them.
     */
    @Test
    void everyEntryNamesItsService() {
        assertThat(ProtocolTestSkips.load(Paths.get(ProtocolTestSkips.fileName())).entries())
            .allSatisfy(entry -> assertThat(entry.service()).as(entry.toString()).isPresent());
    }
}
