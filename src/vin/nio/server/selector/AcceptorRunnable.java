package vin.nio.server.selector;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class AcceptorRunnable implements Runnable {
    private final ServerSocketChannel serverSocketChannel;
    private final BlockingQueue<SocketChannel> connectionQueue;

    public AcceptorRunnable(ServerSocketChannel serverSocketChannel, BlockingQueue<SocketChannel> connectionQueue) {
        this.serverSocketChannel = serverSocketChannel;
        this.connectionQueue = connectionQueue;
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocketChannel.accept();
                        if (client != null) {
                            client.configureBlocking(false);
                            connectionQueue.offer(client);
                            System.out.println("Accepted connection from " + client.getRemoteAddress());
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

