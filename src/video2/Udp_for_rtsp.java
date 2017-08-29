package video2;

import java.io.IOException;
import java.net.*;
import java.security.PrivateKey;

/**
 * Created by CP on 2017/1/10. 14:45.
 */
public class Udp_for_rtsp {

    private static DatagramSocket ds;

    Udp_for_rtsp send_rtp(int LocalPort, int remotePort, String desIp) {

        try {
            ds = new DatagramSocket(LocalPort);
        } catch (SocketException e) {
            e.printStackTrace();
            System.err.println("cannot open port");
        }
        byte[] buf = new byte[]{(byte) 0xce, (byte) 0xfa, (byte) 0xed, (byte) 0xfe};
        InetAddress des = null;
        try {
            des = InetAddress.getByName(desIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        DatagramPacket dp = new DatagramPacket(buf, 4, des, remotePort);
        try {
            ds.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ds.close();
        }
        return this;
    }

    public static void main(String[] args) {
        new Udp_for_rtsp().send_rtp(5666, 5432, "120.26.39.36").send_rtcp(5666, 5432, "120.26.39.36").send_rtp(5666, 5432, "120.26.39.36").send_rtp(5666, 5432, "120.26.39.36");
    }

    Udp_for_rtsp send_rtcp(int Localport, int remotePort, String desIp) {
        try {
            ds = new DatagramSocket(Localport);

        } catch (SocketException e) {
            e.printStackTrace();
            System.err.println("cannot open port " + Localport);
        }

        byte[] buf = new byte[]{(byte) 0xce, (byte) 0xfa, (byte) 0xed, (byte) 0xfe};
        InetAddress des = null;

        try {
            des = InetAddress.getByName(desIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        DatagramPacket dp = new DatagramPacket(buf, 4, des, remotePort);
        try {
            ds.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ds.close();
        }
        return this;
    }
}
