package com.alan.smart.chat.message.impl;

import javax.websocket.Session;

import com.alan.smart.chat.message.WebSocketMessenger;
import com.alan.smart.chat.message.websocket.dto.ChatMessage;

public class SimpleWebSocketMessenger implements WebSocketMessenger {

    @Override
    public void startSession(Session session, String username, String password) {
	// TODO Auto-generated method stub
	
    }

    @Override
    public void sendMessage(ChatMessage message, Session session) {
	// TODO Auto-generated method stub
	
    }

    @Override
    public void disconnect(Session session) {
	// TODO Auto-generated method stub
	
    }

}
