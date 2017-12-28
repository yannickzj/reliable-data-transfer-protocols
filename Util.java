import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Util {

    private static final int ACK_SIZE = 12;

    public static void sendPacket(byte[] bytes, InetAddress address, int port, DatagramSocket socket) {
        try {
            DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length, address, port);
            socket.send(sendPacket);
        } catch (Exception e) {
            System.out.println("Exception when sending packet");
        }
    }

    public static Packet receivePacket(int bufferSize, DatagramSocket socket) throws Exception {
        try {
            byte[] buffer = new byte[bufferSize];
            DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
            socket.receive(receiveDatagram);
            return Packet.getPacket(receiveDatagram.getData());
        } catch (Exception e) {
            System.out.println("Exception when receiving packet");
            throw e;
        }
    }

    public static void sendACK(
            int ackNum, InetAddress channelAddress, int channelPort, DatagramSocket socket) throws Exception {
        Util.sendPacket(Packet.createACK(ackNum).getBytes(), channelAddress, channelPort, socket);
        System.out.println(String.format("PKT SEND ACK 12 %s", ackNum));
    }

    public static void sendData(Packet packet, InetAddress channelAddress, int port, DatagramSocket socket) {
        Util.sendPacket(packet.getBytes(), channelAddress, port, socket);
        System.out.println(String.format("PKT SEND DAT %s %s", packet.getLength(), packet.getSeqNum()));
    }

    public static void endSenderSession(
            int seqNum, InetAddress channelAddress, int port, DatagramSocket socket) throws Exception {
        // send EOT
        sendPacket(Packet.createEOT(seqNum).getBytes(), channelAddress, port, socket);
        System.out.println("PKT SEND EOT 12 " + seqNum);

        // wait for EOT
        while (true) {
            Packet packet = Util.receivePacket(ACK_SIZE, socket);
            if (packet.getType() == 2) {
                System.out.println("PKT RECV EOT 12 " + packet.getSeqNum());
                break;
            } else if (packet.getType() == 1){
                System.out.println("PKT RECV ACK 12 " + packet.getSeqNum());
            }
        }
    }

    public static void endReceiverSession(
            Packet packet,
            InetAddress channelAddress,
            int channelPort,
            DatagramSocket socket) throws Exception {
        System.out.println(String.format("PKT RECV EOT %s %s", packet.getLength(), packet.getSeqNum()));

        // reply EOT
        Util.sendPacket(Packet.createEOT(packet.getSeqNum()).getBytes(), channelAddress, channelPort, socket);
        System.out.println("PKT SEND EOT 12 " + packet.getSeqNum());
    }
}
