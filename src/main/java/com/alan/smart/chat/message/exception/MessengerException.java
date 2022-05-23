package com.alan.smart.chat.message.exception;

public class MessengerException extends RuntimeException {

    private static final String MESSAGE = "Something went wrong when connecting to the chat server with username '%s'.";
    
    public MessengerException(String username, Throwable e) {
        super(String.format(MESSAGE, username), e);
    }
}
