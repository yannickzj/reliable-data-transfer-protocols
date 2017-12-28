import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class GBNReceiver {

    private static final int BUFFER_SIZE = 512;
    private static final int SEQNUM_MODULO = 256;

    private int expectedSeqNum;

    private DatagramSocket socket;
    private FileOutputStream fout;

    private InetAddress channelAddress;
    private int channelPort;
    private boolean getChannelInfo;

    GBNReceiver(DatagramSocket socket, String file) throws Exception {
        this.socket = socket;
        fout = new FileOutputStream(file);
        expectedSeqNum = 0;
        getChannelInfo = false;
    }

    public void start() throws Exception {

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);

        System.out.println("Start to receive data");
        while(true) {
            // receive packet
            socket.receive(receiveDatagram);
            Packet packet = Packet.getPacket(receiveDatagram.getData());

            // get channel info
            if (!getChannelInfo) {
                channelAddress = receiveDatagram.getAddress();
                channelPort = receiveDatagram.getPort();
                getChannelInfo = true;
            }

            if (packet.getType() == 2) {
                // end receiver session when receiving EOT
                Util.endReceiverSession(packet, channelAddress, channelPort, socket);
                break;

            } else if (packet.getType() == 0){
                // process data packet
                System.out.println(String.format("PKT RECV DAT %s %s", packet.getLength(), packet.getSeqNum()));
                if (packet.getSeqNum() == expectedSeqNum) {
                    fout.write(packet.getData());
                    Util.sendACK(expectedSeqNum, channelAddress, channelPort, socket);
                    expectedSeqNum = (expectedSeqNum + 1) % SEQNUM_MODULO;
                } else {
                    Util.sendACK(((expectedSeqNum + SEQNUM_MODULO - 1) % SEQNUM_MODULO),
                            channelAddress, channelPort, socket);
                }
            }
        }

        // close socket and file outputstream
        System.out.println("Finish receiving file");
        fout.close();
        socket.close();
    }
}
