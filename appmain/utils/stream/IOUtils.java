package com.proxgrind.chameleon.utils.stream;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public class IOUtils {
    public static boolean close(@Nullable Closeable closeable) {
        if (closeable == null) return false;
        try {
            closeable.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean flush(@Nullable Flushable flushable) {
        if (flushable == null) return false;
        try {
            flushable.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
