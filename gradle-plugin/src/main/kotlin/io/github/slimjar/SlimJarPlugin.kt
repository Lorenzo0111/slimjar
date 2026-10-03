//
// MIT License
//
// Copyright (c) 2021 Vaishnav Anil
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package io.github.slimjar

import io.github.slimjar.exceptions.ShadowNotFoundException
import io.github.slimjar.func.applyReleaseRepo
import io.github.slimjar.func.applySnapshotRepo
import io.github.slimjar.func.createConfig
import io.github.slimjar.relocation.ShadowInterop
import io.github.slimjar.task.SlimJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.maven

const val SLIM_CONFIGURATION_NAME = "slim"
const val SLIM_API_CONFIGURATION_NAME = "slimApi"
const val SLIM_JAR_TASK_NAME = "slimJar"
const val RELEASES_REPOSITORY = "https://dl.lorenzo0111.me/releases/"
const val SNAPSHOTS_REPOSITORY = "https://dl.lorenzo0111.me/snapshots/"
internal const val SHADOW_ID = "com.gradleup.shadow"
internal const val LEGACY_SHADOW_ID = "com.github.johnrengelman.shadow"

class SlimJarPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        // Applies Java if not present, since it's required for the compileOnly configuration
        plugins.apply(JavaPlugin::class.java)

        if (ShadowInterop.appliedPluginId(project) == null) {
            throw ShadowNotFoundException("SlimJar depends on the Shadow plugin, please apply the plugin. For more information visit: https://gradleup.com/shadow/")
        }

        val slimConfig = createConfig(
            SLIM_CONFIGURATION_NAME,
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
        )
        if (plugins.hasPlugin("java-library")) {
            createConfig(
                SLIM_API_CONFIGURATION_NAME,
                JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME,
                JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
            )
        }

        val slimJar = tasks.create(SLIM_JAR_TASK_NAME, SlimJar::class.java, slimConfig)
        // Auto adds the slimJar lib dependency
        afterEvaluate {
            if (applyReleaseRepo) {
                repositories.maven(RELEASES_REPOSITORY)
            }
            if (applySnapshotRepo) {
                repositories.maven(SNAPSHOTS_REPOSITORY)
            }
        }
        project.dependencies.extra.set(
            "slimjar",
            asGroovyClosure("+") { version -> slimJarLib(version) }
        )
        // Hooks into shadow to inject relocations, once the build script had the chance to configure the slimJar task.
        // Shadow is accessed reflectively since its API isn't binary compatible across versions (legacy, GradleUp 8.x and 9.x)
        afterEvaluate {
            val shadowTask = tasks.findByName(ShadowInterop.SHADOW_JAR_TASK_NAME) ?: return@afterEvaluate
            slimJar.relocations().forEach { rule -> ShadowInterop.relocate(shadowTask, rule) }
        }

        // Bundles the generated files (slimjar.json, isolated jars...) with the main resources, so that any jar task includes them
        extensions.getByType(SourceSetContainer::class.java)
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .output
            .dir(mapOf("builtBy" to slimJar), slimJar.outputDirectory)
    }

}

internal fun slimJarLib(version: String) = "me.lorenzo0111:slimjar:$version"
