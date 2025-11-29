package networkMessages;

public class ReadChunk {
    public final long fileID;
    public final int chunkID;

    public ReadChunk(long fileID, int chunkID) {
        this.fileID = fileID;
        this.chunkID = chunkID;
    }
}
