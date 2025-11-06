
import java.util.concurrent.ConcurrentHashMap;

import exceptions.FileExists;

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
    private long currentFileIndex = 0;

    public NameServer() {
        //TODO : Read from file or generate
        this.uuid = "";
    }

    private long newFile(String pathName) throws FileExists {
        if (fileMap.putIfAbsent(pathName, File(currentFileIndex, pathName)) != null) {
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




}

