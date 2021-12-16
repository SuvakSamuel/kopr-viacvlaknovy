package server;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Server {

    private ServerSocket metadataSocket;
    private ServerSocket fileSocket;
    private File requestedDir;
    private BlockingQueue<File> everyFileFound;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.metadataSocket = new ServerSocket(9501);
        server.fileSocket = new ServerSocket(9503);
        System.out.println("Server is now running...");
        System.out.println("Waiting for a request");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // fixedthread na 2, jeden ma filerequest a druhy ma serversendery (oba maju rozlicne porty)
        while(true) {
            executor.execute(() -> {
                try {
                    System.out.println("in metadata thread");
                    Socket metaS = server.metadataSocket.accept();
                    server.exchangeFileData(metaS);
                } catch (SocketException e) {
                    System.out.println("Something's wrong with the metadata socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            executor.execute(() -> {
                try {
                    System.out.println("in filesend thread");
                    Socket fileS = server.fileSocket.accept();
                    server.sendFiles(fileS);
                } catch (SocketException e) {
                    System.out.println("Something's wrong with the file socket");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    //TODO wtf is this server

    public void exchangeFileData(Socket socket) throws IOException {
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

        searchRequestedDirectory();

        outputPW.println(everyFileFound.size());
        System.out.println("The server has found " + everyFileFound.size() + " files");
        System.out.println(everyFileFound);

        while (!everyFileFound.isEmpty()) {
            File toSend = everyFileFound.poll();
            outputPW.println(toSend.toString());
            outputPW.println(toSend.length());
        }
    }

    public void sendFiles(Socket socket) throws Exception {
        System.out.println("---------sending---------");
        Callable send = new ServerSender(socket);
        send.call();
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
