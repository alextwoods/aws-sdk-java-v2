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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.testing.LogCapture;

public class DefaultAttributeConverterProviderTest {

    @Test
    void findConverter_whenConverterFound_logsConverterFound() {
        try (LogCapture logCaptor = new LogCapture(DefaultAttributeConverterProvider.class)) {
            DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
            provider.converterFor(EnhancedType.of(String.class));

            List<LogRecord> logEvents = logCaptor.records();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel()).isEqualTo(Level.FINE);
            assertThat(logEvents.get(0).getMessage())
                .contains("Converter for EnhancedType(java.lang.String): software.amazon.awssdk.enhanced.dynamodb.internal"
                          + ".converter.attribute.StringAttributeConverter");
        }
    }

    @Test
    void findConverter_whenConverterNotFound_logsNoConverter() {
        try (LogCapture logCaptor = new LogCapture(DefaultAttributeConverterProvider.class)) {
            DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();

            assertThatThrownBy(() -> provider.converterFor(EnhancedType.of(CustomUnsupportedType.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb"
                                      + ".DefaultAttributeConverterProviderTest$CustomUnsupportedType)");
            List<LogRecord> logEvents = logCaptor.records();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel()).isEqualTo(Level.FINE);
            assertThat(logEvents.get(0).getMessage())
                .contains("No converter available for EnhancedType(software.amazon.awssdk.enhanced.dynamodb"
                          + ".DefaultAttributeConverterProviderTest$CustomUnsupportedType)");
        }
    }

    /**
     * A custom type with no converter registered for it.
     */
    private static class CustomUnsupportedType {
    }
}
