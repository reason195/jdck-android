package cn.xanderye.android.jdck.util;

import java.io.IOException;

public class PanelAuthException extends IOException {

    private final int statusCode;

    public PanelAuthException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
