
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import exceptions.*;
import networkMessages.*;

public class NameServer {
    private String uuid;
    /**
     * The TOTAL number of existence of each chunk, not the number of replicas to which you add a primary.
     */
    private final static short replication = 3;
    private final ConcurrentHashMap<String, FileMetadata> fileMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileMetadata> missingReplicas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ServerStatus> dataServers = new ConcurrentHashMap<>();
    private long totalFreeSpace = 0;
    /**
     * The next file ID to be used. File Id are never reused, this counter only increases
     */
    private long currentFileIndex = 1;
    
    private static final String STATE_FILE = "nameserver_state.dat";

    public NameServer() {
        loadState();
        
        // Register shutdown hook to save state on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeAllDataServerConnections();
            saveState();
        }));
    }
    
    private void closeAllDataServerConnections() {
        for (ServerStatus server : dataServers.values()) {
            server.closeConnection();
        }
    }
    //Class used to store the state of the name server, when shutted down (so if everything is restated the same way, it is in the same state).
    private static class NameServerState implements Serializable {
        //@Serial
        //private static final long serialVersionUID = 1L;
        Map<String, FileMetadata> fileMap;
        Map<String, FileMetadata> missingReplicas;
        String uuid;
        long currentFileIndex;
        
        NameServerState(Map<String, FileMetadata> fileMap, Map<String, FileMetadata> missingReplicas, 
                       String uuid, long currentFileIndex) {
            this.fileMap = fileMap;
            this.missingReplicas = missingReplicas;
            this.uuid = uuid;
            this.currentFileIndex = currentFileIndex;
        }
    }
    
    private void saveState() {
        try {
            NameServerState state = new NameServerState(fileMap, missingReplicas, uuid, currentFileIndex);
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATE_FILE))) {
                oos.writeObject(state);
                System.out.println("State saved to " + STATE_FILE);
            }
        } catch (IOException e) {
            System.err.println("Failed to save state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadState() {
        Path statePath = Path.of(STATE_FILE);
        if (!Files.exists(statePath)) {
            // Generate UUID if no state file exists
            this.uuid = UUID.randomUUID().toString();
            System.out.println("No state file found. Starting fresh with UUID: " + uuid);
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STATE_FILE))) {
            NameServerState state = (NameServerState) ois.readObject();
            this.uuid = state.uuid != null ? state.uuid : UUID.randomUUID().toString();
            this.currentFileIndex = state.currentFileIndex;
            
            if (state.fileMap != null) {
                fileMap.putAll(state.fileMap);
            }
            if (state.missingReplicas != null) {
                missingReplicas.putAll(state.missingReplicas);
            }
            
            System.out.println("State loaded from " + STATE_FILE);
            System.out.println("  UUID: " + uuid);
            System.out.println("  Files: " + fileMap.size());
            System.out.println("  Missing replicas: " + missingReplicas.size());
            System.out.println("  Current file index: " + currentFileIndex);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load state: " + e.getMessage());
            System.err.println("Starting fresh...");
            this.uuid = UUID.randomUUID().toString();
        }
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
     * Write in the FileMetadata the location where the chunks have been stored
     * @param file The fileMetadata objects of the file
     * @param chunk The chunk of data to write.
     */
    private void writeChunk(FileMetadata file, Chunk chunk) throws Exception {
        List<ServerStatus> availableServers = new ArrayList<>(dataServers.values());
        if (availableServers.isEmpty()) {
            throw new Exception("No data servers available");
        }
        file.setChunkHash(chunk);
        
        int nbCopies = 0;
        int nbEssaye = 0;
        ServerStatus[] serverArray = availableServers.toArray(new ServerStatus[0]);
        while (nbEssaye < availableServers.size() && nbCopies < replication) {
            ServerStatus tryServer = Quickselect.quickselect(serverArray, nbEssaye);
            nbEssaye++;
            try {
                //TODO : paralléliser cet envoi
                sendChunkToDataServer(tryServer, chunk);
                nbCopies++;
                file.addServerToChunk(chunk.chunkID, tryServer.uuid);
            } catch (RuntimeException e) {
                e.printStackTrace();
                //Ce serveur n'était manifestement pas disponible, essayons en un autre.
            }
        }
        if (nbCopies == 0) {
            throw new Exception("Could not write the file");
        }
    }

    public void start(int port) throws IOException {
        // Start periodic save timer (every 3 minutes)
        Timer saveTimer = new Timer(true); // daemon thread
        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveState();
            }
        }, 180000, 180000); // 3 minutes = 180000 milliseconds
        
        System.out.println("NameServer started with UUID: " + uuid);
        System.out.println("Auto-save enabled (every 3 minutes)");
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } finally {
            // Save state when server stops
            saveTimer.cancel();
            // Close all persistent connections to DataServers
            closeAllDataServerConnections();
            saveState();
        }
    }

    private void handleClient(Socket client) {
        boolean isADataServer = false;
        ObjectOutputStream oos =null;
        ObjectInputStream ois = null;
        try {
            oos = new ObjectOutputStream(client.getOutputStream());
            ois = new ObjectInputStream(client.getInputStream());

            Object obj;
            while (true) {
                try {
                    obj = ois.readObject();
                } catch (EOFException e) {
                    // Client closed connection normally
                    System.out.println("Connection closed normally");
                    break;
                }

                if (obj instanceof CreateFileMessage msg) {
                    try {
                        newFile(msg.fileName);
                        oos.writeObject(new OkMessage());
                        oos.flush();
                    } catch (FileExists e) {
                        oos.writeObject(new ErrorMessage("File exists", msg.fileName));
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
                        oos.writeObject(new ErrorMessage("Unknown file", chunk.fileName, chunk.chunkID));
                        oos.flush();
                        continue;
                    }
                    try {
                        writeChunk(metadata, chunk);
                        oos.writeObject(new OkMessage());
                        oos.flush();
                    } catch (Exception e) {
                        oos.writeObject(new ErrorMessage("Write failed: " + e.getMessage(), chunk.fileName, chunk.chunkID));
                        oos.flush();
                    }
                } else if (obj instanceof EndOfFile) {//For future use
                    oos.writeObject(new OkMessage());
                    oos.flush();
                    break;
                } else if (obj instanceof RegisterDataServerMessage msg) {
                    registerDataServer(msg, client.getRemoteSocketAddress().toString(), client, ois, oos);
                    updateLastSeen(msg.uuid);
                    isADataServer = true;
                    break;
                } else if (obj instanceof ReadFile readFile) {
                    try {
                        readFile(readFile.pathName, readFile.offset, oos, ois);
                    } catch (FileNotFound e) {
                        oos.writeObject(new ErrorMessage("The file you requested does not exist.", readFile.pathName));
                    } catch (IOException e) {
                        oos.writeObject(new ErrorMessage("An internal network IO error occured, sorry"));
                    }


                } else {
                    oos.writeObject(new ErrorMessage("Unknown message"));
                    oos.flush();
                }
            }
        } catch (ClassNotFoundException e){
            //This should not happen, but just in case.
            System.err.println("A unknown message has been received, coming from something else. (Maybe different version even if there is only one)");
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //If it is not a connection initialized by a dataserver, we close it.
            if (!isADataServer){
                try {
                    ois.close();
                } catch (IOException e) {

                }
                try {
                    oos.close();
                } catch (IOException e) {

                }
                try {
                    client.close();
                } catch (IOException e) {

                }
            }
        }
    }

    private void registerDataServer(RegisterDataServerMessage msg, String remoteAddress, Socket socket, ObjectInputStream ois, ObjectOutputStream oos) throws IOException {
        // Use IP from message (which is the DataServer's local IP) or fallback to remote address
        String ipAddress = msg.ipAddress != null && !msg.ipAddress.isEmpty() 
            ? msg.ipAddress 
            : extractIPFromAddress(remoteAddress);
        ServerStatus status = new ServerStatus(ipAddress, msg.port, msg.uuid, msg.freeSpace, msg.occupiedSpace);
        status.lastSeen = System.currentTimeMillis() / 1000; // Unix timestamp
        dataServers.put(msg.uuid, status);
        totalFreeSpace += msg.freeSpace;

        status.setOis(ois);
        status.setOos(oos);
        status.setSocket(socket);

        oos.writeObject(new OkMessage());
        oos.flush();
        System.out.println("Registered DataServer: " + msg.uuid + " at " + ipAddress + ":" + msg.port + 
            " (free: " + msg.freeSpace + ", occupied: " + msg.occupiedSpace + ")");
    }

    private void updateLastSeen(String uuid) {
        ServerStatus status = dataServers.get(uuid);
        if (status != null) {
            status.lastSeen = System.currentTimeMillis() / 1000; // Unix timestamp
        }
    }

    private String extractIPFromAddress(String address) {
        // Extract IP from format like "/127.0.0.1:12345"
        if (address.startsWith("/")) {
            address = address.substring(1);
        }
        int colonIndex = address.indexOf(':');
        if (colonIndex > 0) {
            return address.substring(0, colonIndex);
        }
        return address;
    }

    private void sendChunkToDataServer(ServerStatus server, Chunk chunk) {
        synchronized (server.getConnectionLock()) {
            ObjectOutputStream oos = server.getOos();
            ObjectInputStream ois = server.getOis();
            
            try {
                oos.writeObject(chunk);
                oos.flush();
                
                Object response = ois.readObject();
                if (response instanceof OkMessage) {
                    updateLastSeen(server.uuid);
                    // Update server's free space (approximate - we don't know exact chunk size)
                    server.freeSpace -= chunk.getData().length;
                    server.occupiedSpace += chunk.getData().length;
                } else if (response instanceof ErrorMessage error) {
                    throw new RuntimeException("DataServer " + server.uuid + " error: " + error.message);
                } else {
                    throw new RuntimeException("Unexpected response from DataServer " + server.uuid);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to send chunk to DataServer " + server.uuid + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     *
     * @param filePath
     * @param offset The offset, in byte, to read from (0 means from the beginning)
     */
    private void readFile(String filePath, long offset, ObjectOutputStream client_oos, ObjectInputStream client_ois) throws FileNotFound, IOException, ClassNotFoundException {
        FileMetadata fileMetadata = fileMap.get(filePath);
        if (fileMetadata == null) {
            throw new FileNotFound();
            //TODO : Notify the client of this.
        }
        final int startFromChunk = (int) (offset/ FileMetadata.ChunkSize);


        List<List<String>> chunksLocations = fileMetadata.getChunksLocations(); // fileMap: entrée: file paths, sortie: file metadat
        // number of chunks that have been read
        for (int chunkRead = 0; chunkRead + startFromChunk < chunksLocations.size(); chunkRead++) {

            // for each chunk > startFromChunk, communicate with DataServer of the
            List<String> chunkLocations = chunksLocations.get(startFromChunk); // locations of the replicas of the chunk

            int i = 0;
            for (; i < chunkLocations.size(); i++) {
                String dataServerId = chunkLocations.get(i);
                ServerStatus dataServer = dataServers.get(dataServerId);
                // Establish connection with server to request
                synchronized (dataServer.getConnectionLock()) {
                    ObjectOutputStream oos = dataServer.getOos();
                    ObjectInputStream ois = dataServer.getOis();
                    oos.writeObject(new ReadChunk(fileMetadata.id, startFromChunk + chunkRead));
                    oos.flush();

                    Object dataServerResponse = ois.readObject();
                    if (dataServerResponse instanceof Chunk chunk) {
                        client_oos.writeObject(chunk);
                        client_oos.flush();
                        break;
                    }
                }
            }

            if (i == chunkLocations.size()) {
                throw new RuntimeException("Chunk not found on any server. This should not happen.");
            }
            client_oos.writeObject(new EndOfFile());
        }
    }


}

