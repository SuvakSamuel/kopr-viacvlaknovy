package client;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ConnectionEstablisher {

    private File sourceFile;
    private File destinationFile;
    private int socketCount;
    private int numberOfFiles;
    private BlockingQueue<File> everyFileRequested = new LinkedBlockingQueue<>();
    private BlockingQueue<File> everyFileNotDownloaded = new LinkedBlockingQueue<>();

    public ConnectionEstablisher(String sourceFileLocation, String destinationFileLocation, int socketCount) {
        Path sourceFilePath = Paths.get(sourceFileLocation);
        this.sourceFile = sourceFilePath.toFile();
        Path destinationFilePath = Paths.get(destinationFileLocation);
        this.destinationFile = destinationFilePath.toFile();
        this.socketCount = socketCount;
    }

    public void exchangeNecessaryInformation(String requestedFilePath, String destinationFilePath, int socketCount) throws IOException {
        Socket socket = new Socket("localhost", 9001);

        PrintWriter outputPW = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader inputBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.println("Requesting " + requestedFilePath);
        outputPW.println(requestedFilePath);

        System.out.println("Destination should be " + destinationFilePath);
        outputPW.println(destinationFilePath);

        System.out.println("The number of sockets the client requested is " + socketCount);
        outputPW.println(socketCount);

        numberOfFiles = Integer.parseInt(inputBR.readLine());
        System.out.println("The server says that it found " + numberOfFiles + " files");


        for (int i = 0; i < numberOfFiles; i++){
            String toPath = inputBR.readLine();
            Path toFile = Paths.get(toPath);
            everyFileRequested.add(toFile.toFile());
        }

        while(!everyFileRequested.isEmpty()) {
            File polledFileToSend = everyFileRequested.poll();
            String fileToSendParentless = polledFileToSend.getAbsolutePath().substring(sourceFile.getAbsolutePath().length()+1);
        }

        socket.close();
        //connectSockets();
    }

    public void connectSockets() {
        System.out.println("-------------receiving------------");
        ExecutorService executor = Executors.newFixedThreadPool(socketCount);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < socketCount; i++) {
            try {
                //futures.add(completionService.submit(new ClientReceiver(destinationFile)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < socketCount; i++) {
            try {
                completionService.take().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }catch (ExecutionException e) {
                System.out.println("Client - Something's not quite right");
                e.printStackTrace();
                executor.shutdownNow();
                break;
            }
        }
        executor.shutdown();
        System.out.println("----------Job's done!-----------");
    }
}