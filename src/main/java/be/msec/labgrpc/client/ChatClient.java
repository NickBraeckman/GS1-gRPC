package be.msec.labgrpc.client;

import be.msec.labgrpc.*;
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

    public void leave() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void connectUser(String username) {
        UserCredentials credentials = UserCredentials.newBuilder().setName(username).build();
        ConnectMessage response;
        try {
            response = blockingStub.connectUser(credentials);
            if (response.getIsConnected()) {
                user = new User(username);
                info("Successfully connected to server.");
                Platform.runLater(() -> messages.add(new Message(user, MessageType.CONNECTION_SUCCESS, "").getTextFormat()));
                sendBroadcast(new Message(user, MessageType.CONNECTION_SUCCESS_BROADCAST, "").getTextFormat());
            } else {
                info("Username already taken, choose another one.");
                Platform.runLater(() -> messages.add("Username already taken, choose another one."));
            }
        } catch (StatusRuntimeException e) {
            error(e.getMessage());
        }
    }

    // check if their are new message's in the server's message list
    public void sync() {
        StreamObserver<MessageText> observer = new StreamObserver<MessageText>() {
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
            asyncStub.syncMessages(Empty.newBuilder().build(), observer);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    // send a message
    // add the message to the shared message's list at the serverside
    public void sendBroadcast(String text) {

        MessageText messageText = MessageText.newBuilder().setText(text).setSender(user.getName()).build();

        try {
            info("Broadcasting...");
            blockingStub.sendBroadcast(messageText);
        } catch (StatusRuntimeException e) {
            error(e.getMessage());
            Platform.runLater(() -> messages.add("Could not connect with server. Try again."));
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
