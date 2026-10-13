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

package software.amazon.awssdk.codecs.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait.Format;

class CodecSettingsTest {

    @Test
    void build_noValuesSet_usesDefaults() {
        TestSettings settings = TestSettings.builder().build();

        assertThat(settings.useTimestampFormat()).isFalse();
        assertThat(settings.defaultTimestampFormat()).isEqualTo(Format.EPOCH_SECONDS);
        assertThat(settings.defaultNamespace()).isNull();
        assertThat(settings.forbidUnknownUnionMembers()).isFalse();
        assertThat(settings.extra()).isFalse();
    }

    @Test
    void build_allValuesSet_exposesThem() {
        TestSettings settings = fullySet();

        assertThat(settings.useTimestampFormat()).isTrue();
        assertThat(settings.defaultTimestampFormat()).isEqualTo(Format.DATE_TIME);
        assertThat(settings.defaultNamespace()).isEqualTo("smithy.example");
        assertThat(settings.forbidUnknownUnionMembers()).isTrue();
        assertThat(settings.extra()).isTrue();
    }

    @Test
    void toBuilder_copiesSharedAndSubclassValues() {
        TestSettings settings = fullySet();

        TestSettings copy = settings.toBuilder().build();
        TestSettings changed = settings.toBuilder().defaultNamespace(null).build();

        assertThat(copy).isEqualTo(settings).hasSameHashCodeAs(settings).isNotSameAs(settings);
        assertThat(changed.defaultNamespace()).isNull();
        assertThat(settings.defaultNamespace()).isEqualTo("smithy.example");
    }

    @Test
    void equals_differentValuesOrClass_notEqual() {
        TestSettings settings = fullySet();

        assertThat(settings).isNotEqualTo(settings.toBuilder().useTimestampFormat(false).build());
        assertThat(settings).isNotEqualTo(settings.toBuilder().defaultTimestampFormat(Format.HTTP_DATE).build());
        assertThat(settings).isNotEqualTo(settings.toBuilder().defaultNamespace("other").build());
        assertThat(settings).isNotEqualTo(settings.toBuilder().forbidUnknownUnionMembers(false).build());
        assertThat(settings).isNotEqualTo(settings.toBuilder().extra(false).build());
        assertThat(TestSettings.builder().build()).isNotEqualTo(OtherSettings.builder().build());
        assertThat(settings).isNotEqualTo(null);
    }

    @Test
    void toString_includesClassNameAndSharedSettings() {
        assertThat(TestSettings.builder().defaultNamespace("ns").build().toString())
            .isEqualTo("TestSettings(useTimestampFormat=false, defaultTimestampFormat=epoch-seconds, "
                       + "defaultNamespace=ns, forbidUnknownUnionMembers=false, extra=false)");
        assertThat(OtherSettings.builder().build().toString())
            .isEqualTo("OtherSettings(useTimestampFormat=false, defaultTimestampFormat=epoch-seconds, "
                       + "defaultNamespace=null, forbidUnknownUnionMembers=false)");
    }

    @Test
    void defaultTimestampFormat_unknownOrNull_throws() {
        TestSettings.Builder builder = TestSettings.builder();

        assertThatThrownBy(() -> builder.defaultTimestampFormat(Format.UNKNOWN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultTimestampFormat");
        assertThatThrownBy(() -> builder.defaultTimestampFormat(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("defaultTimestampFormat must not be null");
        assertThat(builder.build().defaultTimestampFormat()).isEqualTo(Format.EPOCH_SECONDS);
    }

    private static TestSettings fullySet() {
        return TestSettings.builder()
                           .useTimestampFormat(true)
                           .defaultTimestampFormat(Format.DATE_TIME)
                           .defaultNamespace("smithy.example")
                           .forbidUnknownUnionMembers(true)
                           .extra(true)
                           .build();
    }

    /**
     * A settings subclass with one extra setting, written as a codec module would write one.
     */
    private static final class TestSettings extends CodecSettings {
        private final boolean extra;

        private TestSettings(Builder builder) {
            super(builder);
            this.extra = builder.extra;
        }

        static Builder builder() {
            return new Builder();
        }

        Builder toBuilder() {
            return new Builder(this);
        }

        boolean extra() {
            return extra;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && extra == ((TestSettings) o).extra;
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Boolean.hashCode(extra);
        }

        @Override
        public String toString() {
            return "TestSettings(" + sharedSettingsToString() + ", extra=" + extra + ")";
        }

        private static final class Builder extends CodecSettings.Builder<TestSettings, Builder> {
            private boolean extra;

            private Builder() {
            }

            private Builder(TestSettings settings) {
                super(settings);
                this.extra = settings.extra;
            }

            Builder extra(boolean extra) {
                this.extra = extra;
                return this;
            }

            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public TestSettings build() {
                return new TestSettings(this);
            }
        }
    }

    private static final class OtherSettings extends CodecSettings {
        private OtherSettings(Builder builder) {
            super(builder);
        }

        static Builder builder() {
            return new Builder();
        }

        private static final class Builder extends CodecSettings.Builder<OtherSettings, Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public OtherSettings build() {
                return new OtherSettings(this);
            }
        }
    }
}
