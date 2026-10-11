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

package software.amazon.awssdk.archtests.portcompliance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.codehaus.mojo.animal_sniffer.Clazz;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;
import org.codehaus.mojo.animal_sniffer.logging.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Scans compiled classes (a directory or a jar) for constructs that break the Java 8 porting rules of the
 * schema-serde work (Requirements 1.3, 1.4, 1.5, 1.8):
 *
 * <ul>
 *     <li>class file major version above 52 (Java 8);</li>
 *     <li>records ({@code java/lang/Record} superclass or a {@code Record} attribute);</li>
 *     <li>sealed types ({@code PermittedSubclasses} attribute);</li>
 *     <li>any reference to {@code java.util.concurrent.Flow} or its nested types;</li>
 *     <li>any reference to a JDK class, method or field that is not in the Java 8 API, checked with animal-sniffer
 *     against the {@code org.codehaus.mojo.signature:java18} signature;</li>
 *     <li>SDK visibility annotations: a public top-level type must carry exactly one of {@code @SdkPublicApi},
 *     {@code @SdkProtectedApi}, {@code @SdkInternalApi}; every other type (nested, local, anonymous, non-public)
 *     may carry at most one. Nested types inherit the visibility of their enclosing type, as elsewhere in the SDK.
 *     {@code package-info} and {@code module-info} are not checked.</li>
 * </ul>
 */
public final class PortComplianceChecker {

    static final int JAVA_8_MAJOR_VERSION = 52;

    private static final Set<String> VISIBILITY_ANNOTATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "Lsoftware/amazon/awssdk/annotations/SdkPublicApi;",
        "Lsoftware/amazon/awssdk/annotations/SdkProtectedApi;",
        "Lsoftware/amazon/awssdk/annotations/SdkInternalApi;")));

    /**
     * Package prefixes (internal form) that belong to the JDK. References outside them are third-party or SDK
     * classes and are not checked against the Java 8 signature.
     */
    private static final List<String> JDK_PACKAGE_PREFIXES = Collections.unmodifiableList(Arrays.asList(
        "java/", "javax/", "jdk/", "sun/", "com/sun/", "org/w3c/dom/", "org/xml/sax/", "org/ietf/jgss/", "org/omg/"));

    private static final String FLOW = "java/util/concurrent/Flow";

    private final Map<String, Clazz> java8Signature;

    public PortComplianceChecker(Map<String, Clazz> java8Signature) {
        this.java8Signature = java8Signature;
    }

    /**
     * Load the animal-sniffer {@code java18} API signature.
     */
    public static PortComplianceChecker fromSignature(InputStream java18Signature) throws IOException {
        return new PortComplianceChecker(SignatureChecker.loadClasses(java18Signature));
    }

    /**
     * Check every class file under {@code root} (a directory of class files or a jar) and return one message per
     * violation. An empty list means the classes comply.
     */
    public List<String> check(Path root) throws IOException {
        Map<String, byte[]> classFiles = readClassFiles(root);
        List<String> violations = new ArrayList<>();
        Set<String> nonJdkPackages = new TreeSet<>();

        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            ClassReader reader = new ClassReader(entry.getValue());
            ClassFacts facts = new ClassFacts();
            ReferenceCollector references = new ReferenceCollector();
            reader.accept(new ClassRemapper(facts, references), 0);

            String name = reader.getClassName();
            checkClass(name, facts, references.referenced, violations);
            for (String referenced : references.referenced) {
                if (!isJdk(referenced)) {
                    nonJdkPackages.add(packageOf(referenced));
                }
            }
        }

        violations.addAll(checkJava8Api(root, nonJdkPackages));
        Collections.sort(violations);
        return violations;
    }

    private static void checkClass(String name, ClassFacts facts, Set<String> referenced, List<String> violations) {
        String javaName = name.replace('/', '.');
        if (facts.majorVersion > JAVA_8_MAJOR_VERSION) {
            violations.add(javaName + ": class file major version " + facts.majorVersion
                           + " is above Java 8 (" + JAVA_8_MAJOR_VERSION + ")");
        }
        if (facts.record) {
            violations.add(javaName + ": is a record; use a final class (porting conventions)");
        }
        if (facts.sealed) {
            violations.add(javaName + ": is a sealed type; use package-private constructors instead");
        }
        Set<String> flowTypes = referenced.stream()
                                          .filter(r -> r.equals(FLOW) || r.startsWith(FLOW + "$"))
                                          .map(r -> r.replace('/', '.'))
                                          .collect(Collectors.toCollection(TreeSet::new));
        if (!flowTypes.isEmpty()) {
            violations.add(javaName + ": references " + flowTypes + "; use org.reactivestreams instead");
        }
        checkVisibilityAnnotations(name, javaName, facts, violations);
    }

    private static void checkVisibilityAnnotations(String name, String javaName, ClassFacts facts,
                                                   List<String> violations) {
        if (name.endsWith("/package-info") || name.equals("module-info")) {
            return;
        }
        if (facts.visibilityAnnotations.size() > 1) {
            violations.add(javaName + ": has more than one SDK visibility annotation " + facts.visibilityAnnotations);
            return;
        }
        boolean topLevelPublic = !facts.nested
                                 && (facts.access & Opcodes.ACC_PUBLIC) != 0
                                 && (facts.access & Opcodes.ACC_SYNTHETIC) == 0;
        if (topLevelPublic && facts.visibilityAnnotations.isEmpty()) {
            violations.add(javaName + ": public top-level type has no SDK visibility annotation "
                           + "(@SdkPublicApi, @SdkProtectedApi or @SdkInternalApi)");
        }
    }

    private List<String> checkJava8Api(Path root, Set<String> ignoredPackages) throws IOException {
        List<String> errors = new ArrayList<>();
        // animal-sniffer matches wildcard ignores ("a.b.*") against referenced class names; exact entries are not
        // package names, so every non-JDK package is passed as a wildcard.
        Set<String> ignores = ignoredPackages.stream()
                                             .map(p -> p.replace('/', '.') + ".*")
                                             .collect(Collectors.toCollection(TreeSet::new));
        SignatureChecker checker = new SignatureChecker(java8Signature, ignores, new CollectingLogger(errors));
        checker.setCheckJars(true);
        checker.setSourcePath(Collections.emptyList());
        checker.process(root.toFile());
        return errors.stream()
                     .map(e -> "Java 9+ API (not in the java18 signature): " + e)
                     .collect(Collectors.toList());
    }

    private static boolean isJdk(String internalName) {
        for (String prefix : JDK_PACKAGE_PREFIXES) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String packageOf(String internalName) {
        int idx = internalName.lastIndexOf('/');
        return idx < 0 ? "" : internalName.substring(0, idx);
    }

    private static Map<String, byte[]> readClassFiles(Path root) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        if (Files.isDirectory(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path p : paths.filter(f -> f.toString().endsWith(".class")).collect(Collectors.toList())) {
                    result.put(root.relativize(p).toString(), Files.readAllBytes(p));
                }
            }
        } else {
            try (ZipFile zip = new ZipFile(root.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")
                        && !entry.getName().startsWith("META-INF/")) {
                        try (InputStream in = zip.getInputStream(entry)) {
                            result.put(entry.getName(), readAll(in));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Facts about one class, gathered in a single ASM pass.
     */
    private static final class ClassFacts extends ClassVisitor {
        private int majorVersion;
        private int access;
        private boolean record;
        private boolean sealed;
        private boolean nested;
        private String name;
        private final Set<String> visibilityAnnotations = new TreeSet<>();

        ClassFacts() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            this.majorVersion = version & 0xFFFF;
            this.access = access;
            this.name = name;
            this.record = "java/lang/Record".equals(superName) || (access & Opcodes.ACC_RECORD) != 0;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (VISIBILITY_ANNOTATIONS.contains(descriptor)) {
                visibilityAnnotations.add(descriptor.substring(descriptor.lastIndexOf('/') + 1,
                                                               descriptor.length() - 1));
            }
            return new NoOpAnnotationVisitor();
        }

        // The visitors below return non-null no-op visitors so that the wrapping ClassRemapper descends into
        // annotations, fields and method bodies and sees every referenced type.

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
                                                     boolean visible) {
            return new NoOpAnnotationVisitor();
        }

        @Override
        public FieldVisitor visitField(int fieldAccess, String fieldName, String descriptor, String signature,
                                       Object value) {
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    return new NoOpAnnotationVisitor();
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int methodAccess, String methodName, String descriptor, String signature,
                                         String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    return new NoOpAnnotationVisitor();
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String annotationDescriptor,
                                                                  boolean visible) {
                    return new NoOpAnnotationVisitor();
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return new NoOpAnnotationVisitor();
                }
            };
        }

        @Override
        public void visitInnerClass(String innerName, String outerName, String simpleName, int innerAccess) {
            // An InnerClasses entry naming this class means it is a member, local or anonymous class.
            if (innerName.equals(name)) {
                nested = true;
            }
        }

        @Override
        public void visitOuterClass(String owner, String methodName, String methodDescriptor) {
            nested = true;
        }

        @Override
        public void visitNestHost(String nestHost) {
            nested = true;
        }

        @Override
        public void visitPermittedSubclass(String permittedSubclass) {
            sealed = true;
        }

        @Override
        public RecordComponentVisitor visitRecordComponent(String componentName, String descriptor, String signature) {
            record = true;
            return null;
        }
    }

    /**
     * An annotation visitor that does nothing but lets a wrapping remapper descend into nested values.
     */
    private static final class NoOpAnnotationVisitor extends AnnotationVisitor {
        NoOpAnnotationVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return this;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return this;
        }
    }

    /**
     * Records every internal class name that appears anywhere in a class: supertypes, descriptors, signatures,
     * constant pool references, annotations and invokedynamic arguments.
     */
    private static final class ReferenceCollector extends Remapper {
        private final Set<String> referenced = new TreeSet<>();

        @Override
        public String map(String internalName) {
            referenced.add(internalName);
            return internalName;
        }
    }

    private static final class CollectingLogger implements Logger {
        private final List<String> errors;

        CollectingLogger(List<String> errors) {
            this.errors = errors;
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void info(String message, Throwable t) {
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public void debug(String message, Throwable t) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable t) {
        }

        @Override
        public void error(String message) {
            errors.add(message);
        }

        @Override
        public void error(String message, Throwable t) {
            errors.add(message + ": " + t);
        }
    }

    /**
     * Locate the jar or class directory of an SDK artifact on the test classpath.
     *
     * @return the location, or {@code null} if it is not on the classpath
     */
    static Path locateArtifact(String artifactId) throws IOException {
        // Packaged jars (local repository or reactor "package" phase) carry Maven metadata.
        String pomProperties = "META-INF/maven/software.amazon.awssdk/" + artifactId + "/pom.properties";
        URL url = PortComplianceChecker.class.getClassLoader().getResource(pomProperties);
        if (url != null && "jar".equals(url.getProtocol())) {
            String spec = url.getPath();
            String jarUri = spec.substring(0, spec.indexOf("!/"));
            try {
                return new File(new URI(jarUri)).toPath();
            } catch (URISyntaxException e) {
                throw new IOException("Cannot resolve " + url, e);
            }
        }
        // Reactor builds that stop before "package" resolve to <module>/target/classes.
        String suffix = File.separator + artifactId + File.separator + "target" + File.separator + "classes";
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (entry.endsWith(suffix)) {
                return new File(entry).toPath();
            }
        }
        return null;
    }
}
