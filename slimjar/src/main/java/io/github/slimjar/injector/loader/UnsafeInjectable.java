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

package io.github.slimjar.injector.loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;

public final class UnsafeInjectable implements Injectable {
    private final ArrayDeque<URL> unopenedURLs;
    private final ArrayList<URL> pathURLs;

    public UnsafeInjectable(final ArrayDeque<URL> unopenedURLs, final ArrayList<URL> pathURLs) {
        this.unopenedURLs = unopenedURLs;
        this.pathURLs = pathURLs;
    }

    @Override
    public void inject(final URL url) {
        unopenedURLs.addLast(url);
        pathURLs.add(url);
    }

    @SuppressWarnings("unchecked")
    public static Injectable create(final URLClassLoader classLoader) throws ReflectiveOperationException {
        // sun.misc.Unsafe is accessed reflectively since it is not exposed when compiling with --release
        final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        final Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        final Object unsafe = field.get(null);
        final Method objectFieldOffset = unsafeClass.getMethod("objectFieldOffset", Field.class);
        final Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);

        final Object ucp = fetchField(unsafe, objectFieldOffset, getObject, URLClassLoader.class, classLoader, "ucp");
        final ArrayDeque<URL> unopenedURLs = (ArrayDeque<URL>) fetchField(unsafe, objectFieldOffset, getObject, ucp.getClass(), ucp, "unopenedUrls");
        final ArrayList<URL> pathURLs = (ArrayList<URL>) fetchField(unsafe, objectFieldOffset, getObject, ucp.getClass(), ucp, "path");
        return new UnsafeInjectable(unopenedURLs, pathURLs);
    }

    private static Object fetchField(final Object unsafe, final Method objectFieldOffset, final Method getObject, final Class<?> clazz, final Object object, final String name) throws ReflectiveOperationException {
        final Field field = clazz.getDeclaredField(name);
        final long offset = (Long) objectFieldOffset.invoke(unsafe, field);
        return getObject.invoke(unsafe, object, offset);
    }
}
