package be.msec.labgrpc.client;

import be.msec.labgrpc.*;
import be.msec.labgrpc.exceptions.UserNotFoundException;
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
    /*  -------------------------------- LOGGER -------------------------------- */
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    /*  -------------------------------- CONNECTION STUFF -------------------------------- */
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;

    /*  -------------------------------- LISTS -------------------------------- */
    private final ObservableList<String> messagesPublic;
    private final ObservableList<String> messagesPrivate;
    private final ObservableList<String> users;

    /*  -------------------------------- USER INFO -------------------------------- */
    private User user;

    /*  -------------------------------- CONSTRUCTORS -------------------------------- */
    public ChatClient(String hostname, int portNumber) {
        this(ManagedChannelBuilder.forAddress(hostname, portNumber).usePlaintext(true));
    }

    public ChatClient(ManagedChannelBuilder<?> channelBuilder) {
        messagesPublic = FXCollections.observableArrayList();
        messagesPrivate = FXCollections.observableArrayList();
        users = FXCollections.observableArrayList();

        /*  -------------------------------- START -------------------------------- */
        channel = channelBuilder.build();
        asyncStub = ChatServiceGrpc.newStub(channel);
        blockingStub = ChatServiceGrpc.newBlockingStub(channel);
        logger.log(Level.INFO, "Client started");
    }

    /*  -------------------------------- CONNECT/DISCONNECT -------------------------------- */
    public boolean connectUser(String username) {
        UserInfo userInfo = UserInfo.newBuilder().setName(username).build();
        ConnectMessage response;
        try {
            response = blockingStub.connectUser(userInfo);
            if (response.getIsConnected()) {
                user = new User(username);
                logger.log(Level.INFO, "Successfully connected to server.");

                Platform.runLater(() -> messagesPublic.add("Welcome to the chat " + username + " !"));

                sendBroadcastMsg(username + " has entered the chat");
                return true;
            } else {
                logger.log(Level.WARNING, "Duplicate username (" + username + ") entered");
                Platform.runLater(() -> messagesPublic.add("Username already taken, choose another one."));
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            logger.log(Level.SEVERE, "Exception" + e.getMessage());
        }
        return false;
    }

    public void disconnectUser() throws InterruptedException {
        UserInfo userInfo = UserInfo.newBuilder().setName(user.getName()).build();
        DisconnectMessage response;
        try {
            response = blockingStub.disconnectUser(userInfo);
            if (response.getIsDisconnected()) {
                logger.log(Level.INFO, "Successfully disconnected from server.");
                sendBroadcastMsg(user.getName() + " has left the chat");
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } else {
                logger.log(Level.WARNING, "Failed to disconnect from server");
                Platform.runLater(() -> messagesPublic.add("Failed to disconnect from server, try again."));
            }
        } catch (StatusRuntimeException | UserNotFoundException e) {
            logger.log(Level.SEVERE, "Exception" + e.getMessage());
        }
    }

    /*  -------------------------------- SENDING MESSAGES -------------------------------- */
    // (send) add the message to the shared message's list at the serverside
    public void sendBroadcastMsg(String text) throws UserNotFoundException {

        if (user != null) {
            MessageText messageText = MessageText.newBuilder().setText(text).setSender(user.getName()).build();
            try {
                info("Broadcasting...");
                blockingStub.sendBroadcastMsg(messageText);
            } catch (StatusRuntimeException e) {
                error(e.getMessage());
                Platform.runLater(() -> messagesPublic.add("Could not connect with server. Try again."));
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
                Platform.runLater(() -> messagesPrivate.add("Could not connect with server. Try again."));
            }
        } else {
            throw new UserNotFoundException("Could not find user");
        }
    }

    /*  -------------------------------- GETTING MESSAGES -------------------------------- */
    // check if their are new message's in the server's message list
    public void syncPublicMessages() {
        StreamObserver<MessageText> observer = new StreamObserver<MessageText>() {
            @Override
            public void onNext(MessageText value) {
                info("Public message received from " + value.getSender() + ".");
                Platform.runLater(() -> messagesPublic.add(value.getText()));
            }

            @Override
            public void onError(Throwable t) {
                error("Server error.");
                Platform.runLater(() -> messagesPublic.add("Server error."));
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncPublicMessages(UserInfo.newBuilder().setName(user.getName()).build(), observer);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }
    public void syncPrivateMessages() {
        StreamObserver<PrivateMessageText> observer = new StreamObserver<PrivateMessageText>() {
            @Override
            public void onNext(PrivateMessageText value) {
                info("Private message received from " + value.getMessageText().getSender() + ".");
                Platform.runLater(() -> messagesPrivate.add(value.getMessageText().getText()));
            }

            @Override
            public void onError(Throwable t) {
                error("Server error.");
                Platform.runLater(() -> messagesPrivate.add("Server error."));
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncPrivateMessages(UserInfo.newBuilder().setName(user.getName()).build(), observer);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }
/*
    public void syncUserList() {
        StreamObserver<MessageText> observer = new StreamObserver<MessageText>() {
            @Override
            public void onNext(MessageText value) {
                info("Public message received from " + value.getSender() + ".");
                Platform.runLater(() -> messagesPublic.add(value.getText()));
            }

            @Override
            public void onError(Throwable t) {
                error("Server error.");
                Platform.runLater(() -> messagesPublic.add("Server error."));
            }

            @Override
            public void onCompleted() {
            }
        };
        try {
            asyncStub.syncPublicMessages(UserInfo.newBuilder().setName(user.getName()).build(), observer);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }
*/



    public ObservableList<String> getPublicMessages() {
        return messagesPublic;
    }

    public ObservableList<String> getPrivateMessages() {
        return messagesPrivate;
    }

    public ObservableList<String> getUsers() {
        return users;
    }

    public User getUser() {
        return user;
    }

    private static void info(String msg, @Nullable Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private static void error(String msg, @Nullable Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
}
