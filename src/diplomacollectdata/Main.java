/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package diplomacollectdata;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.Timer;

/**
 *
 * @author egg
 */
public class Main {
    private static Timer oddsTimer;
    private static Timer matchTimer;
    private static ScriptEngineManager manager;
    private static ScriptEngine engine;
    
    public static void main(String args[]){
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
        manager = new ScriptEngineManager();
        engine = manager.getEngineByExtension("R");
        matchTimer = new Timer(matchesFreq, (ActionEvent e) -> {
            try {
                DownloadMatches.startDownload();
            } catch (SQLException | IOException | ParseException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        matchTimer.setInitialDelay(0);
        System.out.println("Első");
        oddsTimer = new Timer(oddsFreq, (ActionEvent e) -> {
            try {
                DownloadOdds odds = new DownloadOdds();
                System.out.println("odds");
                Classification.Clean(engine);
                System.out.println("cleaned");
                Classification.Classify(engine);
                System.out.println("classified");
            } catch (ScriptException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        oddsTimer.setInitialDelay(0);
        System.out.println("Második");
        
        matchTimer.start();
        oddsTimer.start();
        System.out.println("Harmadik");
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
