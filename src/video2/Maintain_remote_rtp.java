package video2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

/**
 * Created by CP on 2017/1/12. 7:48.
 */
public class Maintain_remote_rtp {
    private Socket TcpSocket;
    private InputStream in;
    private OutputStream out;
    private InetSocketAddress remoteIp;
    private InetSocketAddress localIp;
    private SocketChannel socketChannel;
    private final String TcpMsg;


    private static final byte[] lock = {};
    private static volatile Maintain_remote_rtp instance;

    public static Maintain_remote_rtp getInstance(SocketChannel socketChannel, String msgToBeTransmited) {
        if (null == instance) {
            synchronized (lock) {
                instance = new Maintain_remote_rtp(socketChannel, msgToBeTransmited);
                return instance;
            }
        } else {
            return instance;
        }
    }

    public Maintain_remote_rtp(InetSocketAddress remoteIp, String msgToBeTransmited) {
        this.remoteIp = remoteIp;
        this.TcpMsg = msgToBeTransmited;
    }


    public Maintain_remote_rtp(SocketChannel socketChannel, String msgToBeTransmited) {
        this.socketChannel = socketChannel;
        this.TcpMsg = msgToBeTransmited;
    }

    public void run() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                doSentWithChannel();
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

    }

    private void doSentWithChannel() {
        int writedBytes = 0;
        byte[] bytes = TcpMsg.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        if (socketChannel.isConnected() && byteBuffer.position() != 0) {
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                try {
                    writedBytes = socketChannel.write(byteBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.err.println("写进channel OPTIONS 保活 " + writedBytes + "字节，内容如下:");
                    System.err.println(new String(byteBuffer.array(), 0, writedBytes));
                }
            }
        } else {
            System.err.println("socketChannel断开连接,或者byteBuffer为空内容");
        }

    }

    private void doSent() {
        try {
            out = TcpSocket.getOutputStream();

            StringBuilder sb = new StringBuilder();
//            sb.append("OPTIONS ").append(RTSPImpl);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initSocketConnect() {

        try {
            TcpSocket = new Socket(remoteIp.getAddress(), remoteIp.getPort());
            TcpSocket.setKeepAlive(true);
            TcpSocket.connect(new InetSocketAddress(remoteIp.getAddress(), remoteIp.getPort()));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        Selector selector = null;
        SocketChannel socketChannel_test = null;

        try {
//            while (true){
            socketChannel_test = SocketChannel.open();
            socketChannel_test.socket().bind(new InetSocketAddress("192.168.1.144", 5667));

            socketChannel_test.configureBlocking(false);
            socketChannel_test.connect(new InetSocketAddress("120.26.39.36", 5432));
            System.out.println("连接远程成功");
            //
            selector = Selector.open();

            socketChannel_test.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                int n = 0;
                n = selector.select();//  blocking

                if (n > 0) {
                    System.out.println("有channel就绪");
                    Set selectedKeys = selector.selectedKeys();
                    for (Iterator i = selectedKeys.iterator(); i.hasNext(); ) {
                        SelectionKey sk = (SelectionKey) i.next();
                        i.remove();
                        String res = "";
                        if (sk.isConnectable()) res = res + "now connectable\n";
                        if (sk.isReadable()) res = res + "now readable\n";
                        if (sk.isWritable()) res = res + "now writable\n";
                        //
                        System.out.println(res);
                        if (sk.isConnectable()) {
                            if (!socketChannel_test.isConnected()) {
                                socketChannel_test.finishConnect();
                                System.out.println("完成链接");
                                sk.interestOps(SelectionKey.OP_WRITE);
                            }
                        } else if (sk.isWritable()) {
                            Maintain_remote_rtp m = new Maintain_remote_rtp(socketChannel_test, "msg");
                            m.doSentWithChannel();
                            System.err.println("done");
                            System.exit(0);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}


