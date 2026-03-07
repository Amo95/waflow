package com.infusi.waflow.flow;

public class FlowNotFoundException extends RuntimeException {
    public FlowNotFoundException(String message) {
        super(message);
    }
}
