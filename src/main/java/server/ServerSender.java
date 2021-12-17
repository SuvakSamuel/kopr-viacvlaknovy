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
        System.out.println("New server socket " + Thread.currentThread().getId() + " created");
        while (true) {
            try {
                sendFile(serverSocket);
            } catch (EOFException e) {
                System.out.println("End of file, closing server socket");
                break;
            } catch (NullPointerException e) {
                System.out.println("Null detected, closing server socket");
                break;
            } catch (FileNotFoundException e) {
                System.out.println("File not found for some reason, skipping operation");
                e.printStackTrace();
            }
        }
        System.out.println("Server socket " + Thread.currentThread().getId() + " finished");
        return null;
    }

    public void sendFile(Socket socket) throws IOException {
        DataInputStream inputBR = new DataInputStream(socket.getInputStream());
        String serverRequest = inputBR.readUTF();
        Path fileToSendPath = Paths.get(serverRequest).toAbsolutePath();
        File fileToSend = fileToSendPath.toFile();

        DataOutputStream outputPW = new DataOutputStream(socket.getOutputStream());
        System.out.println("Server socket " + Thread.currentThread().getId() + " is now sending " + fileToSend);

        Long fileToSendSize = fileToSend.length();
        outputPW.writeLong(fileToSendSize);

        FileInputStream fileToSendFIS = new FileInputStream(fileToSend);
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


