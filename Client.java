import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import networkMessages.CloseFileMessage;
import networkMessages.CreateFileMessage;
import networkMessages.ErrorMessage;
import networkMessages.GetMetadataMessage;
import networkMessages.MetadataResponse;
import networkMessages.OkMessage;

public class Client {
    private final String nameServerIPAddress;
    private final int nameServerPort;

    public Client(String nameServerIPAddress, int nameServerPort) {
        this.nameServerIPAddress = nameServerIPAddress;
        this.nameServerPort = nameServerPort;
    }
    /**
    Cette fonction sert a traiter l'eventuel message d'erreur envoyé par le serveur
    si c'en est un, ca cree juste un message a arficher qyi concatene le retour serveur et "l'operation"
    */
    private void checkForError(Object response, String operation) throws IOException {
        if (response instanceof ErrorMessage error) {
            StringBuilder errorMsg = new StringBuilder(operation);
            errorMsg.append(" failed: ").append(error.message);
            if (error.fileName != null) {
                errorMsg.append(" (fileName: ").append(error.fileName).append(")");
            }
            if (error.chunkID != null) {
                errorMsg.append(" (chunkID: ").append(error.chunkID).append(")");
            }
            throw new IOException(errorMsg.toString());
        }
    }

    public void putFile(String path) throws Exception {
        String fileName = Path.of(path).getFileName().toString();
        int chunkSize = (int) FileMetadata.ChunkSize;
        int chunkIndex = 0;
        //On cree les flux reseau oos et ois, et un flux pour le fichier a envoyer in
        try (Socket socket = new Socket(nameServerIPAddress, nameServerPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             InputStream in = Files.newInputStream(Path.of(path))) {

            //On cree un fichier vide sur le serveur
            oos.writeObject(new CreateFileMessage(fileName));
            oos.flush();
            Object ack = ois.readObject();
            checkForError(ack, "CreateFile");
            if (!(ack instanceof OkMessage)) {
                throw new IOException("CreateFile not acknowledged by NameServer");
            }

            //We ask the file ID (that could have been returned automatically previously but that's not the case)
            //Just to include it in the chunks we send
            oos.writeObject(new GetMetadataMessage(fileName));
            oos.flush();
            Object metaObj = ois.readObject();
            checkForError(metaObj, "GetMetadata");
            if (!(metaObj instanceof MetadataResponse)) {
                throw new IOException("GetMetadata did not return metadata");
            }
            MetadataResponse meta = (MetadataResponse) metaObj;
       
            long fileId = meta.fileId;

            //Creation d'un buffer de la taille d'un chunk
            byte[] buffer = new byte[chunkSize];
            int read; //the number of bytes read for this iteration
            while ((read = in.read(buffer)) != -1) {//while we read something
                byte[] data;
                if (read == buffer.length) {
                    //If the buffer is full, just send it as is
                    data = buffer;
                } else {
                    //Otherwise remove the trailing space to avoid sending these random datas (that woukd be handled as part of the file by the server)
                    data = new byte[read];
                    System.arraycopy(buffer, 0, data, 0, read);
                }
                //The chunk is sent and we wait for acknowledgement
                Chunk chunk = new Chunk(fileId, fileName, chunkIndex, data);
                oos.writeObject(chunk);
                oos.flush();
                Object chunkAck = ois.readObject();
                checkForError(chunkAck, "Chunk " + chunkIndex);
                if (!(chunkAck instanceof OkMessage)) {
                    throw new IOException("Chunk " + chunkIndex + " not acknowledged by NameServer");
                }
                chunkIndex++;
                //TODO : check if the following if is useful
                if (data == buffer) {
                    buffer = new byte[chunkSize];
                }
            }
            //Send a closefile message that coukd be useful with a lock system but is currently unused.
            oos.writeObject(new CloseFileMessage(fileName));
            oos.flush();
            Object closeAck = ois.readObject();
            checkForError(closeAck, "CloseFile");
            if (!(closeAck instanceof OkMessage)) {
                throw new IOException("CloseFile not acknowledged by NameServer");
            }
        }
    }
}


