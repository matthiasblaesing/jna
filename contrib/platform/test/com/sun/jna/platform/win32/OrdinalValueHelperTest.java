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
import com.sun.jna.Native;
import com.sun.jna.platform.win32.OrdinalValueHelper.OrdinalValueInvocation;
import com.sun.jna.platform.win32.OrdinalValueHelper.OrdinalValueInvocationHandler;
import com.sun.jna.platform.win32.WinDef.DWORD;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class OrdinalValueHelperTest {
    /**
     * Sample interface to demonstrate ordinal value based binding.
     * 
     * From link.exe /dump /exports c:\\Windows\\System32\\kernel32.dll
     * 
     *  746  2E9 0004FA20 GetTapeStatus
     *  747  2EA 0002DB20 GetTempFileNameA
     *  748  2EB 0002DB30 GetTempFileNameW
     *  749  2EC 0002DB40 GetTempPathA
     *  750  2ED 0002DB50 GetTempPathW
     *  751  2EE 00026780 GetThreadContext
     * 
     * Highest ordinal at time of writing was 1599, so the value for "WronglyMapped"
     * should be save for some time.
     */
    interface Kernel32Ordinal {
        public static final Kernel32Ordinal INSTANCE = (Kernel32Ordinal) Proxy.newProxyInstance(Kernel32Ordinal.class.getClassLoader(),
                new Class[] {Kernel32Ordinal.class},
                new OrdinalValueInvocationHandler("kernel32", Function.ALT_CONVENTION, null)
        );
        
        @OrdinalValueInvocation(ordinalValue = 750)
        void GetTempPathW(int bufferLength, char[] buffer);
        
        @OrdinalValueInvocation(ordinalValue = 749)
        void GetTempPathA(int bufferLength, byte[] buffer);
        
        @OrdinalValueInvocation(ordinalValue = 65000)
        void WronglyMapped();
        
        void Unmapped();
    }
    

    @Test
    public void testBasicInvoke() {
        // Compare results of the two ordinal based calls (Ansi and Wide variants)
        // with the name based call (classic).
        char[] namedBuffer = new char[2048];
        Kernel32.INSTANCE.GetTempPath(new DWORD(namedBuffer.length), namedBuffer);
        String namedString = Native.toString(namedBuffer);
        char[] ordinal750Buffer = new char[2048];
        Kernel32Ordinal.INSTANCE.GetTempPathW(ordinal750Buffer.length, ordinal750Buffer);
        String ordinal750String = Native.toString(ordinal750Buffer);
        byte[] ordinal749Buffer = new byte[2048];
        Kernel32Ordinal.INSTANCE.GetTempPathA(ordinal749Buffer.length, ordinal749Buffer);
        String ordinal749String = Native.toString(ordinal749Buffer, Native.getDefaultStringEncoding());
        
        assertArrayEquals(namedBuffer, ordinal750Buffer);
        assertEquals(namedString, ordinal750String);
        assertEquals(namedString, ordinal749String);
    }
    
    @Test(expected = UnsatisfiedLinkError.class)
    public void testWronglyMapped() {
        Kernel32Ordinal.INSTANCE.WronglyMapped();
    }
    
    @Test(expected = UnsatisfiedLinkError.class)
    public void testUnmapped() {
        Kernel32Ordinal.INSTANCE.Unmapped();
    }
}
