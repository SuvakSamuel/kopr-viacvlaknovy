package server;

import java.io.File;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class FileInformation {

    private String filePath;
    private Long fileSize;
    private int fileCount;
    private BlockingQueue<File> everyFile;

    public FileInformation(String filePath) {
        this.filePath = filePath;
        this.fileSize = 0L;
        this.fileCount = 0;
        this.everyFile = new LinkedBlockingDeque<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getSize() {
        return fileSize;
    }

    public int getFileCount() {
        return fileCount;
    }

    public BlockingQueue<File> getEveryFile() {
        return everyFile;
    }

    public void addFile(File file) {
        everyFile.offer(file);
        fileSize += file.length();
        fileCount++;
    }

    public void addDirectory(FileInformation fileInformation) {
        fileSize += fileInformation.getSize();
        fileCount += fileInformation.getFileCount();
        everyFile.addAll(fileInformation.getEveryFile());
    }
}
