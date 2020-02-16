/* Copyright (c) 2019 Matthias Bläsing, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */

package com.sun.jna;

import com.sun.jna.BooleanMappingTest.TestLibrary.BooleanStruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class BooleanMappingTest {
    public static interface TestLibrary extends Library {
        public TestLibrary INSTANCE = Native.load("testlib", TestLibrary.class, getOptions());

        @Structure.FieldOrder({"s1", "s2", "s3", "s4", "s5", "s6"})
        class BooleanStruct extends Structure {
            public String s1;
            public int s2;
            public double s3;
            public boolean s4;
            public double s5;
            public int s6;
        }

        // It is always the native function returnInt8Argument that is called
        // in getOptions a corresponding typemapper is added to the library options
        byte booleanPrimitiveToByte(boolean arg);
        byte booleanObjectToByte(Boolean arg);
        boolean byteToBooleanPrimitive(byte arg);
        Boolean byteToBooleanObject(byte arg);
        int stringifyBooleanStruct(byte[] buffer, int bufferLength, BooleanStruct struct);
    }

    public static class TestLibraryDirect implements TestLibrary {
        static {
            Native.register(NativeLibrary.getInstance("testlib", getOptions()));
        }

        public static final TestLibraryDirect INSTANCE = new TestLibraryDirect();

        @Override
        public native byte booleanPrimitiveToByte(boolean arg);
        @Override
        public native byte booleanObjectToByte(Boolean arg);
        @Override
        public native boolean byteToBooleanPrimitive(byte arg);
        @Override
        public native Boolean byteToBooleanObject(byte arg);
        @Override
        public native int stringifyBooleanStruct(byte[] buffer, int bufferLength, BooleanStruct struct);
    }

    @Test
    public void testMarshallingToInt8() {
        assertEquals(0, TestLibrary.INSTANCE.booleanPrimitiveToByte(false));
        assertEquals((byte) 0xFF, TestLibrary.INSTANCE.booleanPrimitiveToByte(true));
        assertEquals(0, TestLibrary.INSTANCE.booleanObjectToByte(Boolean.FALSE));
        assertEquals((byte) 0xFF, TestLibrary.INSTANCE.booleanObjectToByte(Boolean.TRUE));
    }

    @Test
    public void testUnmarshallingFromInt8() {
        assertFalse(TestLibrary.INSTANCE.byteToBooleanPrimitive((byte) 0));
        assertTrue(TestLibrary.INSTANCE.byteToBooleanPrimitive((byte) 1));
        assertTrue(TestLibrary.INSTANCE.byteToBooleanPrimitive((byte) 0xFF));
        assertFalse(TestLibrary.INSTANCE.byteToBooleanObject((byte) 0));
        assertTrue(TestLibrary.INSTANCE.byteToBooleanObject((byte) 1));
        assertTrue(TestLibrary.INSTANCE.byteToBooleanObject((byte) 0xFF));
    }

    @Test
    public void testMarshallingToInt8Direct() {
        assertEquals(0, TestLibraryDirect.INSTANCE.booleanPrimitiveToByte(false));
        assertEquals((byte) 0xFF, TestLibraryDirect.INSTANCE.booleanPrimitiveToByte(true));
        assertEquals(0, TestLibraryDirect.INSTANCE.booleanObjectToByte(Boolean.FALSE));
        assertEquals((byte) 0xFF, TestLibraryDirect.INSTANCE.booleanObjectToByte(Boolean.TRUE));
    }

    @Test
    public void testUnmarshallingFromInt8Direct() {
        assertFalse(TestLibraryDirect.INSTANCE.byteToBooleanPrimitive((byte) 0));
        assertTrue(TestLibraryDirect.INSTANCE.byteToBooleanPrimitive((byte) 1));
        assertTrue(TestLibraryDirect.INSTANCE.byteToBooleanPrimitive((byte) 0xFF));
        assertFalse(TestLibraryDirect.INSTANCE.byteToBooleanObject((byte) 0));
        assertTrue(TestLibraryDirect.INSTANCE.byteToBooleanObject((byte) 1));
        assertTrue(TestLibraryDirect.INSTANCE.byteToBooleanObject((byte) 0xFF));
    }

    @Test
    public void testBooleanStruct() {
        byte[] data = new byte[2048];
        BooleanStruct s = new TestLibrary.BooleanStruct();
        s.s1 = "Test - Struct";
        s.s2 = 42;
        s.s3 = 1.42;
        s.s4 = true;
        s.s5 = 2.23;
        s.s6 = 23;
        TestLibrary.INSTANCE.stringifyBooleanStruct(data, data.length, s);
        assertTrue(Native.toString(data).contains("s4: 255,"));
        TestLibraryDirect.INSTANCE.stringifyBooleanStruct(data, data.length, s);
        assertTrue(Native.toString(data).contains("s4: 255,"));
        s.s4 = false;
        TestLibrary.INSTANCE.stringifyBooleanStruct(data, data.length, s);
        assertTrue(Native.toString(data).contains("s4: 0,"));
        TestLibraryDirect.INSTANCE.stringifyBooleanStruct(data, data.length, s);
        assertTrue(Native.toString(data).contains("s4: 0,"));
    }

    private static Map<String,?> getOptions() {
        Map<String,Object> result = new HashMap<>();
        // We want to call the same native function under different names
        result.put(Library.OPTION_FUNCTION_MAPPER, new FunctionMapper() {
            @Override
            public String getFunctionName(NativeLibrary library, Method method) {
                switch (method.getName()) {
                    case "booleanPrimitiveToByte":
                    case "booleanObjectToByte":
                    case "byteToBooleanPrimitive":
                    case "byteToBooleanObject":
                        return "returnInt8Argument";
                    default:
                        return method.getName();
                }
            }
        });
        DefaultTypeMapper typeMapper = new DefaultTypeMapper();

        typeMapper.addTypeConverter(Boolean.class, new TypeConverter() {
            @Override
            public Object fromNative(Object nativeValue, FromNativeContext context) {
                return ((byte) nativeValue) != 0;
            }

            @Override
            public Class<?> nativeType() {
                return byte.class;
            }

            @Override
            public Object toNative(Object value, ToNativeContext context) {
                if(value.getClass() == Boolean.class) {
                    value = (boolean) value;
                }
                if((boolean) value) {
                    return (byte) 0xFF;
                } else {
                    return (byte) 0;
                }
            }
        });

        result.put(Library.OPTION_TYPE_MAPPER, typeMapper);
        return result;
    }
}
