import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.3.1"
    id("com.gradleup.shadow") version "8.3.11"
    `maven-publish`
}

group = "me.lorenzo0111"
version = "1.3.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val shadowImplementation: Configuration by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
    shadowImplementation(project(":slimjar"))
    shadowImplementation("com.google.code.gson:gson:2.9.0")

    // Shadow is only needed for tests: the plugin talks to whichever Shadow version (legacy or GradleUp) the build applies
    testImplementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.11")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

val shadowJarTask = tasks.named("shadowJar", ShadowJar::class.java)

shadowJarTask.configure {
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)
    // Metadata of shaded dependencies, gson's module descriptor would describe the wrong (relocated) packages
    exclude("META-INF/versions/*/module-info.class", "META-INF/maven/**")
}

// Disabling default jar task as it is overridden by shadowJar
tasks.named("jar").configure {
    enabled = false
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

val ensureDependenciesAreInlined by tasks.registering {
    description = "Ensures all declared dependencies are inlined into shadowed jar"
    group = HelpTasksPlugin.HELP_GROUP
    dependsOn(tasks.shadowJar)

    doLast {
        val nonInlinedDependencies = mutableListOf<String>()
        val inlinedPackagePrefixes = listOf(
            "io/github/slimjar/"
        )
        zipTree(tasks.shadowJar.flatMap { it.archiveFile }).visit {
            if (!isDirectory) {
                val path = relativePath
                if (
                    !path.startsWith("META-INF") &&
                    path.lastName.endsWith(".class") &&
                    inlinedPackagePrefixes.none { path.pathString.startsWith(it) }
                ) {
                    nonInlinedDependencies.add(path.pathString)
                }
            }
        }
        if (nonInlinedDependencies.isNotEmpty()) {
            throw GradleException("Found non inlined dependencies: $nonInlinedDependencies")
        }
    }
}

tasks.named("check") {
    dependsOn(ensureDependenciesAreInlined)
}

tasks {
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    withType<ShadowJar> {
        mapOf(
            "io.github.slimjar" to "",
            "me.lucko.jarrelocator" to ".jarrelocator",
            "com.google.gson" to ".gson"
        ).forEach { relocate(it.key, "io.github.slimjar${it.value}") }
    }

    test {
        useJUnitPlatform()
    }
}

gradlePlugin {
    website.set("https://github.com/Lorenzo0111/slimjar")
    vcsUrl.set("https://github.com/Lorenzo0111/slimjar")
    plugins {
        create("slimjar") {
            id = "me.lorenzo0111.slimjar"
            displayName = "SlimJar"
            description = "JVM Runtime Dependency Management."
            tags.set(listOf("runtime dependency", "relocation"))
            implementationClass = "io.github.slimjar.SlimJarPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://dl.lorenzo0111.me/releases")

            credentials {
                username = project.findProperty("lorenzo0111RepositoryUsername")?.toString() ?: ""
                password = project.findProperty("lorenzo0111RepositoryPassword")?.toString() ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
