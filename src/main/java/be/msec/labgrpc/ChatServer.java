package be.msec.labgrpc;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ChatServer {

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private final int portNumber;
    static ArrayList<Message> history;
    private final Server server;
    private static LinkedHashSet<StreamObserver<Message>> observers = new LinkedHashSet<>();

    public ChatServer(int portNumber) throws IOException {
        this(ServerBuilder.forPort(portNumber), portNumber);
    }

    public ChatServer(ServerBuilder<?> serverBuilder, int portNumber) {
        this.portNumber = portNumber;
        if (history == null) {
            history = new ArrayList<Message>();
        }
        server = serverBuilder.addService(new ChatService()).build();
    }

    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + portNumber);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ChatServer.this.stop();
                System.err.println("*** server shut down");
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

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer server = new ChatServer(1000);
        server.start();
        server.blockUntilShutdown();
    }

    private static class ChatService extends ChatServiceGrpc.ChatServiceImplBase{
        @Override
        public StreamObserver<Message> broadCast(StreamObserver<Message> responseObserver) {
            observers.add(responseObserver);
            return new StreamObserver<Message>() {
                @Override
                public void onNext(Message value) {
                    for (StreamObserver<Message> observer: observers){
                        observer.onNext(Message.newBuilder().setTxt(value.getTxt()).build());
                        logger.log(Level.INFO, "Message: " + value.getTxt());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Encountered error in broadcasting", t);
                    observers.remove(responseObserver);
                }

                @Override
                public void onCompleted() {
                    observers.remove(responseObserver);
                }
            };
        }
    }
}
