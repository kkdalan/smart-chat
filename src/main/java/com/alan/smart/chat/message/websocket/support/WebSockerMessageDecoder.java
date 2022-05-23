package com.alan.smart.chat.message.websocket.support;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.alan.smart.chat.message.websocket.dto.ChatMessage;
import com.google.gson.Gson;

public class WebSockerMessageDecoder implements Decoder.Text<ChatMessage> {

    @Override
    public ChatMessage decode(String message) {
        Gson gson = new Gson();
        return gson.fromJson(message, ChatMessage.class);
    }

    @Override
    public boolean willDecode(String message) {
        return (message != null);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
