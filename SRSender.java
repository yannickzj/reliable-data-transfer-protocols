import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class SRSender {

    private static final int ACK_SIZE = 12;
    private static final int BUFFER_SIZE = 500;
    private static final int HEADER_SIZE = 12;
    private static final int SEQNUM_MODULO = 256;
    private final Semaphore available = new Semaphore(10);

    private FileInputStream fileStream;

    static DatagramSocket socket;
    static InetAddress channelAddress;
    static int port;

    private Deque<TimerPacket> queue;
    private Map<Integer, TimerPacket> map;

    static int timeout;

    private int base;
    private int nextSeqNum;

    private volatile boolean sendFinished;

    SRSender(String file, String hostname, int channelPort, int t) throws Exception {
        timeout = t;
        base = 0;
        nextSeqNum = 0;
        port = channelPort;
        channelAddress = InetAddress.getByName(hostname);
        queue = new ArrayDeque<>();
        map = new HashMap<>();
        fileStream = new FileInputStream(file);
        sendFinished = false;
    }

    private void receivePackets() {
        byte[] buffer = new byte[ACK_SIZE];
        DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
        Packet packet;

        while (!sendFinished || !queue.isEmpty()) {
            try {
                // get ack number
                socket.receive(receiveDatagram);
                packet = Packet.getPacket(receiveDatagram.getData());
                System.out.println(String.format("PKT RECV ACK %s %s", packet.getLength(), packet.getSeqNum()));
                int ackNum = packet.getSeqNum();

                // mark that packet as having been received in the window
                if (map.containsKey(ackNum)) {
                    TimerPacket timerPacket = map.get(ackNum);
                    timerPacket.stopTimer();

                    // move forward the window if ackNum == base
                    if (ackNum == base) {
                        while (!queue.isEmpty() && queue.peek().isAck()) {
                            timerPacket = queue.poll();
                            map.remove(timerPacket.getPacket().getSeqNum());
                            available.release();
                        }
                        base = (timerPacket.getPacket().getSeqNum() + 1) % SEQNUM_MODULO;
                    }
                }

            } catch (Exception e) {
                System.out.println("Exception when receiving datagram packet");
            }
        }
    }

    public void start() throws Exception {

        // create socket to send and receive data
        socket = new DatagramSocket();

        // create new thread to receive ACK packets
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receivePackets();
            }
        });
        receiveThread.start();

        // send file data
        System.out.println("Start to send file data");
        while (true) {
            // make packet with individual timer
            byte[] buffer = new byte[BUFFER_SIZE];
            int readNum = fileStream.read(buffer, 0, BUFFER_SIZE);
            if (readNum < 0) {
                sendFinished = true;
                break;
            }
            Packet packet = new Packet(0, readNum + HEADER_SIZE, nextSeqNum, buffer);
            TimerPacket timerPacket = new TimerPacket(packet);

            // send the packet and start its timer
            available.acquire();
            queue.offer(timerPacket);
            map.put(nextSeqNum, timerPacket);
            timerPacket.startTimer();
            Util.sendData(packet, channelAddress, port, socket);

            // update nextSeqNum
            nextSeqNum = (nextSeqNum + 1) % SEQNUM_MODULO;
        }

        // join the receive thread
        receiveThread.join();

        // end sender session
        Util.endSenderSession(base, channelAddress, port, socket);
        System.out.println("Finish sending file");
        socket.close();
        fileStream.close();
    }
}