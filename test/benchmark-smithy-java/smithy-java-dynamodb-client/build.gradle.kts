plugins {
    `java-library`
    id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
    `maven-publish`
}

group = "software.amazon.awssdk.benchmark"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val smithyJavaVersion = "1.5.1"
val smithyVersion = "1.54.0"

dependencies {
    // Codegen plugin - runs at build time to generate client code
    smithyBuild("software.amazon.smithy.java:codegen-plugin:$smithyJavaVersion")
    smithyBuild("software.amazon.smithy.java:client-core:$smithyJavaVersion")
    smithyBuild("software.amazon.smithy.java:client-waiters:$smithyJavaVersion")
    smithyBuild("software.amazon.smithy.java:aws-client-rulesengine:$smithyJavaVersion")

    // AWS service model from Maven Central
    implementation("software.amazon.api.models:dynamodb:1.0.14")

    // Smithy model trait dependencies
    implementation("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    implementation("software.amazon.smithy:smithy-rules-engine:$smithyVersion")
    implementation("software.amazon.smithy:smithy-waiters:$smithyVersion")

    // Runtime: client-core
    api("software.amazon.smithy.java:client-core:$smithyJavaVersion")

    // Protocol runtime - awsJson1_0 for DynamoDB
    implementation("software.amazon.smithy.java:aws-client-awsjson:$smithyJavaVersion")

    // AWS auth (SigV4)
    implementation("software.amazon.smithy.java:aws-sigv4:$smithyJavaVersion")

    // AWS client core
    implementation("software.amazon.smithy.java:aws-client-core:$smithyJavaVersion")

    // Credential resolution
    implementation("software.amazon.smithy.java:aws-credential-chain:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:aws-credentials-imds:$smithyJavaVersion")

    // HTTP client transport
    implementation("software.amazon.smithy.java:client-http-smithy:$smithyJavaVersion")
    implementation("software.amazon.smithy.java:http-client:$smithyJavaVersion")

    // Endpoint resolution
    implementation("software.amazon.smithy.java:aws-client-rulesengine:$smithyJavaVersion")

    // Waiters
    implementation("software.amazon.smithy.java:client-waiters:$smithyJavaVersion")
}

repositories {
    mavenLocal()
    mavenCentral()
}

// Configure codegen projection source set
afterEvaluate {
    val generated = smithy.getPluginProjectionPath("dynamodb-client", "java-codegen").get()
    sourceSets["main"].java.srcDir("$generated/java")
    sourceSets["main"].resources.srcDir("$generated/resources")
}

tasks.compileJava { dependsOn(tasks.named("smithyBuild")) }
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    dependsOn(tasks.named("smithyBuild"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "software.amazon.awssdk.benchmark"
            artifactId = "smithy-java-dynamodb-client"
            version = "1.0-SNAPSHOT"
        }
    }
}
