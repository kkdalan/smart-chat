package com.alan.smart.chat.message.websocket.dto;

import com.alan.smart.chat.util.ObjectUtil;

public class ChatMessage {

    private String from;
    private String to;
    private String content;
    private MessageType type;

    public enum MessageType {
	 CHAT,
	 JOIN,
	 LEAVE
    }
    
    private ChatMessage() {}
    
    @Override
    public String toString() {
	return ObjectUtil.reflectToString(this);
    }

    public String getFrom() {
	return from;
    }

    public void setFrom(String from) {
	this.from = from;
    }

    public String getTo() {
	return to;
    }

    public void setTo(String to) {
	this.to = to;
    }

    public String getContent() {
	return content;
    }

    public void setContent(String content) {
	this.content = content;
    }

    public MessageType getType() {
	return type;
    }

    public void setType(MessageType type) {
	this.type = type;
    }

    public static Builder builder() {
	return new ChatMessage.Builder();
    }

    public static class Builder {
	String from;
	String to;
	String content;
	MessageType type;

	public ChatMessage build() {
	    ChatMessage websocketMessage = new ChatMessage();
	    websocketMessage.setFrom(from);
	    websocketMessage.setTo(to);
	    websocketMessage.setContent(content);
	    websocketMessage.setType(type);
	    return websocketMessage;
	}

	public Builder from(String from) {
	    this.from = from;
	    return this;
	}

	public Builder to(String to) {
	    this.to = to;
	    return this;
	}

	public Builder content(String content) {
	    this.content = content;
	    return this;
	}

	public Builder type(MessageType type) {
	    this.type = type;
	    return this;
	}

    }
}
