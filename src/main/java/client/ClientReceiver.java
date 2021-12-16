package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ClientReceiver implements Callable<Void> {

    private Socket socket;
    private File destinationFile;
    private BlockingQueue<File> everyFileToBeDownLoaded;

    public ClientReceiver(File destinationFile, BlockingQueue<File> everyFileToBeDownloaded) {
        this.destinationFile = destinationFile;
        this.everyFileToBeDownLoaded = everyFileToBeDownloaded;
    }

    //progressbar sa updatuje tuna

    @Override
    public Void call() throws Exception {
        socket = new Socket("localhost", 9503);
        while(true) {
            File poll = everyFileToBeDownLoaded.poll();
            if(poll == null) {
                break;
            } else {
                try {
                    receiveFile(poll);
                } catch (SocketException e) {
                    System.out.println("Connection lost, closing client socket");
                    socket.close();
                }
            }
        }
        socket.close();
        return null;
    }

    public void receiveFile(File file) throws IOException {
        // https://www.programiz.com/java-programming/examples/get-relative-path
        System.out.println("ClientReceiver is looking to create " + file);
        URI requestedPath = file.toURI();
        URI destinationPath = destinationFile.toURI();
        URI relativePathToSendToServer = destinationPath.relativize(requestedPath);
        System.out.println("ClientReceiver is requesting server for " + relativePathToSendToServer);

        DataOutputStream outputPW = new DataOutputStream(socket.getOutputStream());
        outputPW.writeUTF(relativePathToSendToServer.toString());

        //prijmi data

        /*DataInputStream inputBR = new DataInputStream(socket.getInputStream());

        String serverSending = inputBR.readUTF();
        Long serverSentFileSize = inputBR.readLong();
        System.out.println("Client socket received from the server " + serverSending + ", the file size is " + serverSentFileSize);

        File fileToCreate = new File(destinationFile + serverSending);
        Long downloadedSize = 0L;*/

/*      fileToCreate.getParentFile().mkdirs();
        fileToCreate.createNewFile();*/

        /*FileOutputStream fileOut = new FileOutputStream(fileToCreate.getAbsolutePath());
        BufferedOutputStream bufferOut = new BufferedOutputStream(fileOut);
        byte[] byteArr;
        int offset = 2048;
        while (downloadedSize.compareTo(serverSentFileSize) != 0) {
            if (serverSentFileSize - downloadedSize >= offset) {
                downloadedSize += offset;
                byteArr = new byte[offset];
            } else {
                offset = (int) (serverSentFileSize - downloadedSize);
                byteArr = new byte[offset];
                downloadedSize = serverSentFileSize;
            }
            inputBR.read(byteArr, 0, offset);
            bufferOut.write(byteArr, 0, offset);
        }
        bufferOut.flush();
        bufferOut.close();*/
    }
}
