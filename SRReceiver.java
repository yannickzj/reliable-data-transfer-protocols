import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class SRReceiver {

    private static final int BUFFER_SIZE = 512;
    private static final int SEQNUM_MODULO = 256;
    private static final int WINDOW_SIZE = 10;

    private int base;

    private Map<Integer, Packet> map;

    private DatagramSocket socket;
    private FileOutputStream fout;

    private InetAddress channelAddress;
    private int channelPort;
    private boolean getChannelInfo;

    SRReceiver(DatagramSocket socket, String file) throws Exception {
        this.socket = socket;
        fout = new FileOutputStream(file);
        base = 0;
        getChannelInfo = false;
        map = new HashMap<>();
    }

    // check if ackNum falls in the receiver's window
    private boolean withinWindow(int ackNum) {
        int distance = ackNum - base;
        if (ackNum < base) {
            distance += SEQNUM_MODULO;
        }
        return distance < WINDOW_SIZE;
    }

    // check if ackNum falls in receiver's previous window
    private boolean withinPrevWindow(int ackNum) {
        int distance = base - ackNum;
        if (base < ackNum) {
            distance += SEQNUM_MODULO;
        }
        return distance <= WINDOW_SIZE && distance > 0;
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
                int ackNum = packet.getSeqNum();
                if (withinWindow(ackNum)) {
                    // send ACK back to sender
                    Util.sendACK(ackNum, channelAddress, channelPort, socket);

                    // if the packet is not previously received, it is buffered
                    if (!map.containsKey(ackNum)) {
                        map.put(ackNum, packet);
                    }

                    // if ackNum == base, move forward the window
                    if (ackNum == base) {
                        while (map.containsKey(ackNum)) {
                            fout.write(map.get(ackNum).getData());
                            map.remove(ackNum);
                            ackNum = (ackNum + 1) % SEQNUM_MODULO;
                        }
                        base = ackNum % SEQNUM_MODULO;
                    }

                } else if (withinPrevWindow(ackNum)) {
                    // if the packet falls in receiver's previous window, send back ACK
                    Util.sendACK(ackNum, channelAddress, channelPort, socket);
                }
            }

        }

        // close socket and file outputstream
        System.out.println("Finish receiving file");
        fout.close();
        socket.close();
    }
}
