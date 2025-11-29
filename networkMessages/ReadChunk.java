package networkMessages;

import java.io.Serializable;

public class ReadChunk implements Serializable {
    public final long fileID;
    public final int chunkID;

    public ReadChunk(long fileID, int chunkID) {
        this.fileID = fileID;
        this.chunkID = chunkID;
    }
}
