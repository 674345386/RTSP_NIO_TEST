//package video2;
//
///**
// * Created by CP on 2017/1/9. 14:05.
// */
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.nio.ByteBuffer;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.Set;
//
//public class Second {
//
//    private int ports[];
////    int ports;
//    private ByteBuffer echoBuffer = ByteBuffer.allocate(1024);
//
//    public Second(int ports[]) throws IOException {
//        this.ports = ports;
//
//        go();
//    }
//
//    static public void main(String args[]) throws Exception {
////        if (args.length <= 0) {
////            System.err.println("Usage: java MultiPortEcho port [port port …]");
////            System.exit(1);
////        }
////
////        int ports[] = new int[args.length];
////
////        for (int i = 0; i < args.length; ++i) {
////            ports[i] = Integer.parseInt(args[i]);
////        }
//        int ports[]={8888};
//        new Second(ports);
//    }
//
//    private void go() throws IOException {
//// Create a new selector
//        Selector selector = Selector.open();
//
//// Open a listener on each port, and register each one
//// with the selector
//        for (int i = 0; i < ports.length; ++i) {
//            ServerSocketChannel ssc = ServerSocketChannel.open();
//            ssc.configureBlocking(false);
//            ServerSocket ss = ssc.socket();
//            InetSocketAddress address = new InetSocketAddress(ports[i]);
//            ss.bind(address);
//
//            SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
//
//            System.out.println("Going to listen on " + ports[i]);
//        }
//        int i=1;
//          boolean isConnect =true;
//
//        while (true) {
//
//            if (isConnect==false) break;
//            System.out.println(i++);
//            int num = selector.select();
//            System.out.println("num = "+num);
//            if (num==0)continue;
//            Set selectedKeys = selector.selectedKeys();
//            System.out.println("size is "+ selectedKeys.size());
//            System.out.println(Arrays.toString(selectedKeys.toArray()));
//            Iterator it = selectedKeys.iterator();
//
//
//            while (it.hasNext()) {
//                SelectionKey key = (SelectionKey) it.next();
//
//                if ((key.readyOps() & SelectionKey.OP_ACCEPT)
//                        == SelectionKey.OP_ACCEPT) {
//// Accept the new connection
//                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
//                    SocketChannel sc = ssc.accept();
//                    sc.configureBlocking(false);
//
//// Add the new connection to the selector
//                    SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
//                    it.remove();
//
//                    System.out.println("Got connection from " + sc);
//                } else if (key.isReadable()) {
//
//                    try {
//
//
//// Read the data
//                        SocketChannel sc = (SocketChannel) key.channel();
//
//// Echo data
//                        int bytesEchoed = 0;
//                        while (true) {
//                            echoBuffer.clear();
//
//
//                            int r = sc.read(echoBuffer);
//
//                            if (r <= 0) {
////                            sc.close();
//                                break;
//                            }
////                        echoBuffer.flip();
//                            System.out.println();
//
////                        echoBuffer.flip();
////
////                        sc.write(echoBuffer);
//                            bytesEchoed += r;
//                        }
//
//                        System.out.println("Echoed " + bytesEchoed + " from " + sc);
////                    key.cancel();
//                        it.remove();
//                    }catch(IOException e){
//                        e.printStackTrace();
//                        key.cancel();
//                    }
//                }
//
//            }
//
//            System.out.println("going to clear");
//            selectedKeys.clear();
//            System.out.println("cleared");
//        }
//    }
//}
