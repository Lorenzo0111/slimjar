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

package io.github.slimjar.resolver.pinger;

import junit.framework.TestCase;
import org.mockito.Mockito;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class URLPingerTest extends TestCase {

    public void testHttpURLPingerHttp() throws IOException {
        final HttpURLConnection httpURLConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doReturn(HttpURLConnection.HTTP_OK).when(httpURLConnection).getResponseCode();
        final URLPinger urlPinger = new HttpURLPinger();
        boolean result = urlPinger.ping(stubUrl("http", httpURLConnection));
        assertTrue("Valid http URL", result);
    }

    public void testHttpURLPingerHttps() throws IOException {
        final HttpsURLConnection httpsURLConnection = Mockito.mock(HttpsURLConnection.class);
        Mockito.doReturn(HttpURLConnection.HTTP_OK).when(httpsURLConnection).getResponseCode();
        final URLPinger urlPinger = new HttpURLPinger();
        boolean result = urlPinger.ping(stubUrl("https", httpsURLConnection));
        assertTrue("Valid https URL", result);
    }

    public void testHttpURLPingerFailIfNotOk() throws IOException {
        final HttpsURLConnection httpsURLConnection = Mockito.mock(HttpsURLConnection.class);
        Mockito.doReturn(HttpURLConnection.HTTP_BAD_REQUEST).when(httpsURLConnection).getResponseCode();
        final URLPinger urlPinger = new HttpURLPinger();
        boolean result = urlPinger.ping(stubUrl("https", httpsURLConnection));
        assertFalse("Non-OK should fail", result);
    }

    public void testHttpURLPingerExceptionOnPing() throws IOException {
        final HttpsURLConnection httpsURLConnection = Mockito.mock(HttpsURLConnection.class);
        Mockito.doThrow(new IOException()).when(httpsURLConnection).connect();
        Mockito.doReturn(HttpURLConnection.HTTP_OK).when(httpsURLConnection).getResponseCode();
        final URLPinger urlPinger = new HttpURLPinger();
        boolean result = urlPinger.ping(stubUrl("https", httpsURLConnection));
        assertFalse("Exception should fail", result);
    }

    public void testHttpURLPingerUnsupportedProtocol() throws IOException {
        final URLConnection connection = Mockito.mock(URLConnection.class);
        final URLPinger urlPinger = new HttpURLPinger();
        boolean result = urlPinger.ping(stubUrl("non-existent-protocol", connection));
        assertFalse("Non-OK should fail", result);
        Mockito.verifyNoInteractions(connection);
    }

    /**
     * Creates a real URL (URL is final and can't be mocked) whose connection is the given mock
     */
    private static URL stubUrl(final String protocol, final URLConnection connection) throws MalformedURLException {
        return new URL(protocol, "repo.example", -1, "/dep.jar", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL url) {
                return connection;
            }
        });
    }
}
