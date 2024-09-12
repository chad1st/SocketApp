package vin.nio.client.channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioChannelEchoClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 7000));
        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
//        String message;
        String[] messages = {"Testing 01", "Testing 02", "Testing 03"};

//        while ((message = stdIn.readLine()) != null) {
        for (String message : messages) {
            Thread.sleep(1000);
            executorService.submit(() -> {
                System.out.println("Echo client started: {}" + socketChannel);
                try {

                    ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                    socketChannel.write(buffer);
                    System.out.println("Echo client sent: {}" + message);

                    int totalRead = 0;
                    while (totalRead < message.getBytes().length) {
                        buffer.clear();

                        int read = socketChannel.read(buffer);
                        System.out.println("Echo client read: {} byte(s)" + read);
                        if (read <= 0)
                            break;

                        totalRead += read;

                        buffer.flip();
                        System.out.println("Echo client received: {}" + StandardCharsets.UTF_8.newDecoder().decode(buffer));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
//        socketChannel.close();
//        executorService.shutdown();
//        System.out.println("Echo client disconnected");

    }
}
