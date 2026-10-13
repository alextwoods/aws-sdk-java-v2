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

package software.amazon.awssdk.core.client.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

class HttpMessageExchangeTest {
    @Test
    void instance_hasOnlyPrivateConstructors() {
        for (Constructor<?> constructor : HttpMessageExchange.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
        assertThat(Modifier.isFinal(HttpMessageExchange.class.getModifiers())).isTrue();
    }

    @Test
    void requestAndResponseTypes_areSdkHttpFullTypes() {
        assertThat(HttpMessageExchange.INSTANCE.requestType()).isEqualTo(SdkHttpFullRequest.class);
        assertThat(HttpMessageExchange.INSTANCE.responseType()).isEqualTo(SdkHttpFullResponse.class);
    }

    @Test
    void toString_namesBothTypes() {
        assertThat(HttpMessageExchange.INSTANCE)
            .hasToString("HttpMessageExchange(request=software.amazon.awssdk.http.SdkHttpFullRequest, "
                         + "response=software.amazon.awssdk.http.SdkHttpFullResponse)");
    }
}
