package server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class FileSearcher extends RecursiveTask<FileInformation>{

    File requestedDirectory;
    FileInformation information;

    public FileSearcher(File dir) {
        this.requestedDirectory = dir;
        information = new FileInformation(requestedDirectory.getPath());
    }

    @Override
    protected FileInformation compute() {
        analyzeDirectory(requestedDirectory);
        return information;
    }

    public void analyzeDirectory(File dir) {
        File[] files = dir.listFiles();
        List<FileSearcher> tasks = new ArrayList<>();
        for (File file : files) {
            if (file.isFile())
                information.addFile(file);
            System.out.println("The server found a file " + file);
            if (file.isDirectory()) {
                System.out.println("The server found a directory " + file);
                FileSearcher subTask = new FileSearcher(file);
                subTask.fork();
                tasks.add(subTask);
            }
        }
        for (FileSearcher subTask: tasks) {
            FileInformation fileInformation = subTask.join();
            this.information.addDirectory(fileInformation);
        }
    }
}

