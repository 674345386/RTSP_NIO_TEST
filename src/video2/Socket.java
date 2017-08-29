//package video2;
//
//import java.io.*;
//
///**
// * Created by CP on 2017/1/9. 14:23.
// */
//public class Socket {
//
//    public  static void main(String[] args){
//        try {
//            java.net.Socket socket = new java.net.Socket("192.168.1.144",8888);
//            OutputStream os =socket.getOutputStream();
//            BufferedWriter bw =new BufferedWriter(new OutputStreamWriter(os));
//            bw.write("frme cp");
//            bw.flush();
////            PrintWriter printWriter = new PrintWriter(os);
////            printWriter.write("frome cp");
////            printWriter.flush();
////            printWriter.close();
////            socket.shutdownInput();
////            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//
//        }
//    }
//}
