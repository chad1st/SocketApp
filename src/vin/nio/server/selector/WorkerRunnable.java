package vin.nio.server.selector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerRunnable implements Runnable {
    private final BlockingQueue<SocketChannel> connectionQueue;
    private final ExecutorService taskExecutor;

    public WorkerRunnable(BlockingQueue<SocketChannel> connectionQueue) {
        this.connectionQueue = connectionQueue;
        this.taskExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();

            while (true) {
                SocketChannel client = connectionQueue.take();
                client.register(selector, SelectionKey.OP_READ);

                while (true) {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();

                        if (key.isReadable()) {
                            handleRead(key);
                        }
                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int readResult = client.read(buffer);

        if (readResult > 0) {
            buffer.flip();
            String request = new String(buffer.array(), 0, readResult).trim();
            System.out.println(Thread.currentThread().getName() + " - Request received: " + request);

            // Offload long-running task to a separate thread
            taskExecutor.submit(() -> {
                try {
                    if (request.equals("Testing 01")) {
                        Thread.sleep(10000);
                    }

                    String response = "RESPONSE OK";
                    ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes("UTF-8"));
                    client.write(responseBuffer);
                    System.out.println(Thread.currentThread().getName() + " - Response sent: " + response);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else {
            client.close();
            System.out.println(Thread.currentThread().getName() + " - Connection closed by client");
        }
    }
}
