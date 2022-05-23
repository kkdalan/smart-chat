package com.alan.smart.chat.message;

import javax.websocket.Session;

import com.alan.smart.chat.message.websocket.dto.ChatMessage;

public interface WebSocketMessenger {
    
    public static final String XMPP_WEB_SOCKET_MESSENGER = "XMPPWebSocketMessenger";

    void startSession(Session session, String username, String password);

    void sendMessage(ChatMessage message, Session session);

    void disconnect(Session session);

}