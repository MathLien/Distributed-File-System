import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataServer {
    private final String uuid;
    private final String NameServerIPAddress = "127.0.0.1";
    private final String DATA_DIR = "/home/datas";

    private void writeChunk(Chunk chunk) throws IOException {
        final String fileName = String.valueOf(chunk.fileID)+'-'+ String.valueOf(chunk.chunkID);
        try{
            Files.write(Path.of(DATA_DIR, fileName), chunk.getData());
        } catch(IOException e){
            //TODO : gérer l'erreur et s'arrêter
        }
        acknoledgeToLeader(chunk.fileID, chunk.chunkID);

    }
}
