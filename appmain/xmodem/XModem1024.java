package com.proxgrind.chameleon.xmodem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.proxgrind.chameleon.posixio.PosixCom;

/**
 * Unfinished
 *
 * @author DXL
 * @version 1.1
 * @deprecated
 */
public class XModem1024 extends AbstractXModem {

    //起始头!
    private byte STX = 0x02;

    public XModem1024(PosixCom com) {
        super(com);
    }

    @Override
    public boolean send(InputStream sources) throws IOException {
        return false;
    }

    @Override
    public boolean recv(OutputStream target) throws IOException {
        return false;
    }
}
