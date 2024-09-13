package vin.nio.server.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class NioSelectorEchoServer {

    public static void main(String[] args) throws IOException {
        ExecutorService acceptorService = Executors.newSingleThreadExecutor();
        ExecutorService workerService = Executors.newFixedThreadPool(10);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress("localhost", 7000));

        BlockingQueue<SocketChannel> connectionQueue = new LinkedBlockingQueue<>();

        acceptorService.submit(new AcceptorRunnable(serverSocketChannel, connectionQueue));

        for (int i = 0; i < 10; i++) {
            workerService.submit(new WorkerRunnable(connectionQueue));
        }
    }
}

