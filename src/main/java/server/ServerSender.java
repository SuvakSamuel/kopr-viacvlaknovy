package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ServerSender implements Callable<Void> {

    private ServerSocket serverSocket;
    private Socket socket;
    private BlockingQueue<File> everyFile;
    private File polledFileToSend;
    private File requestedDir;

    public ServerSender(ServerSocket socket, BlockingQueue everyFile, File requestedDir) {
        this.serverSocket = socket;
        this.everyFile = everyFile;
        this.requestedDir = requestedDir;
    }

    @Override
    public Void call() throws Exception {
        socket = serverSocket.accept();
        while (!everyFile.isEmpty()) {
            polledFileToSend = everyFile.poll();
            if (polledFileToSend != null) {
                try {
                    sendFile(socket, polledFileToSend);
                } catch (SocketException e) {
                    System.out.println("Connection issue, closing server socket");
                    socket.close();
                    return null;
                }
            }
        }
        socket.close();
        return null;
    }

    public void sendFile(Socket socket, File polledFileToSend) throws IOException {
        String fileToSendParentless = polledFileToSend.getAbsolutePath().substring(requestedDir.getAbsolutePath().length()+1);
        System.out.println("Server socket is sending " + fileToSendParentless + " to client");

        DataOutputStream outputPW = new DataOutputStream(socket.getOutputStream());
        outputPW.writeUTF(fileToSendParentless);

        Long fileToSendSize = polledFileToSend.length();
        outputPW.writeLong(fileToSendSize);

        FileInputStream fileToSendFIS = new FileInputStream(polledFileToSend.getAbsolutePath());
        BufferedInputStream fileToSendBIS = new BufferedInputStream(fileToSendFIS);
        OutputStream output = socket.getOutputStream();
        byte[] buffer = new byte[2048];
        int bytesRead;
        while (fileToSendSize > 0 && (bytesRead = fileToSendBIS.read(buffer, 0, (int)Math.min(buffer.length, 4096))) != -1) {
            output.write(buffer, 0, bytesRead);
            fileToSendSize -= bytesRead;
        }
        output.flush();
        fileToSendBIS.close();
    }
}


