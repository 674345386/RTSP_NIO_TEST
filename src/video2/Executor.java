package video2;

import java.net.InetSocketAddress;

/**
 * Created by CP on 2017/1/15. 22:01.
 */
public class Executor {

    private int localRTSP_port = 2387;
    //    private int[] randomRTP_ports={16268,16269};
    private int port = 16868;
    private String[] randomRTP_ports = {String.valueOf(port), String.valueOf(port + 1)};

    void execute() {
        for (int i = 0; i < 1000; i++) {
            RTSPImpl mRtsp = new RTSPImpl(new InetSocketAddress("120.26.39.36", 7070),
                    new InetSocketAddress("192.168.1.124", localRTSP_port++),
                    "rtsp://120.26.39.36:7070/20_19.sdp");

            mRtsp.setClientRtpPosts(randomRTP_ports);
            port += 2;
            mRtsp.start();
        }

    }

    public static void main(String[] args) {
        new Executor().execute();
    }

}
