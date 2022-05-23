package com.alan.smart.chat.message.websocket.support;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alan.smart.chat.message.websocket.dto.ChatMessage;

@Component
public class WebSocketMessageHelper {

    public static final Logger log = LoggerFactory.getLogger(WebSocketMessageHelper.class);

    public void send(Session session, ChatMessage webSocketMessage) {
	try {
	    session.getBasicRemote().sendObject(webSocketMessage);
	} catch (IOException | EncodeException e) {
	    log.error("WebSocket error, message {} was not sent.", webSocketMessage.toString(), e);
	}
    }
}
