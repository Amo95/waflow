package com.infusi.waflow.flow;

public class StepNotFoundException extends RuntimeException {
    public StepNotFoundException(String message) {
        super(message);
    }
}
