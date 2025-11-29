import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import networkMessages.ErrorMessage;
import networkMessages.OkMessage;
import networkMessages.ReadChunk;
import networkMessages.RegisterDataServerMessage;

public class DataServer {
    private final String uuid;
    private final String nameServerIPAddress;
    private final int nameServerPort;
    private final int dataServerPort;
    private final String DATA_DIR;
    private long freeSpace;
    private long occupiedSpace;
    private String localIPAddress;

    // We save the connection initiated to the data server to use it later for data transfer.
    private Socket socketToNameServer;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public DataServer(String uuid, String nameServerIPAddress, int nameServerPort, int dataServerPort) {
        this.uuid = uuid;
        this.nameServerIPAddress = nameServerIPAddress;
        this.nameServerPort = nameServerPort;
        this.dataServerPort = dataServerPort;
        this.DATA_DIR = "test_data/" + uuid +"/";
        this.occupiedSpace = 0;
        this.freeSpace = 100 * 1024 * 1024; // 100MB default
        
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(Path.of(DATA_DIR));
            updateOccupiedSpace();
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    public void start() throws IOException {
        // Start server to receive chunks
        try (ServerSocket serverSocket = new ServerSocket(dataServerPort)) {
            System.out.println("DataServer " + uuid + " listening on port " + dataServerPort);

            registerWithNameServer();
            //Socket client = serverSocket.accept();
            //new Thread(() -> handleClient(client)).start();
        }
    }
    public void updateOccupiedSpace(){
        long length = 0;
        File[] files = new File(DATA_DIR).listFiles();

        assert files != null;
        int count = files.length;

        for (File file : files) {
            if (file.isFile()) {
                length += file.length();
            }
        }
        this.occupiedSpace = length;
    }

    private void registerWithNameServer() {
        try (Socket socket = new Socket(nameServerIPAddress, nameServerPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            
            // Get local IP from the socket
            localIPAddress = socket.getLocalAddress().getHostAddress();
            
            RegisterDataServerMessage msg = new RegisterDataServerMessage(
                uuid, localIPAddress, dataServerPort, freeSpace, occupiedSpace);
            oos.writeObject(msg);
            oos.flush();
            
            Object response = ois.readObject();
            if (response instanceof OkMessage) {
                System.out.println("Successfully registered with NameServer");
                this.ois = ois;
                this.oos = oos;
                this.socketToNameServer = socket;

                //Vu qu'on ne se connecte qu'une seule fois au nameServer, we can do it only once in a blocking way
                //Sorry for the English-French mix.
                handleClient(this.ois,this.oos);
            } else if (response instanceof ErrorMessage error) {
                System.err.println("Failed to register with NameServer: " + error.message);
            }
        } catch (Exception e) {
            System.err.println("Error registering with NameServer: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

//    private void handleClient(Socket client) {
//        try (Socket c = client;
//             ObjectOutputStream oos = new ObjectOutputStream(c.getOutputStream());
//             ObjectInputStream ois = new ObjectInputStream(c.getInputStream())){
//            handleClient(ois,oos);
//        } catch (IOException e) {
//            System.err.println("Error handling data client : "+e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }

    private void handleClient(ObjectInputStream ois, ObjectOutputStream oos){
        try {
            Object obj;
            while (true) {
                try {
                    obj = ois.readObject();
                } catch (EOFException e) {
                    e.printStackTrace();
                    break;
                }

                if (obj instanceof Chunk chunk) {
                    try {
                        writeChunk(chunk);
                        oos.writeObject(new OkMessage()); //TODO : meilleur accusé de réception ?
                        oos.flush();
                    } catch (IOException e) {
                        oos.writeObject(new ErrorMessage("Failed to write chunk: " + e.getMessage(),
                                chunk.fileName, chunk.chunkID));
                        oos.flush();
                    }
                }else if (obj instanceof ReadChunk readChunk) {
                    try{
                        Chunk chunk = readChunk(readChunk.fileID, readChunk.chunkID);
                        oos.writeObject(chunk);
                    } catch (FileNotFoundException e) {
                        oos.writeObject(new ErrorMessage("Chunk not found on this server"));
                    } catch (IOException e){
                        oos.writeObject(new ErrorMessage("Random IOexception occured, sorry"));
                    } finally {
                        oos.flush();
                    }
                } else {
                    oos.writeObject(new ErrorMessage("Unknown message"));
                    oos.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void writeChunk(Chunk chunk) throws IOException {
        final String fileName = String.valueOf(chunk.fileID) + '-' + String.valueOf(chunk.chunkID);
        Path filePath = Path.of(DATA_DIR, fileName);
        
        try {
            Files.write(filePath, chunk.getData());
            // Update space tracking
            long chunkSize = chunk.getData().length;
            occupiedSpace += chunkSize;
            freeSpace -= chunkSize;
            System.out.println("Written chunk " + chunk.chunkID + " of file " + chunk.fileID + 
                " (" + chunkSize + " bytes)");
                acknowledgeToLeader(chunk.fileID, chunk.chunkID);
        } catch (IOException e) {
            System.err.println("Failed to write chunk: " + e.getMessage());
            throw e;
        }
    }

    // TODO: readChunk method

    /**
     * Given a fileID and a chunk Id, build the corresponding java object
     * @param fileID
     * @param chunkID
     * @return Chunk with data set to the content of that chunk.
     * @throws IOException
     */
    private Chunk readChunk(long fileID, int chunkID) throws IOException {
        final String fileName = String.valueOf(fileID) + '-' + String.valueOf(chunkID);
        return
                new Chunk(
                        fileID,
                        null,
                        chunkID,
                        Files.readAllBytes(Path.of(DATA_DIR , fileName))
                );
    }

    private void acknowledgeToLeader(long fileID, int chunkID) {
        // TODO: Implement acknowledgment to NameServer if needed
    }
}
