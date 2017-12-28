import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Timer;
import java.util.TimerTask;

public class TimerPacket {
    private Packet packet;
    private Timer timer;
    private boolean ack;
    private final Object lock;

    TimerPacket(Packet packet) {
        this.packet = packet;
        ack = false;
        lock = new Object();
    }

    class TimeoutTask extends TimerTask {
        public void run() {
            synchronized (lock) {
                if (!ack) {
                    byte[] sendData = packet.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            SRSender.channelAddress, SRSender.port);
                    try {
                        SRSender.socket.send(sendPacket);
                    } catch (IOException e) {
                        System.out.println("IOException when TimerPacket sending packet");
                    }
                    System.out.println(String.format("PKT SEND DAT %s %s", packet.getLength(), packet.getSeqNum()));
                    timer.schedule(new TimeoutTask(), SRSender.timeout);
                }
            }
        }
    }

    public Packet getPacket() {
        return packet;
    }

    public boolean isAck() {
        return ack;
    }

    public void startTimer() {
        timer = new Timer();
        timer.schedule(new TimeoutTask(), SRSender.timeout);
    }

    public void stopTimer() {
        synchronized (lock) {
            timer.cancel();
            ack = true;
        }
    }
}
