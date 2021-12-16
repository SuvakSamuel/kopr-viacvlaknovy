package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ServerSender implements Callable<Void> {

    private Socket serverSocket;

    public ServerSender(Socket socket) {
        this.serverSocket = socket;
    }

    @Override
    public Void call() throws Exception {
        while (true) {
            try {
                sendFile(serverSocket);
            } catch (NullPointerException e) {
                break;
            }
        }
        return null;
    }

    public void sendFile(Socket socket) throws IOException {
        BufferedReader inputBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String serverRequest = inputBR.readLine();
        System.out.println(serverRequest);
        Path fileToSendPath = Paths.get(serverRequest);
        File fileToSend = fileToSendPath.toFile();

        System.out.println("Server is now sending " + fileToSend.getAbsolutePath());

        /*DataOutputStream outputPW = new DataOutputStream(socket.getOutputStream());
        FileInputStream fileToSendFIS = new FileInputStream(fileToSend.getAbsolutePath());
        BufferedInputStream fileToSendBIS = new BufferedInputStream(fileToSendFIS);
        OutputStream output = socket.getOutputStream();
        byte[] buffer = new byte[2048];
        int bytesRead;
        while (fileToSendSize > 0 && (bytesRead = fileToSendBIS.read(buffer, 0, (int)Math.min(buffer.length, 4096))) != -1) {
            output.write(buffer, 0, bytesRead);
            fileToSendSize -= bytesRead;
        }
        output.flush();
        fileToSendBIS.close();*/
    }
}


