package com.proxgrind.devices;

import java.io.IOException;

public class EmptyDevice implements Device {
    @Override
    public boolean working() throws IOException {
        return true;
    }

    @Override
    public boolean close() throws IOException {
        return true;
    }
}
