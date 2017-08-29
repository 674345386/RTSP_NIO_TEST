package video2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by CP on 2017/1/9. 14:56.
 */
public class RTSPImpl extends Thread implements IEvent {
    private static final String VERSION = " RTSP/1.0\r\n";
    private static final String RTSP_OK = "RTSP/1.0 200 OK";

    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;

    private SocketChannel socketChannel;
    private String msgToBeTransmit;

    //SETUP返回信息中解析的client_port和server_port
    String[] client_ports = {};
    String[] server_ports = {};
    //发送缓冲区
    private final ByteBuffer sendBuffer;
    //接收缓冲区
    private final ByteBuffer recBuffer;

    private static final int BUFFERSIZE = 8192;

    //随机client接收RTP端口
    String clientRtpPorts[] =new String[2];
    //端口选择器

    private Selector selector;
    private String address;

    //当前状态
    private enum Status {
        init, options, describe, setup, play, pause, teardown, playing
    }

    private Status sysStatus;
    private String sessionId;

    //线程结束的标识
    private AtomicBoolean shutdown;

    private int seq = 1;
    private boolean isSended;
    private String trackInfo;


    /**
     * 构造函数 (1
     *
     * @param remoteAddress
     * @param localAddress
     * @param address
     */
    public RTSPImpl(InetSocketAddress remoteAddress, InetSocketAddress localAddress,
                    String address) {
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        this.address = address;

        //init buffer
        sendBuffer = ByteBuffer.allocate(BUFFERSIZE);
        recBuffer = ByteBuffer.allocate(BUFFERSIZE);

        if (selector == null) {
            // 创建新的Selector
            try {
                selector = Selector.open();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        startup();
        sysStatus = Status.init;
        shutdown = new AtomicBoolean(false);
        isSended = false;
    }

    public void startup() {
        try {
            // 打开通道
            socketChannel = SocketChannel.open();
            // 绑定到本地端口
            socketChannel.socket().setSoTimeout(30000);
            socketChannel.configureBlocking(false);
            socketChannel.socket().bind(localAddress);
//            socketChannel.configureBlocking(true);
            if (socketChannel.connect(remoteAddress)) {
                System.out.println("开始建立连接: " + remoteAddress);
            } else {
                System.out.println("line 88 :开始建立连接暂时失败，要后面通过finishConnected完成");
            }
//            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT
                    | SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            System.out.println("端口打开成功");

        } catch (final IOException e1) {
            e1.printStackTrace();
        }
    }

    public void setClientRtpPosts(String[] randomPorts) {
        this.clientRtpPorts[0] = randomPorts[0];
        this.clientRtpPorts[1] = randomPorts[1];
    }

    public void send(byte[] out) {
        if (out == null || out.length < 1) {
            return;
        }
        synchronized (sendBuffer) {
            sendBuffer.clear();
            //把out写进缓存
            sendBuffer.put(out);
            sendBuffer.flip();
        }

        // 发送出去
        try {
            write();
            isSended = true;
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void write() throws IOException {
        int writedBytes = 0;
        if (isConnected()) {
            try {
                //写到channel
//                writedBytes = socketChannel.write(sendBuffer);
                while (sendBuffer.hasRemaining()) {
                    writedBytes = socketChannel.write(sendBuffer);
                }
                System.out.println("line 121 : 写进channel " + writedBytes + " bytes,内容描述如下:");
                System.out.println(new String(sendBuffer.array(), 0, writedBytes));
                System.out.println(sendBuffer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("通道为空或者没有连接上");
        }
    }

    public byte[] recieve() {
        if (isConnected()) {
            try {
                int len = 0;
                int readBytes = 0;

                synchronized (recBuffer) {
                    recBuffer.clear();
                    try {
                        while ((len = socketChannel.read(recBuffer)) > 0) {
                            readBytes += len;
                        }
                    } finally {
                        recBuffer.flip();
                    }
                    if (readBytes > 0) {
                        final byte[] tmp = new byte[readBytes];
                        recBuffer.get(tmp);
                        return tmp;
                    } else {
                        System.out.println("接收到数据为空,重新启动连接");
                        return null;
                    }
                }
            } catch (final IOException e) {
                System.out.println("接收消息错误:");
            }
        } else {
            System.out.println("端口没有连接");
        }
        return null;
    }

    /**
     * 检查 socketChannel是否连接
     *
     * @return
     */
    public boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected();
    }


    /**
     * 检测nio的就绪channel
     */
    private void select() {
        int n = 0;
        try {
            if (selector == null) {
                return;
            }
            n = selector.select(1000);

        } catch (final Exception e) {
            e.printStackTrace();
        }

        // 如果select返回大于0，处理事件
        if (n > 0) {
            Set nowSelectedKeys = selector.selectedKeys();
            System.out.println("line 190 :now selectedKeys set is " + Arrays.toString(nowSelectedKeys.toArray()));
            System.out.println("line 188 : 有" + n + "个channel就绪");
            for (final Iterator<SelectionKey> i = selector.selectedKeys()
                    .iterator(); i.hasNext(); ) {
                // 得到下一个Key
                final SelectionKey sk = i.next();
                String res = "";
                if (sk.isConnectable()) res = res + "sk now connectable\n";
                if (sk.isReadable()) res = res + "sk now readable\n";
                if (sk.isWritable()) res = res + "sk now writable\n";
                System.out.println(res);
                //从已就绪的selectedKey集合中移除当前的sk
                i.remove();
                // 检查其是否还有效
                if (!sk.isValid()) {
                    System.out.println("line 190 :  sk's channel is closed, or its selector is closed");
                    continue;
                }

                // 处理事件
                final IEvent handler = (IEvent) sk.attachment();//?????
                try {
                    if (sk.isConnectable()) {       //相当于   sk.readyOps() & OP_CONNECT != 0
                        handler.connect(sk);
                    } else if (sk.isReadable()) {
                        handler.read(sk);
                    } else {
                        // System.err.println("Ooops");
                    }
                } catch (final Exception e) {
                    handler.error(e);
                    sk.cancel();
                }
            }
        }
        System.out.println("end select()");
    }

    public void shutdown() {
        if (isConnected()) {
            try {
                socketChannel.close();
                System.out.println("端口关闭成功");
            } catch (final IOException e) {
                System.out.println("端口关闭错误:");
            } finally {
                socketChannel = null;
            }
        } else {
            System.out.println("通道为空或者没有连接");
        }
    }

    @Override
    public void run() {
        // 启动主循环流程
        while (!shutdown.get()) {
            try {
                if (isConnected() && (!isSended)) {     //发送了信息，isSended会变成true,接收了相应的回应信息，才变回false
                    switch (sysStatus) {
                        case init:
                            doOption();
                            System.out.println("line 242 : doOption 完毕");
                            break;
                        case options:
                            doDescribe();
                            System.out.println("line 242 : doDescribe 完毕");
                            break;
                        case describe:
                            doSetup();
                            System.out.println("line 242 : doSetup 完毕");
                            break;
                        case setup:
                            if (sessionId == null && sessionId.length() > 0 && client_ports.length <= 0) {
                                System.out.println("setup还没有正常返回 ,或者没解析出其中的ports");
                            } else {
                                beforePlay();
                                doPlay();
                            }
                            break;
                        case play:
                            doKeepServerAlive();
                            isSended = true;
                            doPause();
                            System.err.println("line 294 : doPause 完毕");
                            break;
                        //my own
                        case playing:
                            break;


                        case pause:
                            doTeardown();
                            break;
                        default:
                            break;
                    }
                } else {
                    System.out.println("line 274 : socketChannel 还没连接,或者还没接收服务器回应");
                }
                // do select
                System.out.println("line 277 : do select() in run()");
                select();
                try {
                    Thread.sleep(1000);
                } catch (final Exception e) {
                }
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println("line 286 : 完成一次while() in run()");
            }
        }

        shutdown();
    }

    public void connect(SelectionKey key) throws IOException {
        if (isConnected()) {
            System.out.println("line 285 : 已经connected,不用finishConnect");
            return;
        }
        // 完成SocketChannel的连接
        socketChannel.finishConnect();
        while (!socketChannel.isConnected()) {
            try {
                Thread.sleep(300);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            socketChannel.finishConnect();

        }
        System.out.println("line 297 : 用finishConnect，完成socketChannel连接: " + socketChannel.getLocalAddress() + "----->" + socketChannel.getRemoteAddress());

    }

    public void error(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void read(SelectionKey key) throws IOException {
        // 接收消息
        final byte[] msg = recieve();
        if (msg != null) {
            handle(msg);
        } else {
            key.cancel();
        }
    }

    private void handle(byte[] msg) {
        String tmp = new String(msg);
        System.err.println("返回内容：");
        System.err.println(tmp);
        if (tmp.startsWith(RTSP_OK)) {      //  根据当前的sysStatus状态来，解析返回的信息
            switch (sysStatus) {
                case init:                  //options返回了信息
                    sysStatus = Status.options;
                    break;
                case options:               //describe返回了信息
                    sysStatus = Status.describe;
                    trackInfo = tmp.substring(tmp.indexOf("trackID"), tmp.length() - 2);
                    break;
                case describe:              //setup返回了信息
                    sessionId = tmp.substring(tmp.indexOf("Session: ") + 9, tmp
                            .indexOf("Date:"));
                    if (sessionId != null && sessionId.length() > 0) {
                        sysStatus = Status.setup;
                    }
                    //
                    client_ports = tmp.substring(tmp.indexOf("client_port=") + 12, tmp.indexOf(";server_port")).split("-");
                    server_ports = tmp.substring(tmp.indexOf("server_port=") + 12, tmp.length() - 4).split("-");
                    break;
                case setup:             //play返回了信息
                    sysStatus = Status.play;
                    break;
                case play:              //pause返回了信息
//                    sysStatus = Status.pause;
                    //my own
                    sysStatus = Status.playing;
                    break;
                case pause:
                    sysStatus = Status.teardown;
                    shutdown.set(true);
                    break;
                case teardown:
                    sysStatus = Status.init;
                    break;
                default:
                    break;
            }
            isSended = false;
        } else {
            System.out.println("返回错误：" + tmp);
        }

    }

    private void doTeardown() {
        StringBuilder sb = new StringBuilder();
        sb.append("TEARDOWN ");
        sb.append(this.address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("User-Agent: RealMedia Player HelixDNAClient/10.0.0.11279 (win32)/r/n");
        sb.append("Session: ");
        sb.append(sessionId);
        sb.append("/r/n");
        send(sb.toString().getBytes());
        System.out.println(sb.toString());
    }

    private void doPlay() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("PLAY ");
//        sb.append(this.address);
//        sb.append(VERSION);
//        sb.append("Session: ");
//        sb.append(sessionId);
//        sb.append("Cseq: ");
//        sb.append(seq++);
//        sb.append("/r/n");
//        sb.append("/r/n");
//        System.out.println(sb.toString());
//        send(sb.toString().getBytes());

//        StringBuilder sb = new StringBuilder();
//        sb.append("PLAY ");
//        sb.append(this.address);
//        sb.append(VERSION);
//        sb.append("CSeq: ");
//        sb.append(seq++);
//        sb.append("\r\n");
//        sb.append("User-Agent: LibVLC/2.2.4 (LIVE555 Streaming Media v2016.02.22)\r\n");
//        sb.append("Session: ");
//        sb.append(sessionId);
//        sb.append("\r\n");
//        System.out.println(sb.toString());
//        send(sb.toString().getBytes());

        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ");
        sb.append(this.address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("User-Agent: LibVLC/2.2.4 (LIVE555 Streaming Media v2016.02.22)\r\n");
        sb.append("Session: ");
        sb.append(sessionId);
        sb.append("Range: npt=0.000-\r\n");
        sb.append("\r\n");

        System.out.println(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doSetup() {
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ");
        sb.append(this.address);
        sb.append("/");
        String trackInfo_ = trackInfo;
        sb.append("trackID=1");
        sb.append(VERSION);
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("User-Agent: LibVLC/2.2.4 (LIVE555 Streaming Media v2016.02.22)\r\n");
//        sb.append("Transport: RTP/AVP;UNICAST;client_port=16268-16269;mode=play\r\n");
        sb.append("Transport: RTP/AVP;UNICAST;client_port=" + clientRtpPorts[0] + "-" + clientRtpPorts[1] + ";mode=play\r\n");
//        sb.append("Transport: RTP/AVP;unicast;client_port=54222-54223");
        sb.append("\r\n");
        System.out.println(sb.toString());
        send(sb.toString().getBytes());
    }

    private void beforePlay() {
        new Udp_for_rtsp().send_rtp(Integer.valueOf(client_ports[0]), Integer.valueOf(server_ports[0]), "120.26.39.36").send_rtp(Integer.valueOf(client_ports[1]), Integer.valueOf(server_ports[1]), "120.26.39.36").send_rtp(Integer.valueOf(client_ports[0]), Integer.valueOf(server_ports[0]), "120.26.39.36").send_rtp(Integer.valueOf(client_ports[1]), Integer.valueOf(server_ports[1]), "120.26.39.36");
    }

    private void doOption() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ");
//        sb.append(this.address.substring(0, address.lastIndexOf("/")));
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("User-Agent: LibVLC/2.2.4 (LIVE555 Streaming Media v2016.02.22)\r\n");
        sb.append("\r\n");
//        sb.append("\n");
//        sb.append("\n");
        System.out.println(sb.toString());
        msgToBeTransmit = sb.toString();
        //发送OPTIONS报文
        send(sb.toString().getBytes());
    }

    private void doDescribe() {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("User-Agent: LibVLC/2.2.4 (LIVE555 Streaming Media v2016.02.22)\r\n");
        sb.append("Accept: application/sdp\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doPause() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("PAUSE ");
//        sb.append(this.address);
//        sb.append("/");
//        sb.append(VERSION);
//        sb.append("Cseq: ");
//        sb.append(seq++);
//        sb.append("/r/n");
//        sb.append("Session: ");
//        sb.append(sessionId);
//        sb.append("/r/n");
//        send(sb.toString().getBytes());
//        System.out.println(sb.toString());
    }

    private void doKeepServerAlive() {
        if (null != msgToBeTransmit) {
            Maintain_remote_rtp maintain_remote_rtp = new Maintain_remote_rtp(socketChannel, msgToBeTransmit);
            maintain_remote_rtp.run();
//            Maintain_remote_rtp.getInstance(socketChannel, msgToBeTransmit).run();
        } else {
            System.err.println("the OPTION msg to be Transmit is not set");
        }
    }

    public static void main(String[] args) {
        try {
            // RTSPClient(InetSocketAddress remoteAddress,
            // InetSocketAddress localAddress, String address)
            RTSPImpl client = new RTSPImpl(
                    new InetSocketAddress("120.26.39.36", 5432),
                    new InetSocketAddress("192.168.1.144", 2181),
                    "rtsp://120.26.39.36:5432/ceshi0.sdp");
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }
}
