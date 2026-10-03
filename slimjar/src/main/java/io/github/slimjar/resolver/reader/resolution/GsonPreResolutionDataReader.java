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

package io.github.slimjar.resolver.reader.resolution;

import io.github.slimjar.resolver.ResolutionResult;
import io.github.slimjar.resolver.reader.facade.GsonFacade;
import io.github.slimjar.resolver.reader.facade.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;

public final class GsonPreResolutionDataReader implements PreResolutionDataReader {

    private final GsonFacade gson;

    public GsonPreResolutionDataReader(final GsonFacade gson) {
        this.gson = gson;
    }

    @Override
    public Map<String, ResolutionResult> read(final InputStream inputStream) throws IOException, ReflectiveOperationException {
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final Type rawType = new TypeToken<Map<String, ResolutionResult>>(){}.getRawType();
        return gson.fromJson(inputStreamReader, rawType);
    }
}
