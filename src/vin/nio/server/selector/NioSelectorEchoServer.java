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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioSelectorEchoServer {

    private static boolean active = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        ExecutorService executorService= Executors.newFixedThreadPool(3);

        serverSocketChannel.bind(new InetSocketAddress("localhost", 7000));
        System.out.println("Echo server started: {}" + serverSocketChannel);

        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (active) {
            int selected = selector.select(); // blocking
            System.out.println("selected: {} key(s)" + selected);

            Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                if (key.isAcceptable()) {
                    accept(selector, key);
                }
                if (key.isReadable()) {
                    keysIterator.remove();
                    executorService.submit(()->{
                        try {
                            read(selector, key);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                if (key.isWritable()) {
                    keysIterator.remove();
                    executorService.submit(()->{
                        try {
                            write(selector, key);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }

        serverSocketChannel.close();
        System.out.println("Echo server finished");
    }

    private static void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept(); // can be non-blocking
        if (socketChannel != null) {
            System.out.println("Connection is accepted: {}" + socketChannel);

            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }

    private static void read(Selector selector, SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = socketChannel.read(buffer); // can be non-blocking
        System.out.println("Echo server read: {} byte(s)" + read);

//        if(read<0)
//            active=false;

        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        if(message.equals("Testing 01"))
            Thread.sleep(5000);

        System.out.println("Echo server received: {}" + message);

        buffer.flip();
        socketChannel.register(selector, SelectionKey.OP_WRITE, buffer);
    }

    private static void write(Selector selector, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        socketChannel.write(buffer); // can be non-blocking
//        socketChannel.close();

        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Echo server sent: {}" + message);
        socketChannel.register(selector, SelectionKey.OP_READ, bytes);
    }
}
