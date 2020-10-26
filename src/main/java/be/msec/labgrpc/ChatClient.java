package be.msec.labgrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private final ManagedChannel channel;
    //private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private StreamObserver<Message> responseObserver;
    private StreamObserver<Message> requestObserver;

    public ChatClient(String hostname, int portNumber){this(ManagedChannelBuilder.forAddress(hostname,portNumber).usePlaintext(true));}
    public ChatClient(ManagedChannelBuilder<?> channelBuilder){
        channel = channelBuilder.build();
        //blockingStub = ChatServiceGrpc.newBlockingStub(channel);
        asyncStub = ChatServiceGrpc.newStub(channel);
    }

    public void leave() throws InterruptedException{
        requestObserver.onCompleted();
        responseObserver.onCompleted();
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void broadcastingMessage(String message) throws InterruptedException {
        info("Broadcasting message");
        final CountDownLatch finishLatch = new CountDownLatch(1);

         responseObserver = new StreamObserver<Message>() {
            @Override
            public void onNext(Message value) {
                info("Message: " + message);
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "Broadcasting failed", status);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished streaming");
                finishLatch.countDown();
            }
        };

        requestObserver = asyncStub.broadCast(responseObserver);
        try{
            Message msg = Message.newBuilder().setTxt(message).build();
            requestObserver.onNext(msg);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        finishLatch.await(1, TimeUnit.SECONDS);

    }

    public static void main(String[] args) throws InterruptedException {
        ChatClient client = new ChatClient("localhost",1000);
        try {
            while (true){
            client.broadcastingMessage("msg");
            }
        } finally {
            client.leave();
        }
    }

    private static void info(String msg, Object... params){logger.log(Level.INFO,msg,params);}
}
