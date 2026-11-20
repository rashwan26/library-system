package fileio;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;

/** Prevent two desktop instances from editing the same data directory. */
public final class ApplicationLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private ApplicationLock(FileChannel channel,FileLock lock){this.channel=channel;this.lock=lock;}
    public static ApplicationLock acquire(Path directory) throws IOException {
        Files.createDirectories(directory);
        FileChannel channel=FileChannel.open(directory.resolve(".app.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        try {
            FileLock lock=channel.tryLock();
            if(lock==null)throw new IOException("This library is already open in another application instance.");
            return new ApplicationLock(channel,lock);
        } catch(IOException | RuntimeException e) {
            channel.close();throw new IOException("This library is already open, or its lock cannot be acquired.",e);
        }
    }
    @Override public void close() throws IOException {try{lock.release();}finally{channel.close();}}
}
