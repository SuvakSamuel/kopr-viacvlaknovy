package server;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Server {

    private ServerSocket metadataSocket;
    private ServerSocket fileSocket;
    private File requestedDir;
    private BlockingQueue<File> everyFileFound;
    private ExecutorService fileDownloadExecutor;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.metadataSocket = new ServerSocket(9501);
        server.fileSocket = new ServerSocket(9503);
        System.out.println("Server is now running...");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        server.fileDownloadExecutor = Executors.newCachedThreadPool();

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
                    System.out.println("New client file socket detected, creating server socket");
                    Callable<Void> send = new ServerSender(fileS);
                    server.fileDownloadExecutor.submit(send);
                    //server.sendFiles(fileS);
                } catch (SocketException e) {
                    System.out.println("Something's wrong with the file socket");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    //TODO server thread ma problem spracovat viac nez 2 prichadzajucich socketov naraz
    //TODO file names nesmu mat medzery v sebe lebo proste XDDDDDDD

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

    public void sendFiles(Socket socket) throws Throwable {
        System.out.println("-----*****-----creating new server socket-----*****-----");
        Callable<Void> send = new ServerSender(socket);
        fileDownloadExecutor.submit(send);


        /*Future<Void> future = fileDownloadExecutor.submit(send);
        try {
            future.get(2, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            System.out.println("Server socket duration timed out");
        } catch (ExecutionException e) {
            System.out.println("Exception in a server socket thread, aborting operation");
            throw e.getCause();
        } finally {
            future.cancel(true);
        }*/
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
