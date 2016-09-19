/**
 * Copyright (c) 2016 Matthias Bläsing, All Rights Reserved
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 */
package com.sun.jna.platform.win32;

import com.sun.jna.Function;
import com.sun.jna.LastErrorException;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Allow binding of functions, that shall be bound by their ordinal number.
 * 
 * <p>
 * This is win32 specific.</p>
 *
 * <p>
 * The invocation Handler is meant to be used together with the annotation. An
 * interface is declared listing the methods to be bound, each method is
 * annotated with the
 * {@link com.sun.jna.platform.win32.OrdinalValueHelper.OrdinalValueInvocation}
 * annotation. On this interface a static INTERFACE member is defined, that
 * implements the interface by using the
 * {@link com.sun.jna.platform.win32.OrdinalValueHelper.OrdinalValueInvocationHandler}
 * </p>
 *
 * <p>This is documented in <a href="https://msdn.microsoft.com/de-de/library/windows/desktop/ms683212(v=vs.85).aspx">the MSDN - GetProcAddress</a></p>
 */
public abstract class OrdinalValueHelper {

    public static class OrdinalValueInvocationHandler implements InvocationHandler {

        private final NativeLibrary backingLibrary;
        private final HMODULE hmodule;
        private final int defaultCallflags;
        private final String defaultEncoding;
        private final Map<FunctionKey, Function> functionCache
                = Collections.synchronizedMap(new HashMap<FunctionKey, Function>());

        public OrdinalValueInvocationHandler(String backingLibraryName, int defaultCallflags, String defaultEncoding) {
            // ensure library is loaded and hold it
            this.backingLibrary = NativeLibrary.getInstance(backingLibraryName);
            // get module handle needed to resolve function pointer via GetProcAddress
            this.hmodule = Kernel32.INSTANCE.GetModuleHandle(backingLibraryName);
            this.defaultCallflags = defaultCallflags;
            this.defaultEncoding = defaultEncoding;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            OrdinalValueInvocation annotation = method.getAnnotation(OrdinalValueInvocation.class);
            if (annotation == null) {
                throw new UnsatisfiedLinkError("Method must be annotated with @OrdinalValueInvocation to be used with OrdinalValueInvocationHandler");
            }

            int ordinal = annotation.ordinalValue();
            int callflags = defaultCallflags;
            if (annotation.callflags() != Integer.MIN_VALUE) {
                callflags = annotation.callflags();
            }
            String encoding = defaultEncoding;
            if (!annotation.encoding().isEmpty()) {
                encoding = annotation.encoding();
            }
            Class returnType = method.getReturnType();

            FunctionKey key = new FunctionKey(ordinal, callflags, encoding, returnType);

            Function function = functionCache.get(key);
            if (function == null) {
                try {
                    Pointer functionPointer = Kernel32.INSTANCE.GetProcAddress(hmodule, ordinal);
                    function = Function.getFunction(functionPointer, callflags, encoding);
                    functionCache.put(key, function);
                } catch (LastErrorException ex) {
                    throw new UnsatisfiedLinkError(String.format(
                            "Could not find native method for method: %s#%s (Error: %s (%d))",
                            method.getDeclaringClass().getName(),
                            method.getName(),
                            Kernel32Util.formatMessage(ex.getErrorCode()),
                            ex.getErrorCode()
                    ));
                }
            }
            return function.invoke(returnType, args);
        }

        private static class FunctionKey {

            private final int ordinal;
            private final int callflags;
            private final String encoding;
            private final Class returnType;

            public FunctionKey(int ordinal, int callflags, String encoding, Class returnType) {
                this.ordinal = ordinal;
                this.callflags = callflags;
                this.encoding = encoding;
                this.returnType = returnType;
            }

            @Override
            public int hashCode() {
                int hash = 3;
                hash = 67 * hash + this.ordinal;
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final FunctionKey other = (FunctionKey) obj;
                if (this.ordinal != other.ordinal) {
                    return false;
                }
                if (this.callflags != other.callflags) {
                    return false;
                }
                if ((this.encoding == null) ? (other.encoding != null) : !this.encoding.equals(other.encoding)) {
                    return false;
                }
                if (this.returnType != other.returnType && (this.returnType == null || !this.returnType.equals(other.returnType))) {
                    return false;
                }
                return true;
            }

        }
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.METHOD)
    public @interface OrdinalValueInvocation {

        /**
         * Ordinal value to use to resolve function definition
         */
        public int ordinalValue();

        /**
         * Encoding to use for this method call - overrides default encoding
         * provided on library level.
         *
         * <p>
         * The empty string causes the library defaults to be used.</p>
         */
        public String encoding() default "";

        /**
         * Callflags to apply for the function call - overrides default
         * callflags provided on library level.
         *
         * <p>
         * A value of Integer.MIN_VALUE causes the library defaults to be
         * used.</p>
         */
        public int callflags() default Integer.MIN_VALUE;
    }
}
