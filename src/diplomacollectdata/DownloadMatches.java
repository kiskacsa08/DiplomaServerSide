package diplomacollectdata;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author egg
 */
public final class DownloadMatches {
    private DatabaseConnection dc;
    
    //konstruktorban kapcsolódás az adatbázishoz, és a letöltés elindítása
    public DownloadMatches(){
        try {
            dc = new DatabaseConnection();
            dc.openConnection("MATCHES");
            //dc.deleteAllRows("MATCHES");
            dc.deleteSpecificRows("MATCHES", "ID > 379");
            this.MatchesConnect();
            dc.closeConnection();
        } catch (MalformedURLException | SQLException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //URL felépítése egy adott fordulóhoz
    private String MatchesURLBuilder(int round){
        StringBuilder matchesURL = new StringBuilder();
        matchesURL.append("http://www.nemzetisport.hu/adatbank/labdarugas/" +
                          "premier_league/2015_2016/");
        matchesURL.append(String.valueOf(round));
        return matchesURL.toString();
    }
    
    //adatok letöltése fájlokba
    private void downloadData(URL url, int round){
        try {
            File f = new File("2015_" + String.valueOf(round) + ".txt");
            System.out.println("Downloading file: " + "2015_" + round + ".txt");
            FileOutputStream os = new FileOutputStream("2015_" + String.valueOf(round) + ".txt");
            InputStream is = url.openConnection().getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            int c;
            while ((c=bis.read()) != -1) {                
                os.write(c);
            }
            is.close();
            bis.close();
            os.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //html-ből egy adott meccs dátumának kinyerése
    private Timestamp getDate(int round, int match){
        Timestamp ts = new Timestamp(0);
        String act;
        Elements dates;
        String datesString;
        Document doc;
        try {
            act = readFile("2015_" + String.valueOf(round)+".txt");
            doc = Jsoup.parse(act);
            dates = doc.select("div.col1");
            datesString = dates.get(match-1).text();
            String year;
            String month;
            String day;
            String hour;
            String min;
            if (datesString.length() == 8) {
                year = "20" + datesString.substring(0, 2);
                month = datesString.substring(3, 5);
                day = datesString.substring(6, 8);
                hour = "00";
                min = "00";
            }
            else if (datesString.length() == 0) {
                year = "1970";
                month = "01";
                day = "01";
                hour = "00";
                min = "00";
            }
            else {
                hour = datesString.substring(0, 2);
                min = datesString.substring(3, 5);
                month = datesString.substring(6, 8);
                day = datesString.substring(9, 11);
                if (Integer.parseInt(month) > 7) {
                    year = "2015";
                }
                else {
                    year = "2016";
                }
            }
            DateFormat df = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
            ts.setTime(df.parse(year + "/" + month + "/" + day + " " + hour + ":" + min + ":00").getTime());
        } catch (IOException | ParseException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ts;
    }
    
    //html-ből egy adott meccs eredményének kinyerése
    private String getResult(int round, int match){
        String result = "N/A";
        String act;
        Elements results;
        Document doc;
        try {
            act = readFile("2015_" + String.valueOf(round)+".txt");
            doc = Jsoup.parse(act);
            results = doc.select("div.col_result");
            System.out.println("results size: " + results.size());
            System.out.println("match: " + match);
            if (!results.isEmpty() && results.size() >= match) {
                result = results.get(match-1).text();
            }
        } catch (IOException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    //html-ből egy adott meccs hazai csapatának kinyerése (majd a csapat ID-jének visszaadása)
    private int getHomeTeamId(int round, int match){
        int id = -1;
        String act;
        Elements homeTeam;
        String homeTeamString;
        Document doc;
        ResultSet rs;
        try {
            act = readFile("2015_" + String.valueOf(round)+".txt");
            doc = Jsoup.parse(act);
            homeTeam = doc.select("div.col2");
            homeTeamString = homeTeam.get(match-1).text();
            rs = dc.openConnection("TEAMS");
            boolean found = false;
            rs.first();
            do {
                if (homeTeamString.equals(rs.getString("NAME"))) {
                    id = rs.getInt("ID");
                    found = true;
                }
            } while (!found && rs.next());
            rs.close();
            
        } catch (IOException | SQLException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return id;
    }
    
    //html-ből egy adott meccs vendég csapatának kinyerése (majd a csapat ID-jének visszaadása)
    private int getAwayTeamId(int round, int match){
        int id = -1;
        String act;
        Elements awayTeam;
        String awayTeamString;
        Document doc;
        ResultSet rs;
        try {
            act = readFile("2015_" + String.valueOf(round)+".txt");
            doc = Jsoup.parse(act);
            awayTeam = doc.select("div.col3");
            awayTeamString = awayTeam.get(match-1).text();
            rs = dc.openConnection("TEAMS");
            boolean found = false;
            rs.first();
            do {
                if (awayTeamString.equals(rs.getString("NAME"))) {
                    id = rs.getInt("ID");
                    found = true;
                }
            } while (!found && rs.next());
            rs.close();
            
        } catch (IOException | SQLException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return id;
    }
    
    //html beolvasása egy fájlból Stringbe
    private String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
    
    //fő metódus, a többi metódus segítségével letölti az adatokat, és kiírja őket az adatbázisba
    private void MatchesConnect() throws MalformedURLException{
        URL url;
        ResultSet rs;
        int id = 380;
        try {
            for (int i = 0; i < 38; i++) {
                
                System.out.println(i);
                rs = dc.openConnection("MATCHES");
                url = new URL(MatchesURLBuilder(i+1));
                System.out.println(url);
                downloadData(url, i+1);
                rs.close();
                for (int j = 0; j < 10; j++) {
                    System.out.println(id + ";" + getHomeTeamId(i+1, j+1) + ";" + getAwayTeamId(i+1, j+1) + ";" + (i+1) + ";" + getDate(i+1, j+1) + ";" + getResult(i+1, j+1));
                    rs = dc.openConnection("MATCHES");
                    rs.moveToInsertRow();
                    rs.updateInt("ID", id);
                    rs.updateInt("HOME_TEAM_ID", getHomeTeamId(i+1, j+1));
                    rs.updateInt("AWAY_TEAM_ID", getAwayTeamId(i+1, j+1));
                    rs.updateInt("ROUND", i+1);
                    rs.updateTimestamp("MATCH_DATE", getDate(i+1, j+1));
                    rs.updateString("RESULT", getResult(i+1, j+1));
                    rs.insertRow();
                    rs.close();
                    id++;
                }
                rs.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DownloadMatches.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}