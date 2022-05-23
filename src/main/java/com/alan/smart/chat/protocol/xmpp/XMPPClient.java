package com.alan.smart.chat.protocol.xmpp;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import javax.websocket.Session;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException.MissingMucCreationAcknowledgeException;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucAlreadyJoinedException;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucConfigurationNotSupportedException;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alan.smart.chat.message.exception.MessengerException;
import com.alan.smart.chat.protocol.xmpp.listener.DefaultPresenceEventListener;
import com.alan.smart.chat.protocol.xmpp.listener.DefaultRosterListener;
import com.alan.smart.chat.protocol.xmpp.listener.DefaultRosterLoadedListener;
import com.alan.smart.chat.protocol.xmpp.listener.DefaultSubscribeListener;
import com.alan.smart.chat.util.XMPPUtil;

@Component
@EnableConfigurationProperties(XMPPProperties.class)
public class XMPPClient {

    public static final Logger log = LoggerFactory.getLogger(XMPPClient.class);

    private final XMPPProperties xmppProperties;
    private final XMPPMessageTransmitter xmppMessageTransmitter;

    public XMPPClient(XMPPProperties xmppProperties, XMPPMessageTransmitter xmppMessageTransmitter) {
	this.xmppProperties = xmppProperties;
	this.xmppMessageTransmitter = xmppMessageTransmitter;
    }
    
    public Optional<XMPPTCPConnection> connect(String username, String plainTextPassword) {
	XMPPTCPConnection connection;
	try {
	    EntityBareJid jid = XMPPUtil.createJidForUser(username, xmppProperties.getDomain());
	    XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
		    .setHost(xmppProperties.getHost())
		    .setPort(xmppProperties.getPort())
		    .setXmppDomain(xmppProperties.getDomain())
		    .setUsernameAndPassword(jid.getLocalpart(), plainTextPassword)
		    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
		    .setResource(jid.getResourceOrEmpty())
		    .setSendPresence(true)
		    .build();
	    connection = new XMPPTCPConnection(config);
	    connection.connect();
	} catch (SmackException | IOException | XMPPException | InterruptedException e) {
	    e.printStackTrace();
	    return Optional.empty();
	}
	return Optional.of(connection);
    }

    public void createAccount(XMPPTCPConnection connection, String username, String plainTextPassword) {
	AccountManager accountManager = AccountManager.getInstance(connection);
	accountManager.sensitiveOperationOverInsecureConnection(true);
	try {
	    accountManager.createAccount(Localpart.from(username), plainTextPassword);
	} catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
		| SmackException.NotConnectedException | InterruptedException | XmppStringprepException e) {
	    throw new MessengerException(connection.getUser().toString(), e);
	}
	log.info("Account for user '{}' created.", username);
    }

    public void login(XMPPTCPConnection connection) {
	try {
	    connection.login();
	} catch (XMPPException | SmackException | IOException | InterruptedException e) {
	    log.error("Login to XMPP server with user {} failed.", connection.getUser(), e);

	    EntityFullJid user = connection.getUser();
	    throw new MessengerException(user == null ? "unknown" : user.toString(), e);
	}
	log.info("User '{}' logged in.", connection.getUser());
    }

    public void addIncomingMessageListener(XMPPTCPConnection connection, Session webSocketSession) {
	ChatManager chatManager = ChatManager.getInstanceFor(connection);
	chatManager.addIncomingListener(
		(from, message, chat) -> xmppMessageTransmitter.sendResponse(message, webSocketSession));
	log.info("Incoming message listener for user '{}' added.", connection.getUser());
    }
    
//    public void addOutgoingMessageListener(XMPPTCPConnection connection, Session webSocketSession) {
//   	ChatManager chatManager = ChatManager.getInstanceFor(connection);
//   	chatManager.addOutgoingListener(
//   		(to, messageBuilder, chat) -> xmppMessageTransmitter.sendResponse(message, webSocketSession));
//   	log.info("Outgoing message listener for user '{}' added.", connection.getUser());
////   	MessageBuilder.buildMessageFrom(message, stanza);
//   	
//    }

    public void sendMessage(XMPPTCPConnection connection, String message, String to) {
	ChatManager chatManager = ChatManager.getInstanceFor(connection);
	try {
	    EntityBareJid jid = XMPPUtil.createJidForUser(to, xmppProperties.getDomain());
	    Chat chat = chatManager.chatWith(jid);
	    chat.send(message);
	    log.info("Message sent to user '{}' from user '{}'.", to, connection.getUser());
	} catch (XmppStringprepException | SmackException.NotConnectedException | InterruptedException e) {
	    throw new MessengerException(connection.getUser().toString(), e);
	}
    }
    
    public void createInstantRoom(XMPPTCPConnection connection, String roomName) {
	MultiUserChatManager chatManager = MultiUserChatManager.getInstanceFor(connection);
	try {
	    EntityBareJid jid = XMPPUtil.createJidForRoom(roomName, xmppProperties.getDomain());
	    MultiUserChat muc = chatManager.getMultiUserChat(jid);
	    Resourcepart room = Resourcepart.from(roomName);
	    muc.create(room).makeInstant();
	    log.info("Instant Room: '{}' created from user '{}'.", roomName, connection.getUser());
	} catch (XmppStringprepException | SmackException.NotConnectedException | InterruptedException
		| MucAlreadyJoinedException | MissingMucCreationAcknowledgeException | NotAMucServiceException
		| NoResponseException | XMPPErrorException e) {
	    throw new MessengerException(connection.getUser().toString(), e);
	}
    }
    
    public void createReservedRoom(XMPPTCPConnection connection, String roomName, String[] owners) {
   	MultiUserChatManager chatManager = MultiUserChatManager.getInstanceFor(connection);
   	try {
   	    EntityBareJid jid = XMPPUtil.createJidForRoom(roomName, xmppProperties.getDomain());
   	    MultiUserChat muc = chatManager.getMultiUserChat(jid);
   	    Resourcepart room = Resourcepart.from(roomName);
   	   
   	    Set<Jid> ownerJids = JidUtil.jidSetFrom(owners);
	    muc.create(room)
	    .getConfigFormManager()
	    .setRoomOwners(ownerJids)
	    .submitConfigurationForm();

	    log.info("Reserved Room: '{}' created from user '{}'.", roomName, connection.getUser());
	} catch (MucConfigurationNotSupportedException | XmppStringprepException | SmackException.NotConnectedException
		| InterruptedException | MucAlreadyJoinedException | MissingMucCreationAcknowledgeException
		| NotAMucServiceException | NoResponseException | XMPPErrorException e) {
	    throw new MessengerException(connection.getUser().toString(), e);
	}
    }

    public void addContact(XMPPTCPConnection connection, String to) {
	Roster roster = Roster.getInstanceFor(connection);

	if (!roster.isLoaded()) {
	    try {
		roster.reloadAndWait();
	    } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException
		    | InterruptedException e) {
		log.error("XMPP error. Disconnecting and removing session...", e);
		throw new MessengerException(connection.getUser().toString(), e);
	    }
	}

	try {
	    BareJid contact = JidCreate.bareFrom(to + "@" + xmppProperties.getDomain());
	    roster.createItemAndRequestSubscription(contact, to, null);
	    log.info("Contact '{}' added to user '{}'.", to, connection.getUser());
	} catch (XmppStringprepException | XMPPException.XMPPErrorException | SmackException.NotConnectedException
		| SmackException.NoResponseException | SmackException.NotLoggedInException | InterruptedException e) {
	    log.error("XMPP error. Disconnecting and removing session...", e);
	    throw new MessengerException(connection.getUser().toString(), e);
	}
    }

    public Set<RosterEntry> getContacts(XMPPTCPConnection connection) {
	Roster roster = Roster.getInstanceFor(connection);
	roster.addRosterListener(new DefaultRosterListener());
	roster.addRosterLoadedListener(new DefaultRosterLoadedListener());
	roster.addPresenceEventListener(new DefaultPresenceEventListener());
	roster.addSubscribeListener(new DefaultSubscribeListener());
	roster.setSubscriptionMode(SubscriptionMode.accept_all);
	
	if (!roster.isLoaded()) {
	    try {
		roster.reloadAndWait();
	    } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException
		    | InterruptedException e) {
		log.error("XMPP error. Disconnecting and removing session...", e);
		throw new MessengerException(connection.getUser().toString(), e);
	    }
	}
	return roster.getEntries();
    }

    public void disconnect(XMPPTCPConnection connection) {
	Presence presence = PresenceBuilder.buildPresence().ofType(Presence.Type.unavailable).build();
	try {
	    connection.sendStanza(presence);
	} catch (SmackException.NotConnectedException | InterruptedException e) {
	    log.error("XMPP error.", e);

	}
	connection.disconnect();
	log.info("Connection closed for user '{}'.", connection.getUser());
    }

    public void sendStanza(XMPPTCPConnection connection, Presence.Type type) {
	Presence presence = PresenceBuilder.buildPresence().ofType(type).build();
	try {
	    connection.sendStanza(presence);
	    log.info("Status {} sent for user '{}'.", type, connection.getUser());
	} catch (SmackException.NotConnectedException | InterruptedException e) {
	    log.error("XMPP error.", e);
	    throw new MessengerException(connection.getUser().toString(), e);
	}
    }
}
