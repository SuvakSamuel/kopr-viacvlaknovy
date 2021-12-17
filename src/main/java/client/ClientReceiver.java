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
    private File requestedFile;
    private BlockingQueue<File> everyFileToBeDownLoaded;

    public ClientReceiver(File requestedFile, File destinationFile, BlockingQueue<File> everyFileToBeDownloaded) {
        this.requestedFile = requestedFile;
        this.destinationFile = destinationFile;
        this.everyFileToBeDownLoaded = everyFileToBeDownloaded;
    }

    //progressbar sa updatuje tuna

    @Override
    public Void call() throws Exception {
        System.out.println("ClientReceiver " + Thread.currentThread().getId() + " created");
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
                    break;
                } catch (EOFException e) {
                    System.out.println("Seems to be the end of connection, closing client socket");
                    break;
                }
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Interruption detected, closing client socket");
                    socket.close();
                    return null;
                }
            }
        }
        System.out.println("Closing ClientReceiver " + Thread.currentThread().getId());
        socket.close();
        return null;
    }

    public void receiveFile(File file) throws IOException {
        // https://www.programiz.com/java-programming/examples/get-relative-path
        URI requestedPath = file.toURI();
        URI destinationPath = destinationFile.toURI();
        URI relativePathToSendToServer = destinationPath.relativize(requestedPath);

        DataOutputStream outputPW = new DataOutputStream(socket.getOutputStream());
        String request = requestedFile + "/" + relativePathToSendToServer;
        System.out.println("ClientReceiver " + Thread.currentThread().getId() + " is requesting server for " + request);
        outputPW.writeUTF(request);

        //prijmi data

        DataInputStream inputBR = new DataInputStream(socket.getInputStream());
        Long serverSentFileSize = inputBR.readLong();

        Long downloadedSize = 0L;

        file.getParentFile().mkdirs();
        file.createNewFile();

        FileOutputStream fileOut = new FileOutputStream(file.getAbsolutePath());
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
        bufferOut.close();
    }
}
