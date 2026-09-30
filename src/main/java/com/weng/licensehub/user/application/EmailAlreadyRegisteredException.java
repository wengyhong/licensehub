package com.weng.licensehub.user.application;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(){
        super("An account with this email already exists");
    }

    public EmailAlreadyRegisteredException(Throwable cause)
    {
        super("An account with this email already exists", cause);
    }
}
