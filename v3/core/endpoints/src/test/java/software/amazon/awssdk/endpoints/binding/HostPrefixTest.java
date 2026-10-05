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

package software.amazon.awssdk.endpoints.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.endpoints.EndpointResolutionException;

class HostPrefixTest {

    @Test
    void staticAndLabelledPrefixesArePrependedToTheHost() {
        HostPrefix query = HostPrefix.parse("query-");
        assertEquals(List.of(), query.labels());
        assertEquals(URI.create("https://query-svc.us-east-1.amazonaws.com/path?x=1"),
                     query.apply(URI.create("https://svc.us-east-1.amazonaws.com/path?x=1"), Map.of()));

        HostPrefix account = HostPrefix.parse("{AccountId}.{Kind}.");
        assertEquals(List.of("AccountId", "Kind"), account.labels());
        assertEquals(URI.create("http://user@123.cp.localhost:8080/"),
                     account.apply(URI.create("http://user@localhost:8080/"), Map.of("AccountId", "123", "Kind",
                                                                                    "cp")));
    }

    @Test
    void labelsMustBeValidHostLabels() {
        HostPrefix prefix = HostPrefix.parse("{Label}.");
        URI endpoint = URI.create("https://svc.test");
        for (String invalid : List.of("", "a.b", "a/b", "a b", "x".repeat(64), "-", "a@b")) {
            assertThrows(EndpointResolutionException.class, () -> prefix.apply(endpoint, Map.of("Label", invalid)),
                         invalid);
        }
        assertThrows(EndpointResolutionException.class, () -> prefix.apply(endpoint, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> HostPrefix.parse("{}."));
        assertThrows(IllegalArgumentException.class, () -> HostPrefix.parse("{Label."));
    }
}
