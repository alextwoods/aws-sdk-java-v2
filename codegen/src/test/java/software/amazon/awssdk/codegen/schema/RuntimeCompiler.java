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

package software.amazon.awssdk.codegen.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.javapoet.JavaFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.utils.Validate;

/**
 * Compiles generated sources with {@code --release 8} against the runtime schema module and loads them, so tests check
 * rendered expressions against the real runtime constructors.
 */
public final class RuntimeCompiler {
    private RuntimeCompiler() {
    }

    /**
     * Writes and compiles the file under {@code dir}, then loads its class with the test class loader as parent.
     */
    static Class<?> compileAndLoad(JavaFile file, Path dir) {
        try {
            return classLoader(compileAll(Collections.singletonList(file), dir))
                .loadClass(file.packageName + "." + file.typeSpec.name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Writes and compiles the files under {@code dir} in one {@code javac} run and returns the directory of the
     * compiled classes. Load them with {@link #classLoader(Path)}, once or several times in isolated class loaders.
     */
    public static Path compileAll(List<JavaFile> files, Path dir) {
        try {
            Path sources = dir.resolve("src");
            Path classes = dir.resolve("classes");
            classes.toFile().mkdirs();
            List<String> args = new ArrayList<>(Arrays.asList("--release", "8", "-proc:none", "-classpath", classPath(),
                                                              "-d", classes.toString()));
            StringBuilder all = new StringBuilder();
            for (JavaFile file : files) {
                file.writeTo(sources);
                args.add(sources.resolve(file.packageName.replace('.', File.separatorChar))
                                .resolve(file.typeSpec.name + ".java").toString());
                all.append(file).append('\n');
            }

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int result = compiler.run(null, output, output, args.toArray(new String[0]));
            assertThat(result).as("javac output:%n%s%nsources:%n%s", new String(output.toByteArray(), StandardCharsets.UTF_8),
                                  all)
                              .isZero();
            return classes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a new class loader over compiled classes, with the test class loader as parent.
     */
    public static URLClassLoader classLoader(Path classes) {
        try {
            return new URLClassLoader(new URL[] {classes.toUri().toURL()}, RuntimeCompiler.class.getClassLoader());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String classPath() {
        Set<String> entries = new LinkedHashSet<>();
        for (Class<?> type : Arrays.asList(Trait.class, Validate.class, SdkPublicApi.class, Publisher.class)) {
            entries.add(location(type));
        }
        return String.join(File.pathSeparator, entries);
    }

    private static String location(Class<?> type) {
        try {
            return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
