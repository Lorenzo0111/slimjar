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

package io.github.slimjar.resolver.enquirer;

import io.github.slimjar.resolver.data.Dependency;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.pinger.URLPinger;
import io.github.slimjar.resolver.strategy.MavenChecksumPathResolutionStrategy;
import io.github.slimjar.resolver.strategy.MavenPomPathResolutionStrategy;
import io.github.slimjar.resolver.strategy.PathResolutionStrategy;
import junit.framework.TestCase;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.URL;
import java.util.Collections;

public class RepositoryEnquirerTest extends TestCase {
    public void testPingingEnquirerProvideValidURL() throws Exception {
        final Repository repository = new Repository(new URL("https://a.b.c/repo/"));
        final Dependency dependency = new Dependency("a.b.c","d","", null, Collections.emptySet());

        final PathResolutionStrategy resolutionStrategy = Mockito.mock(PathResolutionStrategy.class);
        final URLPinger pinger = Mockito.mock(URLPinger.class);

        Mockito.doReturn(Collections.singleton("https://a.b.c/repo/dep.jar")).when(resolutionStrategy).pathTo(repository, dependency);
        Mockito.doReturn(true).when(pinger).ping(ArgumentMatchers.any(URL.class));

        final RepositoryEnquirer repositoryEnquirer = new PingingRepositoryEnquirer(repository, resolutionStrategy, new MavenChecksumPathResolutionStrategy("SHA-256", resolutionStrategy), new MavenPomPathResolutionStrategy(), pinger);
        assertNotNull("Valid repo & dep should return non-null URL", repositoryEnquirer.enquire(dependency));
    }

    public void testPingingEnquirerProvideInvalidURL() throws Exception {
        final PathResolutionStrategy resolutionStrategy = Mockito.mock(PathResolutionStrategy.class);
        final URLPinger pinger = Mockito.mock(URLPinger.class);

        Mockito.doReturn(Collections.singleton("https://a.b.c/repo/dep.jar")).when(resolutionStrategy).pathTo(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.doReturn(false).when(pinger).ping(ArgumentMatchers.any(URL.class));

        final RepositoryEnquirer repositoryEnquirer = new PingingRepositoryEnquirer(null, resolutionStrategy, resolutionStrategy, resolutionStrategy, pinger);
        assertNull("Invalid repo or dep should return null URL", repositoryEnquirer.enquire(new Dependency("", "", "", null, Collections.emptySet())));
    }

    public void testPingingEnquirerProvideMalformedURL() throws Exception {
        final Repository repository = new Repository(new URL("https://a.b.c/repo/"));
        final PathResolutionStrategy resolutionStrategy = Mockito.mock(PathResolutionStrategy.class);
        final URLPinger pinger = Mockito.mock(URLPinger.class);

        Mockito.doReturn(Collections.singleton("some_malformed_url")).when(resolutionStrategy).pathTo(ArgumentMatchers.eq(repository), ArgumentMatchers.any());

        final RepositoryEnquirer repositoryEnquirer = new PingingRepositoryEnquirer(repository, resolutionStrategy, resolutionStrategy, resolutionStrategy, pinger);
        assertNull("Malformed URL should return null URL", repositoryEnquirer.enquire(new Dependency("", "", "", null, Collections.emptySet())));
    }
}
