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

package io.github.slimjar.plugin.task

import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.slimjar.plugin.applyPlugins
import io.github.slimjar.task.SlimJar
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskTest {

    private val project = ProjectBuilder.builder().build().also { it.applyPlugins() }

    @Test
    fun `Test slimJar task`() {
        assertThatCode {
            project.tasks.getByName("slimJar")
        }.doesNotThrowAnyException()
    }

    @Test
    fun `Test relocations are forwarded to shadowJar`() {
        val project = ProjectBuilder.builder().build().also { it.applyPlugins() }
        project.tasks.withType(SlimJar::class.java).getByName("slimJar")
            .relocate("a.b.c", "m.n.o") { exclude("a.b.c.Excluded") }
        (project as ProjectInternal).evaluate()

        val relocators = project.tasks.withType(ShadowJar::class.java).getByName("shadowJar")
            .relocators.filterIsInstance<SimpleRelocator>()
        assertThat(relocators).hasSize(1)
        assertThat(relocators.single().canRelocateClass("a.b.c.Kept")).isTrue
        assertThat(relocators.single().canRelocateClass("a.b.c.Excluded")).isFalse
    }

}
