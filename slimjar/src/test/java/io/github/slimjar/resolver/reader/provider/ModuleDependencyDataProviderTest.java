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

package io.github.slimjar.resolver.reader.provider;

import io.github.slimjar.resolver.data.DependencyData;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.reader.MockDependencyData;
import io.github.slimjar.resolver.reader.dependency.DependencyDataProvider;
import io.github.slimjar.resolver.reader.dependency.GsonDependencyReader;
import io.github.slimjar.resolver.reader.dependency.ModuleDependencyDataProvider;
import io.github.slimjar.resolver.reader.facade.ReflectiveGsonFacadeFactory;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ModuleDependencyDataProviderTest extends TestCase {
    private static final Path DEFAULT_DOWNLOAD_DIRECTORY;
    private static final Collection<Repository> CENTRAL_MIRRORS;

    static {
        try {
            CENTRAL_MIRRORS = Collections.singleton(new Repository(new URL(Repository.DEFAULT_CENTRAL_MIRROR_URL)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        final String userHome = System.getProperty("user.home");
        final String defaultPath = String.format("%s/.slimjar", userHome);
        DEFAULT_DOWNLOAD_DIRECTORY = new File(defaultPath).toPath();
    }

    public void testModuleDependencyDataProviderNonEmpty() throws Exception {
        final MockDependencyData mockDependencyData = new MockDependencyData();
        final File module = createModule(mockDependencyData.getSampleDependencyData());
        final DependencyDataProvider dependencyDataProvider = new ModuleDependencyDataProvider(new GsonDependencyReader(ReflectiveGsonFacadeFactory.create(DEFAULT_DOWNLOAD_DIRECTORY, CENTRAL_MIRRORS).createFacade()), module.toURI().toURL());
        assertEquals("Read and provide proper dependencies", mockDependencyData.getExpectedSample(), dependencyDataProvider.get());
    }

    public void testModuleDependencyDataProviderEmpty() throws Exception {
        final File module = createModule(null);
        final DependencyData emptyDependency = new DependencyData(
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );
        final DependencyDataProvider dependencyDataProvider = new ModuleDependencyDataProvider(new GsonDependencyReader(ReflectiveGsonFacadeFactory.create(DEFAULT_DOWNLOAD_DIRECTORY, CENTRAL_MIRRORS).createFacade()), module.toURI().toURL());
        assertEquals("Empty dependency if not exists", emptyDependency, dependencyDataProvider.get());
    }

    /**
     * Creates a module jar, containing slimjar.json only if data is provided
     */
    private static File createModule(final String dependencyData) throws IOException {
        final File module = File.createTempFile("slimjar-module", ".jar");
        module.deleteOnExit();
        try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(module))) {
            outputStream.putNextEntry(new ZipEntry(dependencyData == null ? "placeholder.txt" : "slimjar.json"));
            if (dependencyData != null) {
                outputStream.write(dependencyData.getBytes(StandardCharsets.UTF_8));
            }
            outputStream.closeEntry();
        }
        return module;
    }
}
