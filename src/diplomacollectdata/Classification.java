package diplomacollectdata;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.rosuda.JRI.*;
import org.rosuda.REngine.*;
import org.rosuda.jrs.*;

/**
 *
 * @author Attila
 */
public class Classification {  
//    private static Rengine re;
    
//    public static void startR(){
//        re = new Rengine(new String[] {"--vanilla"}, false, null);
//    }
//    
//    public static void stopR(){
//        re.end();
//    }
    private static ClassLoader classLoader;
    
    public static void Clean(ScriptEngine engine) throws ScriptException, URISyntaxException, IOException {
//        Rengine re = new Rengine(new String[] {"--vanilla"}, false, null);
//        if (!re.waitForR())
//        {
//          System.out.println ("Cannot load R");
//          return;
//        }
//        re.eval("source('datacleaner.r')");
//        re.end();
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
        }
        else {
            path = resource.getPath();
        }
        
        System.out.println("Path: " + path);
        engine.eval("source('" + path + "')");
    }
    
    public static void Classify(ScriptEngine engine) throws ScriptException, URISyntaxException, IOException{
//        Rengine re2 = new Rengine(new String[] {"--vanilla"}, false, null);
//        if (!re.waitForR())
//        {
//          System.out.println ("Cannot load R");
//          return;
//        }
//        re.eval("source('classify.r')");
//        re.end();
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
        }
        else {
            path = resource.getPath();
        }
        engine.eval("source('" + path + "')");
//        System.out.println(engine.eval("source('test.r')"));
//        engine.eval("source('test.r')");
    }
}
