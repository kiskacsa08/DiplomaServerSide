package diplomacollectdata;


import java.io.IOException;
import org.rosuda.JRI.*;

/**
 *
 * @author Attila
 */
public class Classification {
    
    public static void Clean() {
        Rengine re=new Rengine (new String [] {"--vanilla"}, false, null);
        if (!re.waitForR())
        {
          System.out.println ("Cannot load R");
          return;
        }
        re.eval("source('datacleaner.r')");
        re.end();
    }
    
    public static void Classificate(){
        
    }
}
