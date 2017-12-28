import java.io.*;

public class Sender {

    public static void main(String[] args) throws Exception {
        // check argument number
        if (args.length != 3 || args[0].equals("") || args[1].equals("") || args[2].equals("")) {
            System.out.println("Usage: java Sender <protocol selector> <timeout> <filename>");
            return;
        }

        // get input arguments
        int protocolType = Integer.parseInt(args[0]);
        int timeout = Integer.parseInt(args[1]);
        String fileName = args[2];

        // read channel info
        FileReader fileReader = new FileReader("channelInfo");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = bufferedReader.readLine();
        if (line == null) {
            throw new Exception("empty channel info");
        }
        String[] tokens = line.split("\\s");
        String hostName = tokens[0];
        int port = Integer.parseInt(tokens[1]);
        bufferedReader.close();

        // check input file
        File f = new File(fileName);
        if (!f.exists() || !f.canRead()) {
            throw new Exception("Invalid input file");
        }

        // use appropriate protocol type to send file data
        if (protocolType == 0) {
            System.out.println("GBN protocol");
            GBNSender sender = new GBNSender(fileName, hostName, port, timeout);
            sender.start();
        } else if (protocolType == 1) {
            System.out.println("SR protocol");
            SRSender sender = new SRSender(fileName, hostName, port, timeout);
            sender.start();
        } else {
            throw new Exception("invalid protocol type");
        }
    }
}
