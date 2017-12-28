import java.nio.ByteBuffer;

public class Packet {
    private static final int MAX_SIZE = 512;
    private static final int HEADER_SIZE = 12;

    private int type;
    private int length;
    private int seqNum;
    private byte[] data;

    Packet(int type, int length, int seqNum, byte[] data) throws Exception {
        if (length > MAX_SIZE) {
            throw new Exception("too large packet (max size 512)");
        }
        this.type = type;
        this.length = length;
        this.seqNum = seqNum;
        this.data = data;
    }

    public static Packet createACK(int seqNum) throws Exception {
        return new Packet(1, HEADER_SIZE, seqNum, new byte[0]);
    }

    public static Packet createEOT(int seqNum) throws Exception {
        return new Packet(2, HEADER_SIZE, seqNum, new byte[0]);
    }

    public int getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putInt(type);
        buffer.putInt(length);
        buffer.putInt(seqNum);
        buffer.put(data, 0, length - HEADER_SIZE);
        return buffer.array();
    }

    public static Packet getPacket(byte[] bytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int type = buffer.getInt();
        int length = buffer.getInt();
        int seqNum = buffer.getInt();
        if (length > HEADER_SIZE) {
            byte[] data = new byte[length - HEADER_SIZE];
            buffer.get(data, 0, length - HEADER_SIZE);
            return new Packet(type, length, seqNum, data);
        } else {
            return new Packet(type, length, seqNum, new byte[0]);
        }
    }
}
