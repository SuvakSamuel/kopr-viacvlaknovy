package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;

public class ClientReceiver implements Callable<Void> {

    private Socket socket;
    private String destinationFile;
    private int exceptionCounter;

    public ClientReceiver(String destinationFile) {
        this.destinationFile = destinationFile;
    }

    @Override
    public Void call() throws Exception {
        socket = new Socket("localhost", 9001);
        while(true) {
            try {
                receiveFile();
            } catch (EOFException e) {
                System.out.println("Closing client socket");
                socket.close();
                break;
            } catch (SocketException e) {
                System.out.println("Connection lost, closing client socket");
                socket.close();
                break;
            } catch (FileNotFoundException e) {
                System.out.println("Access denied, aborting attempt to download");
            }
        }
        return null;
    }

    public void receiveFile() throws IOException {
        DataInputStream inputBR = new DataInputStream(socket.getInputStream());

        String serverSending = inputBR.readUTF();
        Long serverSentFileSize = inputBR.readLong();
        System.out.println("Client socket received from the server " + serverSending + ", the file size is " + serverSentFileSize);

        File fileToCreate = new File(destinationFile + serverSending);
        Long downloadedSize = 0L;

        if(fileToCreate.exists()) {
            System.out.println("-- Client says it already has the file, checking size");
            Long fileAlreadyExistsSize = fileToCreate.length();
            System.out.println("-- The file has the size " + fileAlreadyExistsSize);
            if(!fileAlreadyExistsSize.equals(serverSentFileSize)) {
                System.out.println("-- File sizes are not the same, downloading again");
            } else {
                System.out.println("-- The file sizes are the same, skipping operation");
                inputBR.skip(fileAlreadyExistsSize);
                return;
            }
        } else {
            fileToCreate.getParentFile().mkdirs();
            fileToCreate.createNewFile();
        }

        FileOutputStream fileOut = new FileOutputStream(fileToCreate.getAbsolutePath());
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
