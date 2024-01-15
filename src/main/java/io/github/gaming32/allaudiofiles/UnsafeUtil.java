package io.github.gaming32.allaudiofiles;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {
    public static final Unsafe UNSAFE;

    static {
        try {
            final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            UNSAFE = (Unsafe)theUnsafeField.get(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
