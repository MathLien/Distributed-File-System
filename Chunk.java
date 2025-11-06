public class Chunk {
    public final long fileID;
    public final int chunkID;
    private final byte[] data;

    public Chunk(long fileID, int chunkID, byte[] data) {
        this.fileID = fileID;
        this.chunkID = chunkID;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
