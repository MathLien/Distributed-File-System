import java.util.ArrayList;
import java.util.List;

public class FileMetadata {
    /* Taille maximale d'un chunk (en octet)
     */
    public static final long ChunkSize = 1024 * 1024 * 4; //4MiB
    private String pathName;
    public final long id;
    private long size;
    private String owner = null;
    /*
     * The MD5 hash of the chunks
     */
    public byte[] chunksHash = {};
    private List<List<String>> chunksLocations = new ArrayList<>(); //TODO : Optimiser ça

    public FileMetadata(long id, String pathName) {
        this.id = id;
        this.pathName = pathName;
    }

    public void addServerToChunk(int chunkID, String uuid) {
        while (chunksLocations.size() <= chunkID) {
            chunksLocations.add(new ArrayList<>());
        }
        chunksLocations.get(chunkID).add(uuid);
    }
}

