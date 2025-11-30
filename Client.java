import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import networkMessages.EndOfFile;
import networkMessages.CreateFileMessage;
import networkMessages.ErrorMessage;
import networkMessages.GetMetadataMessage;
import networkMessages.MetadataResponse;
import networkMessages.OkMessage;
import networkMessages.ReadFile;

public class Client {
    private final String nameServerIPAddress;
    private final int nameServerPort;

    // Client Class constructor
    public Client(String nameServerIPAddress, int nameServerPort) {
        this.nameServerIPAddress = nameServerIPAddress;
        this.nameServerPort = nameServerPort;
    }

    private void checkForError(Object response, String operation) throws IOException {
        /*
        This function processes the potential error message sent by the server. If message error, than prints a
        readable message indicating the file name or chunk ID.
        */
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

    // Upload files from Client to Distributed File System
    public void putFile(String path) throws Exception {
        // Initialization
        String fileName = Path.of(path).getFileName().toString();
        int chunkSize = (int) FileMetadata.ChunkSize;
        int chunkIndex = 0;
        //On cree les flux réseau oos et ois, et un flux pour le fichier a envoyer in
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

            // Get file metadata
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
            if (!meta.exists) {
                throw new IOException("File metadata not found after creation");
            }
            long fileId = meta.fileId;

            // Send file chunks to NameServer
            byte[] buffer = new byte[chunkSize]; //Creation d'un buffer de la taille d'un chunk
            int read; //the number of bytes read for this iteration
            while ((read = in.read(buffer)) != -1) {//while we read something
                byte[] data;
                if (read == buffer.length) {
                    //If the buffer is full, just send it as is
                    data = buffer;
                } else {
                    //Otherwise remove the trailing space to avoid sending these random datas (that would be handled as part of the file by the server)
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
            //Send a closefile message that could be useful with a lock system but is currently unused.
            oos.writeObject(new EndOfFile(fileName));
            oos.flush();
            Object closeAck = ois.readObject();
            checkForError(closeAck, "CloseFile");
            if (!(closeAck instanceof OkMessage)) {
                throw new IOException("CloseFile not acknowledged by NameServer");
            }
        }
    }


    public void readFile(String pathName, long offset) throws IOException {
        try (Socket socket = new Socket(nameServerIPAddress, nameServerPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             FileOutputStream outFile = new FileOutputStream(pathName)) {

            //Query the nameserver
            oos.writeObject(new ReadFile(pathName, offset));
            oos.flush();

            while (true){
                Object response = null;
                try { //To handle the class not found exception.
                    response = ois.readObject();
                    System.out.println();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                checkForError(response, "Read File");
                if (response instanceof EndOfFile){
                    //No need to close the streams, they will be closed when exiting the try.
                    break;
                }

                assert response instanceof Chunk;
                outFile.write(((Chunk) response).getData()); //The cast has been automatically added by the IDE depiste of the assertion, I trust it.
            }
        }

    }
}


