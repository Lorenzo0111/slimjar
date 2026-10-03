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

package io.github.slimjar.relocation

import io.github.slimjar.LEGACY_SHADOW_ID
import io.github.slimjar.SHADOW_ID
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Bridges to the Shadow plugin without linking against it.
 * Method signatures (return types) differ between the legacy plugin (com.github.johnrengelman.shadow),
 * GradleUp 8.x and GradleUp 9.x, so methods are looked up by name and parameters only.
 */
internal object ShadowInterop {

    const val SHADOW_JAR_TASK_NAME = "shadowJar"

    /**
     * Returns the id of the applied Shadow plugin, preferring the GradleUp one, or null if none is applied
     */
    fun appliedPluginId(project: Project): String? =
        listOf(SHADOW_ID, LEGACY_SHADOW_ID).firstOrNull { project.plugins.hasPlugin(it) }

    /**
     * Applies the same Shadow plugin used by [source] to [target], defaulting to the GradleUp one
     */
    fun applyPlugin(source: Project, target: Project) {
        target.pluginManager.apply(appliedPluginId(source) ?: SHADOW_ID)
    }

    /**
     * Calls `shadowJar.relocate(String, String, Action<SimpleRelocator>)`
     */
    fun relocate(shadowTask: Task, rule: RelocationRule) {
        val relocate = shadowTask.javaClass.getMethod(
            "relocate",
            String::class.java,
            String::class.java,
            Action::class.java
        )
        // kotlin-dsl compiles Action lambdas with the argument as receiver
        val configure = Action<Any> {
            val relocator = this
            val include = relocator.javaClass.getMethod("include", String::class.java)
            val exclude = relocator.javaClass.getMethod("exclude", String::class.java)
            rule.inclusions.forEach { include.invoke(relocator, it) }
            rule.exclusions.forEach { exclude.invoke(relocator, it) }
        }
        relocate.invoke(shadowTask, rule.originalPackagePattern, rule.relocatedPackagePattern, configure)
    }
}
