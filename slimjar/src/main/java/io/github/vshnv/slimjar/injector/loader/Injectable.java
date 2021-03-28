package io.github.vshnv.slimjar.injector.loader;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

public interface Injectable {
    void inject(final URL url) throws InvocationTargetException, IllegalAccessException;
    
    static WrappedInjectableClassLoader wrap(URLClassLoader classLoader) {
        return new WrappedInjectableClassLoader(classLoader);
    }
}
