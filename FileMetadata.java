import java.io.File;
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
    private List<List<String>> chunksLocations = {};

    public FileMetadata(long id, String pathName) {
        this.id = id;
        this.pathName = pathName;
    }

    public void addServerToChunk(int chunkID, String uuid) {
        chunksLocations.get(chunkID).add(uuid);
    }
}

public class Chunk {
    public final long fileID;
    public final int chunkID;
    private final byte[] data;

    public Chunk(long fileID, int chunkID, byte[] data) {
        this.fileID = fileID;
        this.chunkID = chunkID;
        this.data = data;
    }
    public Chunk(File chunkFile){
        throw new Exception("Not implemented");
    }
}
