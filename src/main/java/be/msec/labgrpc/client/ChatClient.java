package be.msec.labgrpc.client;

import be.msec.labgrpc.*;
import be.msec.labgrpc.UserNotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private User user;
    private String text;
    private final ObservableList<String> messages;
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;

    public ChatClient(String hostname, int portNumber) {
        this(ManagedChannelBuilder.forAddress(hostname, portNumber).usePlaintext(true));
    }

    public ChatClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        asyncStub = ChatServiceGrpc.newStub(channel);
        blockingStub = ChatServiceGrpc.newBlockingStub(channel);
        messages = FXCollections.observableArrayList();
        info("Client started");
    }

    public void connectUser(String username) {
        UserInfo userInfo = UserInfo.newBuilder().setName(username).build();
        ConnectMessage response;
        try {
            response = blockingStub.connectUser(userInfo);
            if (response.getIsConnected()) {
                user = new User(username);
                info("Successfully connected to server.");

                Platform.runLater(() -> messages.add("Welcome to the chat " + username + " !"));

                sendBroadcastMsg(username + " has entered the chat");

            } else {
                info("Username already taken, choose another one.");
                Platform.runLater(() -> messages.add("Username already taken, choose another one."));
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            error(e.getMessage());
        }
    }

    // check if their are new message's in the server's message list
    public void sync() {
        StreamObserver<MessageText> requestObserver = new StreamObserver<MessageText>() {
            @Override
            public void onNext(MessageText value) {
                info("Message received from " + value.getSender() + ".");
                Platform.runLater(() -> messages.add(value.getText()));
            }

            @Override
            public void onError(Throwable t) {
                error("Server error.");
                Platform.runLater(() -> messages.add("Server error."));
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncMessages(UserInfo.newBuilder().setName(user.getName()).build(), requestObserver);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    // send a message
    // add the message to the shared message's list at the serverside
    public void sendBroadcastMsg(String text) throws UserNotFoundException {

        if (user != null) {
            MessageText messageText = MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            try {
                info("Broadcasting...");
                blockingStub.sendBroadcastMsg(messageText);
            } catch (StatusRuntimeException e) {
                error(e.getMessage());
                Platform.runLater(() -> messages.add("Could not connect with server. Try again."));
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    public void sendPrivateMsg(String text, String receiverName) throws UserNotFoundException {

        if (user != null) {
            // make standard message
            MessageText messageText = MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            // make private message intended for receiver
            PrivateMessageText privateMessageText = PrivateMessageText.newBuilder().setMessageText(messageText).setReceiver(receiverName).build();
            try {
                info("Send private message...");
                blockingStub.sendPrivateMsg(privateMessageText);
            } catch (StatusRuntimeException e) {
                error(e.getMessage());
                Platform.runLater(() -> messages.add("Could not connect with server. Try again."));
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    public void leave() throws InterruptedException {
        UserInfo userInfo = UserInfo.newBuilder().setName(user.getName()).build();
        DisconnectMessage response;
        try {
            response = blockingStub.disconnectUser(userInfo);
            if (response.getIsDisconnected()) {
                info("Successfully disconnected from server.");
                sendBroadcastMsg(user.getName() + " has left the chat");
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } else {
                info("Failed to disconnect from server");
                Platform.runLater(() -> messages.add("Failed to disconnect from server, try again."));
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            error(e.getMessage());
        }
    }

    public ObservableList<String> getMessages() {
        return messages;
    }

    private static void info(String msg, @Nullable Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private static void error(String msg, @Nullable Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
}
