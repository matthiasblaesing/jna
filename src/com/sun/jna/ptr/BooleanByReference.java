/* Copyright (c) 2017 Matthias Bläsing, All Rights Reserved
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
package com.sun.jna.ptr;

import com.sun.jna.Native;

public class BooleanByReference extends ByReference {
    
    public BooleanByReference() {
        this(false);
    }
    
    public BooleanByReference(boolean value) {
        super(Native.BOOL_SIZE);
        setValue(value);
    }

    public void setValue(boolean value) {
        byte nativeValue = value ? ((byte) 1) : 0;
        switch(Native.BOOL_SIZE) {
            case 1:
                getPointer().setByte(0, nativeValue);
                break;
            case 2:
                getPointer().setShort(0, nativeValue);
                break;
            case 4:
                getPointer().setInt(0, nativeValue);
                break;
            case 8:
                getPointer().setLong(0, nativeValue);
                break;
            default:
                throw new IllegalStateException("Unsupported BOOL_SIZE: " + Native.BOOL_SIZE);
        }
    }
    
    public boolean getValue() {
        long nativeValue;
        switch(Native.BOOL_SIZE) {
            case 1:
                nativeValue = getPointer().getByte(0);
                break;
            case 2:
                nativeValue = getPointer().getShort(0);
                break;
            case 4:
                nativeValue = getPointer().getInt(0);
                break;
            case 8:
                nativeValue = getPointer().getLong(0);
                break;
            default:
                throw new IllegalStateException("Unsupported BOOL_SIZE: " + Native.BOOL_SIZE);
        }
        return nativeValue != 0;
    }

}
