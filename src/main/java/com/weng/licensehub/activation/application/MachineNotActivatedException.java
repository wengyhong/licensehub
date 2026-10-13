package com.weng.licensehub.activation.application;

public class MachineNotActivatedException extends RuntimeException {

    public MachineNotActivatedException() {
        super("Machine is not activated for this license");
    }
}