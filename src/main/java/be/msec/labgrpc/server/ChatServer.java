package be.msec.labgrpc.server;


import be.msec.labgrpc.*;
import be.msec.labgrpc.client.ChatClient;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private static final Object mutex = new Object();

    private final int portNumber;
    private static ChatServerController serverController;
    private final Server server;
    private static boolean isRunning;

    public ChatServer(int portNumber) {
        this(ServerBuilder.forPort(portNumber), portNumber);
    }

    public ChatServer(ServerBuilder<?> serverBuilder, int portNumber) {
        this.portNumber = portNumber;
        if (serverController == null) {
            serverController = new ChatServerController();
        }
        server = serverBuilder.addService(new ChatService()).build();
    }

    public void start() throws IOException {
        server.start();
        isRunning = true;
        info("Server started, listening on " + portNumber);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                error("*** shutting down gRPC server since JVM is shutting down");
                isRunning = false;
                ChatServer.this.stop();
                error("*** server shut down");
            }
        });
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
        logger.log(Level.INFO, msg, params);
    }

    private static void error(String msg, @Nullable Object... params) {
        logger.log(Level.WARNING, msg, params);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer server = new ChatServer(1000);
        server.start();
        server.blockUntilShutdown();
    }

    private static class ChatService extends ChatServiceGrpc.ChatServiceImplBase {

        // user tries to connect
        @Override
        public void connectUser(UserCredentials request, StreamObserver<ConnectMessage> responseObserver) {
            try {
                info(request.getName() + " is connecting to server.");
                serverController.connectUser(request.getName());
                responseObserver.onNext(ConnectMessage.newBuilder().setUsername(request.getName()).setIsConnected(true).build());
                info(request.getName() + " is connected to server.");
                responseObserver.onCompleted();
            } catch (DuplicateUsernameException e) {
                responseObserver.onNext(ConnectMessage.newBuilder().setIsConnected(false).build());
                info(request.getName() + " failed to connect to server.");
                responseObserver.onCompleted();
            }
        }

        // send a message to all users
        // put a message in the message list, that is accessible by all users, and notify the sync method
        @Override
        public void broadcast(MessageText request, StreamObserver<Empty> responseObserver) {
            try {
                User sender = serverController.findUserByName(request.getSender());
                serverController.addToMessages(new Message(sender, MessageType.BROADCAST, request.getText()), mutex);
                info(request.getSender()+ " is broadcasting:" + request.getText());
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } catch (UserNotFoundException e) {
                e.printStackTrace();
                responseObserver.onCompleted();
            }
        }

        // synchronize message list of all users, so that they receive the latest message
        // mutex will wait until a message is added to the list
        @Override
        public void syncMessages(Empty request, StreamObserver<MessageText> responseObserver) {
            while (isRunning) {
                synchronized (mutex) {
                    try {
                        mutex.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseObserver.onCompleted();
                    }
                }
                Message msg = serverController.getLastMessage();
                info("Synchronize... : " + msg.getTextFormat());
                responseObserver.onNext(MessageText.newBuilder().setSender(msg.getSender().getName()).setText(msg.getTextFormat()).build());
            }
        }
    }
}
