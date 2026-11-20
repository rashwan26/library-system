package services;

import util.Validation;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

/** Validated images are stored as PNG under a validated record identifier. */
public final class ImageStore {
    private ImageStore() {}
    public static BufferedImage read(Path source) throws IOException {
        if(Files.size(source)>10*1024*1024)throw new IOException("Choose an image smaller than 10 MB.");
        try(ImageInputStream stream=ImageIO.createImageInputStream(source.toFile())) {
            if(stream==null)throw new IOException("Cannot open image.");
            Iterator<ImageReader> readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new IOException("Unsupported image format.");
            ImageReader reader=readers.next();
            try {
                reader.setInput(stream);
                if((long)reader.getWidth(0)*reader.getHeight(0)>16_000_000L)throw new IOException("Image exceeds 16 million pixels.");
                BufferedImage image=reader.read(0);
                if(image==null)throw new IOException("Invalid image.");return image;
            } finally {reader.dispose();}
        }
    }
    public static void remove(Path directory,String id) throws IOException {
        Validation.identifier(id);
        for(String ext:new String[]{".jpg",".jpeg",".png",".gif",".bmp"})Files.deleteIfExists(directory.resolve(id+ext));
    }
    public static Path store(Path source,Path directory,String id) throws IOException {
        Validation.identifier(id);BufferedImage image=read(source);Files.createDirectories(directory);
        Path target=directory.resolve(id+".png"),temp=Files.createTempFile(directory,"image-",".tmp");
        try {
            if(!ImageIO.write(image,"png",temp.toFile()))throw new IOException("PNG encoding unavailable.");
            try {Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
            for(String ext:new String[]{".jpg",".jpeg",".gif",".bmp"})Files.deleteIfExists(directory.resolve(id+ext));
            return target;
        } finally {Files.deleteIfExists(temp);}
    }
}
