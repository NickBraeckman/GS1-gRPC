package be.msec.labgrpc.server;


import be.msec.labgrpc.*;
import be.msec.labgrpc.exceptions.DuplicateUsernameException;
import be.msec.labgrpc.exceptions.UserNotFoundException;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {
    public static final String PRIVATE_MESSAGE_ID = "[PRIVATE";
    public static final String PUBLIC_MESSAGE_ID = "[BROADCAST";
    public static final String MESSAGE_TYPE_REGEX = ":";

    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
    private static final Object PUBLIC_MSG_MUTEX = new Object();
    private static final Object NEW_USER_MUTEX = new Object();
    private static final Object PRIVATE_MSG_MUTEX = new Object();


    private final int portNumber;
    private static UserManager userManager;
    private final Server server;
    private static boolean isRunning;

    public ChatServer(int portNumber) {
        this(ServerBuilder.forPort(portNumber), portNumber);
    }

    public ChatServer(ServerBuilder<?> serverBuilder, int portNumber) {
        this.portNumber = portNumber;
        if (userManager == null) {
            userManager = new UserManager();
        }
        server = serverBuilder.addService(new ChatService()).build();
    }

    public void start() throws IOException {
        server.start();
        isRunning = true;
        info("Server started, listening on " + portNumber);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.log(Level.SEVERE, "gRPC server shutting down (JVM is shutting down)");
            isRunning = false;
            ChatServer.this.stop();
            LOGGER.log(Level.SEVERE, "gRPC server is shutdown");
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static void info(String msg, @Nullable Object... params) {
        LOGGER.log(Level.INFO, msg, params);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer server = new ChatServer(1000);
        server.start();
        server.blockUntilShutdown();
    }

    private static class ChatService extends ChatServiceGrpc.ChatServiceImplBase {
        /*  -------------------------------- CONNECT/DISCONNECT -------------------------------- */
        @Override
        public void connectUser(UserInfo userInfo, StreamObserver<ConnectMessage> responseObserver) {
            try {
                LOGGER.log(Level.INFO, userInfo.getName() + " is connecting to server.");
                userManager.connectUser(userInfo.getName()); //TODO user mutex

                responseObserver.onNext(ConnectMessage.newBuilder().setUsername(userInfo.getName()).setIsConnected(true).build());
                responseObserver.onCompleted();
                LOGGER.log(Level.INFO, userInfo.getName() + " is connected to server.");
            } catch (DuplicateUsernameException e) {
                responseObserver.onNext(ConnectMessage.newBuilder().setIsConnected(false).build());
                LOGGER.log(Level.WARNING, userInfo.getName() + " failed to connect to server.");
                responseObserver.onCompleted();
            }
        }

        @Override
        public void disconnectUser(UserInfo userInfo, StreamObserver<DisconnectMessage> responseObserver) {
            try {
                LOGGER.log(Level.INFO, userInfo.getName() + " is disconnecting from server.");
                userManager.disconnectUser(userInfo.getName());

                responseObserver.onNext(DisconnectMessage.newBuilder().setUsername(userInfo.getName()).setIsDisconnected(true).build());
                responseObserver.onCompleted();
                LOGGER.log(Level.INFO, userInfo.getName() + " is disconnected from server.");
            } catch (UserNotFoundException e) {
                responseObserver.onNext(DisconnectMessage.newBuilder().setIsDisconnected(false).build());
                LOGGER.log(Level.WARNING, userInfo.getName() + " not found.");
                responseObserver.onCompleted();
            }
        }

        /*  -------------------------------- SENDING MESSAGES -------------------------------- */
        // send a message to all users
        // put a message in the message list, that is accessible by all users, and notify the sync method
        @Override
        public void sendBroadcastMsg(MessageText messageText, StreamObserver<Empty> responseObserver) {
            try {
                //GATHERING INFO
                User sender = userManager.findUserByName(messageText.getSender());
                //MESSAGE
                Message msg = new Message(sender, MessageType.BROADCAST, PUBLIC_MESSAGE_ID+MESSAGE_TYPE_REGEX+messageText.getText());
                userManager.addToMessages(msg, PUBLIC_MSG_MUTEX);
                LOGGER.log(Level.INFO, msg.toString());
                //RESPONSE OBSERVER
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } catch (UserNotFoundException e) {
                e.printStackTrace();
                responseObserver.onCompleted();
            }
        }

        @Override
        public void sendPrivateMsg(PrivateMessageText privateMessageText, StreamObserver<Empty> responseObserver) {
            try {
                //GATHERING INFO
                MessageText mt = privateMessageText.getMessageText();
                User sender = userManager.findUserByName(mt.getSender());
                User uReceiver = userManager.findUserByName(privateMessageText.getReceiver());
                String sReceiver = uReceiver.toString();
                //MESSAGE
                Message msg = new Message(sender, MessageType.PRIVATE, PRIVATE_MESSAGE_ID+MESSAGE_TYPE_REGEX+mt.getText(), sReceiver);
                userManager.addToMessages(msg, PUBLIC_MSG_MUTEX);
                LOGGER.log(Level.INFO, msg.toString());

                //RESPONSE OBSERVER
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } catch (UserNotFoundException e) {
                e.printStackTrace();
                responseObserver.onCompleted();
            }
        }

        /*  -------------------------------- GETTING MESSAGES -------------------------------- */
        // synchronize message list of all users, so that they receive the latest message
        // mutex will wait until a message is added to the list
        @Override
        public void syncMessages(UserInfo userInfo, StreamObserver<MessageText> responseObserver) {
            while (isRunning) {
                synchronized (PUBLIC_MSG_MUTEX) {
                    try {
                        PUBLIC_MSG_MUTEX.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseObserver.onCompleted();
                    }
                }
                // check if their is a message that belongs to the user
                Message msg = userManager.getLastMessage(userInfo.getName());
                info("Synchronize... : " + msg);
                responseObserver.onNext(
                        MessageText
                                .newBuilder()
                                .setSender(msg.getSender().getName())
                                .setText(msg.getContent()).build());
            }
        }


        @Override
        public void syncUserList(Empty empty, StreamObserver<UserInfo> responseObserver) {
            List<String> onlineUsers = userManager.getOnlineUsers();
            for (String s : onlineUsers) {
                responseObserver.onNext(UserInfo.newBuilder().setName(s).build());
            }

            while (isRunning) {
                synchronized (NEW_USER_MUTEX) {
                    try {
                        NEW_USER_MUTEX.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseObserver.onCompleted();
                    }

                    onlineUsers = userManager.getOnlineUsers();
                    for (String s : onlineUsers) {
                        responseObserver.onNext(UserInfo.newBuilder().setName(s).build());
                    }
                }
            }
        }
    }
}
