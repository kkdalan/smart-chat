package com.alan.smart.chat.message.websocket.support;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.alan.smart.chat.message.websocket.dto.ChatMessage;
import com.google.gson.Gson;

public class WebSocketMessageEncoder implements Encoder.Text<ChatMessage> {
    @Override
    public String encode(ChatMessage message) {
        Gson gson = new Gson();
        return gson.toJson(message);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
