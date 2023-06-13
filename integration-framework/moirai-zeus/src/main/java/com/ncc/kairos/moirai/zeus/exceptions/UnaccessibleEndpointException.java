package com.ncc.kairos.moirai.zeus.exceptions;

import java.io.IOException;

public class UnaccessibleEndpointException extends IOException {
    public UnaccessibleEndpointException(String message) {
        super(message);
    }

    public UnaccessibleEndpointException() {
    }
}
