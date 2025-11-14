import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileMetadata implements Serializable {
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
    public List<byte[]> chunksHash = new ArrayList<>();
    private List<List<String>> chunksLocations = new ArrayList<>(); //TODO : Optimiser ça

    public FileMetadata(long id, String pathName) {
        this.id = id;
        this.pathName = pathName;
    }

    private void setChunkHash(int chunkID, byte[] hash){
        while (chunksHash.size() <= chunkID) {//Juste on ajoute des cases pour écrire quelque part.
            byte[] nothing = {};
            chunksHash.add(nothing);
        }
        chunksHash.set(chunkID, hash);
    }

    void setChunkHash(Chunk chunk){
        assert chunk.fileID == id;
        final MessageDigest hasheur;
        try {
            hasheur = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); //Mais ça ne devrait pas arriver, MD5 existe non ?
        }
        hasheur.update(chunk.getData());
        setChunkHash(chunk.chunkID, hasheur.digest());
    }

    private boolean validateChunk(Chunk chunk){
        assert chunk.fileID == id;
        final MessageDigest hasheur;
        try {
            hasheur = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); //Mais ça ne devrait pas arriver, MD5 existe non ?
        }
        hasheur.update(chunk.getData());
        return chunksHash.get(chunk.chunkID) == hasheur.digest();
    }

    public void addServerToChunk(int chunkID, String uuid) {
        while (chunksLocations.size() <= chunkID) {
            chunksLocations.add(new ArrayList<>());
        }
        chunksLocations.get(chunkID).add(uuid);
    }
}

