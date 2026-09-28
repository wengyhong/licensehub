// ActivationLimitExceededException.java
package com.weng.licensehub.activation.application;

public class ActivationLimitExceededException
        extends RuntimeException {

    public ActivationLimitExceededException() {
        super("Maximum number of activations reached");
    }
}