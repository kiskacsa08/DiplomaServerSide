package diplomacollectdata;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author egg
 */
public class Main {
    private static Timer oddsTimer;
    private static Timer matchTimer;
    
    public static void main(String args[]) {
        if (args.length != 2) {
            System.out.println("This can be run with two parameters:");
            System.out.println("1: Odds and classification download frequency");
            System.out.println("2: Matches download frequency");
            System.out.println("Download frequencies are in minutes and at least 30 and at most 1440 (one day)");
            return;
        }
        String oddsFreqString = args[0];
        String matchesFreqString = args[1];
        if (!isValidArgument(oddsFreqString) || !isValidArgument(matchesFreqString)) {
            System.out.println("This can be run with two parameters:");
            System.out.println("1: Odds and classification download frequency");
            System.out.println("2: Matches download frequency");
            System.out.println("Download frequencies are in minutes and at least 30 and at most 1440 (one day)");
            return;
        }
        int oddsFreq = Integer.parseInt(oddsFreqString)*60*1000;
        int matchesFreq = Integer.parseInt(matchesFreqString)*60*1000;
        System.out.println(oddsFreq);
        System.out.println(matchesFreq);
        matchTimer = new Timer();
        matchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("Matches");
                    DownloadMatches.startDownload();
                } catch (SQLException | IOException | ParseException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, new Date(), matchesFreq);
        oddsTimer = new Timer();
        oddsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("odds");
                    DownloadOdds odds = new DownloadOdds();
                    Classification.Clean();
                    System.out.println("cleaned");
                    Classification.Classify();
                    System.out.println("classified");
                    System.gc();
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, new Date(), oddsFreq);
    }
    
    private static boolean isValidArgument(String s){
        try {
            int arg = Integer.parseInt(s);
            if (arg < 30 || arg > 1440) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
