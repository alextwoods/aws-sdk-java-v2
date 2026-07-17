plugins {
    `java-library`
    application
    // Smithy Gradle plugin drives codegen via smithy-build.json. From Maven Central.
    id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    // mavenLocal first so the locally-patched aws-client-awsjson:1.4.0 (with the
    // aws-json error-type namespace fix, commit a279c2eed — not yet released to Central)
    // wins over the published 1.4.0. Everything else still resolves from Central.
    mavenLocal()
    mavenCentral()
}

dependencies {
    val smithyJavaVersion: String by project
    val dynamodbModelVersion: String by project

    // ---- Code generation (build-time only) -------------------------------
    // The codegen plugin + client runtime are visible to the smithy build so it
    // can generate the DynamoDB client from the model.
    smithyBuild("software.amazon.smithy.java:codegen-plugin:$smithyJavaVersion")
    smithyBuild("software.amazon.smithy.java:client-core:$smithyJavaVersion")
    smithyBuild("software.amazon.smithy.java:aws-client-awsjson:$smithyJavaVersion")
    // AWS endpoint rules engine codegen — emits the compiled .bdd endpoint ruleset
    // into the projection's resources (generic client-rulesengine does not).
    smithyBuild("software.amazon.smithy.java:aws-client-rulesengine:$smithyJavaVersion")
    // DynamoDB defines waiters (e.g. tableExists), so codegen needs the waiters plugin.
    smithyBuild("software.amazon.smithy.java:client-waiters:$smithyJavaVersion")

    // The DynamoDB Smithy model itself, pulled from Maven Central (no C2J anywhere).
    // Goes on the regular classpath so the Smithy model assembler discovers it.
    implementation("software.amazon.api.models:dynamodb:$dynamodbModelVersion")

    // ---- Runtime: the smithy-java "guts" ---------------------------------
    implementation("software.amazon.smithy.java:client-core:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:client-http:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:client-rulesengine:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:client-waiters:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:json-codec:$smithyJavaVersion")

    // AWS protocol (awsJson1_0), SigV4 signing, AWS client core + endpoint rules.
    implementation("software.amazon.smithy.java:aws-client-awsjson:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:aws-client-core:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:aws-client-http:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:aws-client-rulesengine:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:aws-sigv4:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:aws-auth-api:$smithyJavaVersion")

    // AWS SDK for Java v2 HTTP client — used by V2TransportBridge to prove the smithy-java
    // runtime can send over a v2 SdkHttpClient (RFC: "shim over smithy-java transport").
    implementation("software.amazon.awssdk:url-connection-client:2.46.9")
}

application {
    mainClass = "com.example.demo.Main"
}

// Second entry point: the AWS SDK v2-shaped facade that delegates to smithy-java.
tasks.register<JavaExec>("runV2") {
    group = "application"
    description = "Runs the v2-style facade demo over the smithy-java client."
    mainClass = "com.example.demo.v2shim.V2Style"
    classpath = sourceSets["main"].runtimeClasspath
}

// Wire the generated client sources into the main source set.
afterEvaluate {
    val clientPath = smithy.getPluginProjectionPath(
        smithy.sourceProjection.get(),
        "java-codegen"
    ).get()
    sourceSets {
        main {
            java {
                srcDir("$clientPath/java")
            }
            // Generated endpoint ruleset (.bdd) + other codegen resources.
            resources {
                srcDir("$clientPath/resources")
            }
        }
    }
}

tasks {
    val smithyBuild by getting
    compileJava {
        dependsOn(smithyBuild)
    }
    // Generated resources (the endpoint .bdd) come from smithyBuild.
    processResources {
        dependsOn(smithyBuild)
    }
}
