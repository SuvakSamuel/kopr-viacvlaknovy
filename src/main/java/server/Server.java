package server;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Server {

    private ServerSocket serverSocket;
    private File requestedDir;
    private int socketCount;
    private BlockingQueue<File> everyFileFound;
    private BlockingQueue<File> everyFileToSend;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.serverSocket = new ServerSocket(9001);
        System.out.println("Server is now running...");
        System.out.println("Waiting for a request");
        Socket socket = server.serverSocket.accept();

        while(true) {
            try {
                server.listenForFileRequests(socket);
            } catch (SocketException e) {
                System.out.println("Closing server socket");
                System.out.println("Restart server to copy another directory");
                socket.close();
                break;
            }
        }
    }

    public void listenForFileRequests(Socket socket) throws IOException {
        PrintWriter outputPW = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader inputBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String request = inputBR.readLine();
        if(request == null) {
            System.out.println("The server received a null request, aborting");
            socket.close();
            return;
        }
        System.out.println("The server received a request for directory " + request);
        Path requestFilePath = Paths.get(request);
        requestedDir = requestFilePath.toFile();

        String destination = inputBR.readLine();
        System.out.println("The server has acknowledged the target destination of " + destination);

        String sockets = inputBR.readLine();
        System.out.println("The server has acknowledged that the number of sockets should be " + sockets);
        socketCount = Integer.parseInt(sockets);

        searchRequestedDirectory();

        outputPW.println(everyFileFound.size());
        System.out.println("The server has found " + everyFileFound.size() + " files");


        System.out.println(everyFileFound);
        while (!everyFileFound.isEmpty()) {
            outputPW.println(everyFileFound.poll().toString());
        }
    }

    public void send() {
        System.out.println("------------sending-----------");
        ExecutorService executor = Executors.newFixedThreadPool(socketCount);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < socketCount; i++) {
            try {
                futures.add(completionService.submit(new ServerSender(serverSocket, everyFileToSend, requestedDir)));
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
                System.out.println("Server - Something's not quite right");
                e.printStackTrace();
                executor.shutdownNow();
                break;
            }
        }
        executor.shutdown();
        System.out.println("-----------Job's done!----------");
    }

    public void searchRequestedDirectory() {
        System.out.println("---------------searching--------------");
        everyFileFound = new LinkedBlockingQueue<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        List<FileSearcher> tasks = new ArrayList<>();
        File[] files = requestedDir.listFiles();
        for(int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                System.out.println("server found a file " + files[i]);
                everyFileFound.add(files[i]);
            }
            if (files[i].isDirectory()) {
                System.out.println("server found a directory " + files[i]);
                FileSearcher task = new FileSearcher(files[i]);
                forkJoinPool.submit(task);
                tasks.add(task);
            }
        }
        for (FileSearcher task : tasks) {
            FileInformation fileInformation = task.join();
            everyFileFound.addAll(fileInformation.getEveryFile());
        }
        forkJoinPool.shutdown();
    }
}
