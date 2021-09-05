package me.wuys.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @author: wys
 */
public class NioServer {
    public static void main(String[] args) throws Exception {
        Selector serverSelector = Selector.open();
        Selector clientSelector = Selector.open();

        new Thread(() -> {
            try {
                //服务端的启动
                ServerSocketChannel listenChannel = ServerSocketChannel.open();
                listenChannel.socket().bind(new InetSocketAddress(8000));
                listenChannel.configureBlocking(false);
                listenChannel.register(serverSelector, SelectionKey.OP_ACCEPT);

                while (true) {
                    //监听是否有新的连接，这里1指的是阻塞时间为1ms
                    if (serverSelector.select(1) > 0) {
                        Set<SelectionKey> set = serverSelector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = set.iterator();

                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();

                            if (key.isAcceptable()) {
                                try {
                                    // (1) 每来一个新连接，不需要创建一个线程，而是直接注册到clientSelector
                                    SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
                                    clientChannel.configureBlocking(false);
                                    clientChannel.register(clientSelector, SelectionKey.OP_READ);
                                } finally {
                                    keyIterator.remove();
                                }
                            }

                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();


        new Thread(() -> {
            try {
                while (true) {
                    //批量轮询哪些连接有数据可读，1指的是阻塞时间为1ms
                    if (clientSelector.select(1) > 0) {
                        Set<SelectionKey> set = clientSelector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = set.iterator();

                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            //该连接中是否有数据可读
                            if (key.isReadable()) {
                                try {
                                    SocketChannel clientChannel = (SocketChannel) key.channel();
                                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                                    //nio中数据读取是面向buffer，而io中是面向字节流的
                                    clientChannel.read(buffer);
                                    buffer.flip();
                                    System.out.println(Charset.defaultCharset().newDecoder().decode(buffer).toString());
                                } finally {
                                    keyIterator.remove();
                                    key.interestOps(SelectionKey.OP_READ);
                                }
                            }
                        }
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

    }
}
