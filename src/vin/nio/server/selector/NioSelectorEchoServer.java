package vin.nio.server.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioSelectorEchoServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.bind(new InetSocketAddress("localhost", 7000));

        for (int i = 0; i < 10; i++) {
            executorService.submit(
                    new SelectorRunnable(serverSocketChannel)
            );

//        serverSocketChannel.close();
//        System.out.println("Echo server finished");
        }
    }
}


class SelectorRunnable implements Runnable {
    ServerSocketChannel serverSocketChannel;
    boolean active = true;

    public SelectorRunnable(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }


    @Override
    public void run() {
        System.out.println("Echo server started: {}" + serverSocketChannel);
        try {

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (active) {

                int selected = selector.select(); // blocking
                System.out.println("selected: {} key(s)" + selected);

                Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
                while (keysIterator.hasNext()) {
                    try {
                        SelectionKey key = keysIterator.next();

                        if (key.isAcceptable()) {
                            accept(selector, key);
                        }
                        if (key.isReadable()) {
                            keysIterator.remove();
                            read(selector, key);
                        }
//                        if (key.isWritable()) {
//                            keysIterator.remove();
//                            write(selector, key);
//                        }

                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept(); // can be non-blocking
        if (socketChannel != null) {
            System.out.println("Connection is accepted: {}" + socketChannel);

            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }

    private void read(Selector selector, SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = socketChannel.read(buffer); // can be non-blocking
        System.out.println("Echo server read: {} byte(s)" + read);

//        if (read < 0)
//            active = false;

        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        if (message.equals("Testing 01"))
            Thread.sleep(30000);

        System.out.println("Echo server received: {}" + message);

        buffer.flip();
        socketChannel.write(ByteBuffer.wrap(bytes));
    }

//    private void write(Selector selector, SelectionKey key) throws IOException {
//        SocketChannel socketChannel = (SocketChannel) key.channel();
//        ByteBuffer buffer = (ByteBuffer) key.attachment();
//        socketChannel.write(buffer); // can be non-blocking
////        socketChannel.close();
//
//        buffer.flip();
//        byte[] bytes = new byte[buffer.limit()];
//        buffer.get(bytes);
//        String message = new String(bytes, StandardCharsets.UTF_8);
//        System.out.println("Echo server sent: {}" + message);
//        socketChannel.register(selector, SelectionKey.OP_READ, bytes);
//    }
}
