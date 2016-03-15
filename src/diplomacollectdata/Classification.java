package diplomacollectdata;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 *
 * @author Attila
 */
public class Classification {
    
    public static void Clean() throws IOException, InterruptedException {
        String res = "/resources/datacleaner.r";
        File file = null;
        String path;
        URL resource = Classification.class.getResource(res);

        if (resource.toString().startsWith("jar:")) {
            InputStream input = Classification.class.getResourceAsStream(res);
            file = File.createTempFile("datacleaner", ".r");
            OutputStream out = new FileOutputStream(file);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            file.deleteOnExit();
            path = file.getAbsolutePath();
            input = null;
            out = null;
        }
        else {
            path = resource.getPath();
        }
        System.out.println("Path: " + path);
        
        ProcessBuilder pb = new ProcessBuilder("Rscript", path);
        pb.inheritIO();
        
        Process p = pb.start();
        p.waitFor();
        p.destroy();
        System.gc();
    }
    
    public static void Classify() throws IOException, InterruptedException {
        String res = "/resources/classify.r";
        File file = null;
        String path;
        URL resource = Classification.class.getResource(res);
        if (resource.toString().startsWith("jar:")) {
            InputStream input = Classification.class.getResourceAsStream(res);
            file = File.createTempFile("classify", ".r");
            OutputStream out = new FileOutputStream(file);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            file.deleteOnExit();
            path = file.getAbsolutePath();
            input = null;
            out = null;
        }
        else {
            path = resource.getPath();
        }
        System.out.println("Path: " + path);
        ProcessBuilder pb = new ProcessBuilder("Rscript", path);
        pb.inheritIO();
        
        Process p = pb.start();
        p.waitFor();
        p.destroy();
        System.gc();
    }
}
