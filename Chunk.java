import java.io.Serializable;

public class Chunk implements Serializable {
    public final long fileID;
    public final String fileName;
    public final int chunkID;
    private final byte[] data;

    public Chunk(long fileID, String fileName, int chunkID, byte[] data) {
        this.fileID = fileID;
        this.fileName = fileName;
        this.chunkID = chunkID;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
