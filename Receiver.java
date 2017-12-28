import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class Receiver {

    private final static int FIX_PORT = 1234;

    public static void main(String[] args) throws Exception {

        // check argument number
        if (args.length != 2 || args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage: java Receiver <protocol selector> <filename>");
            return;
        }

        // get input arguments
        int protocolType = Integer.parseInt(args[0]);
        String fileName = args[1];

        // select available n_port and create UDP socket
        int port = FIX_PORT;
        DatagramSocket socket;
        while(true) {
            try {
                socket = new DatagramSocket(port);
                break;
            } catch (SocketException e) {
                port++;
            }
        }

        // output recvInfo file
        try {
            String recvInfo = getHostName() + " " + port;
            FileOutputStream out = new FileOutputStream("recvInfo");
            out.write(recvInfo.getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // use appropriate protocol type to receive file data
        if (protocolType == 0) {
            System.out.println("GBN protocol");
            GBNReceiver receiver = new GBNReceiver(socket, fileName);
            receiver.start();
        } else if (protocolType == 1) {
            System.out.println("SR protocol");
            SRReceiver receiver = new SRReceiver(socket, fileName);
            receiver.start();
        } else {
            throw new Exception("invalid protocol type");
        }
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

}
