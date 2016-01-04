package diplomacollectdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author egg
 */
public class DownloadOdds {
    private DatabaseConnection dc;
    
    //konstruktorban kapcsolódás az adatbázishoz, és a letöltés elindítása
    public DownloadOdds(){
        dc = new DatabaseConnection();
        System.out.println("database");
        downloadData();
        System.out.println("download");
    }

    //az egy héten belül következő meccsek kiválasztása az adatbázisból
    private ArrayList<Match> matchSelector(){
        Match m;
        ArrayList<Match> matchList = new ArrayList<>();
        Timestamp ts = new Timestamp(new Date().getTime());
        ResultSet rs;
        try {
            Timestamp act;
            rs = dc.openConnection("MATCHES");
            while (rs.next()) {
                act = rs.getTimestamp("MATCH_DATE");
                if (ts.getTime()+604800000 > act.getTime() && ts.getTime() < act.getTime()) {
                    m = new Match(rs.getInt("HOME_TEAM_ID"), rs.getInt("AWAY_TEAM_ID"), rs.getInt("ID"), rs.getInt("ROUND"));
                    matchList.add(m);
                }
            }
            rs.close();
            dc.closeConnection();
        } catch (SQLException ex) {
            Logger.getLogger(DownloadOdds.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return matchList;
    }
    
    //oddsok letöltése, és kiírása az adatbázisba
    private void downloadData(){
        StringBuilder rawData = new StringBuilder();
        ArrayList<Match> matchList = matchSelector();
        URL url;
        for (Match match : matchList) {
            try {
                url = OddsURLBuilder(match.getHomeTeamURLString(), match.getAwayTeamURLString(), String.valueOf(match.getRound()));
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                rawData.delete(0, rawData.length());
                while ((line=br.readLine()) != null) {                
                    rawData.append(line);
                }
                System.out.println(match.getHomeTeamURLString() + " - " + match.getAwayTeamURLString());
                double[] odds = parseHTML(rawData.toString());
                boolean allMissing = true;
                for (int i = 0; i < odds.length; i++) {
                    if (odds[i] != -1) {
                        allMissing = false;
                        break;
                    }
                }
                if(!allMissing){
                    writeToDatabase(odds, match.getMatchId());
                }
            } catch (IOException ex) {
                Logger.getLogger(DownloadOdds.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //html-ből az oddsok kinyerése, konvertálása decimális alakra és double tömbben visszaadása
    private double[] parseHTML(String HTML){
        double[] odds = new double[66];
        Elements results = new Elements();
        Document doc;
        doc = Jsoup.parse(HTML);
        results.clear();
        results = doc.select("td[onmouseover]");
        String odd;
        if (results.size() == 66) {
            for (int i = 0; i < results.size(); i++) {
                odd = results.get(i).html();
                odds[i] = convertToDecimal(odd);
            }
        }
        else {
            int defaultNumber = odds.length/3;
            int smallerNumber = results.size()/3;
            int j = 0;
            int i;

            for (i = 0; i < defaultNumber; i++) {
                if (i < smallerNumber) {
                    odd = results.get(i).html();
                    odds[i] = convertToDecimal(odd);
                    j++;
                } else {
                    odds[i] = -1;
                }
            }

            for (;i < defaultNumber*2; i++) {
                if (i<defaultNumber+smallerNumber) {
                    odd = results.get(j).html();
                    odds[i] = convertToDecimal(odd);
                    j++;
                } else {
                    odds[i] = -1;
                }
            }

            for (;i < defaultNumber*3; i++) {
                if (i<defaultNumber*2+smallerNumber) {
                    odd = results.get(j).html();
                    odds[i] = convertToDecimal(odd);
                    j++;
                } else {
                    odds[i] = -1;
                }
            }
        }
        return odds;
    }
    
    //odds tört formátum konvertálása decimállisra
    private double convertToDecimal(String fraction) {
        if (fraction.contains("/")) {
            String[] fract = fraction.split("/");
            return Double.parseDouble(fract[0]) / Double.parseDouble(fract[1]) + 1;
        } else if ("".equals(fraction)) {
            return -1;
        } else {
            return Double.parseDouble(fraction) + 1;
        }
    }
    
    //adatok kiírása az adatbázisba
    private void writeToDatabase(double[] odds, int matchID){
        try {
            ResultSet rs = dc.openConnection("ODDS");
            boolean exists = false;
            int row = -1;
            
            while(rs.next()){
                if (rs.getInt("MATCH_ID") == matchID) {
                    exists = true;
                    System.out.println(matchID + " exists.");
                    row = rs.getRow();
                }
            }

            if (exists) {
                boolean changed = false;
                rs.absolute(row);
                
                for (int i = 2; i < odds.length+2; i++) {
                    if (rs.getDouble(i) != odds[i-2]) {
                        changed = true;
                        System.out.println(i + " changed.");
                        //break;
                    }
                }
                if (changed) {
                    rs.moveToInsertRow();
                    rs.updateInt("MATCH_ID", matchID);
                    for (int i = 2; i < odds.length+2; i++) {
                        rs.updateDouble(i, odds[i-2]);
                    }
                    rs.updateTimestamp("REFRESH_DATE", new Timestamp(new Date().getTime()));
                    rs.insertRow();
                }
            } else {
                rs.moveToInsertRow();
                rs.updateInt("MATCH_ID", matchID);
                for (int i = 2; i < odds.length+2; i++) {
                    rs.updateDouble(i, odds[i-2]);
                }
                rs.updateTimestamp("REFRESH_DATE", new Timestamp(new Date().getTime()));
                rs.insertRow();
            }
            rs.close();
            dc.closeConnection();
        } catch (SQLException ex) {
            Logger.getLogger(DownloadOdds.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //URL felépítése egy adott meccshez
    private URL OddsURLBuilder(String homeTeam, String awayTeam, String round) throws MalformedURLException{
        StringBuilder oddsURL = new StringBuilder();
        oddsURL.append("http://odds.football-data.co.uk/football/england/premier-league/round-");
        oddsURL.append(round);
        oddsURL.append("/");
        oddsURL.append(homeTeam);
        oddsURL.append("-v-");
        oddsURL.append(awayTeam);
        oddsURL.append("/match-result/all-odds/defaultSort");
        return new URL(oddsURL.toString());
    }
}