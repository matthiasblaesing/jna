
package com.sun.jna;

import javax.swing.JFrame;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class AWTTest {
    private JFrame window;

    @Before
    public void setup() {
        window = new JFrame("Test");
        window.setVisible(true);
    }

    @After
    public void after() {
        window.setVisible(false);
    }

    @Test
    public void testAccessNativePointer() {
        long windowPointer = Native.getComponentID(window);
        System.out.println("Got pointer: " + windowPointer);
        assertTrue(windowPointer != 0);
    }
}
