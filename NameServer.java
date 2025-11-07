
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import exceptions.FileExists;
import networkMessages.CloseFileMessage;
import networkMessages.CreateFileMessage;
import networkMessages.ErrorMessage;
import networkMessages.GetMetadataMessage;
import networkMessages.MetadataResponse;
import networkMessages.OkMessage;

public class NameServer {
    private final String uuid;
    /**
     * The TOTAL number of existence of each chunk, not the number of replicas to which you add a primary.
     */
    private final static short replication = 3;
    private final ConcurrentHashMap<String, FileMetadata> fileMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileMetadata> missingReplicas = new ConcurrentHashMap<>();
    private final ServerStatus[] dataServers = {};
    private long totalFreeSpace = 0;
    /**
     * The next file ID to be used. File Id are never reused, this counter only increases
     */
    private long currentFileIndex = 1;

    public NameServer() {
        //TODO : Read from file or generate
        this.uuid = "";
    }

    private long newFile(String pathName) throws FileExists {
        if (fileMap.putIfAbsent(pathName, new FileMetadata(currentFileIndex, pathName)) != null) {
            throw new FileExists();
        } else {
            currentFileIndex++;
            return currentFileIndex - 1;
        }
    }


    /**
     * Sends the chunk to the data servers, managing load balancing  and replication etc.
     * Write in the FileMetadata the location where the chunk have been stored
     * @param file The fileMetadata objects of the file
     * @param chunk The chunk of data to write.
     */
    private void writeChunk(FileMetadata file, Chunk chunk) throws Exception {
        int nbCopies = 0;
        int nbEssaye = 0;
        while (nbEssaye < dataServers.length && nbCopies <replication){
            ServerStatus tryServer = Quickselect.quickselect(dataServers, nbEssaye);
            nbEssaye++;
            try {
                //TODO : paralléliser cet envoi
                sendChunkToDataServer(tryServer, chunk);
                nbCopies ++;
                file.addServerToChunk(chunk.chunkID, tryServer.uuid);
            } catch (RuntimeException e) {
                //Ce serveur n'était manifestement pas disponible, essayons en un autre.
            }
        }
        if (nbCopies == 0){
            throw new Exception("Could not write the file");
        }

    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private void handleClient(Socket client) {
        try (Socket c = client;
             ObjectOutputStream oos = new ObjectOutputStream(c.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(c.getInputStream())) {

            Object obj;
            while ((obj = ois.readObject()) != null) {
                if (obj instanceof CreateFileMessage msg) {
                    try {
                        newFile(msg.fileName);
                        oos.writeObject(new OkMessage());
                        oos.flush();
                    } catch (FileExists e) {
                        oos.writeObject(new ErrorMessage("File exists"));
                        oos.flush();
                    }
                } else if (obj instanceof GetMetadataMessage msg) {
                    FileMetadata metadata = fileMap.get(msg.fileName);
                    if (metadata == null) {
                        oos.writeObject(new MetadataResponse(false, 0L));
                    } else {
                        oos.writeObject(new MetadataResponse(true, metadata.id));
                    }
                    oos.flush();
                } else if (obj instanceof Chunk chunk) {
                    FileMetadata metadata = fileMap.get(chunk.fileName);
                    if (metadata == null) {
                        oos.writeObject(new ErrorMessage("Unknown file"));
                        oos.flush();
                        continue;
                    }
                    try {
                        writeChunk(metadata, chunk);
                        oos.writeObject(new OkMessage());
                        oos.flush();
                    } catch (Exception e) {
                        oos.writeObject(new ErrorMessage("Write failed"));
                        oos.flush();
                    }
                } else if (obj instanceof CloseFileMessage) {
                    oos.writeObject(new OkMessage());
                    oos.flush();
                    break;
                } else {
                    oos.writeObject(new ErrorMessage("Unknown message"));
                    oos.flush();
                }
            }
        } catch (Exception ignored) {
            System.out.println(ignored.getMessage());
        }
    }

    private void sendChunkToDataServer(ServerStatus server, Chunk chunk) {
        // TODO: implement networking to DataServer
        System.out.println(chunk.toString());
        throw new RuntimeException("Not implemented");
    }




}

