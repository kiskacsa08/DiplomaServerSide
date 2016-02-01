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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author egg
 */
public class DownloadMatches {
    
    //letöltés indítása, adatok kinyerése, kiírás adatbázisba
    public static void startDownload() throws SQLException, IOException, ParseException{
        ResultSet rs;
        DatabaseConnection dc = new DatabaseConnection();
        String result;
        String html;
        rs = dc.executeCommand("SELECT COUNT(*) FROM MATCHES");
        rs.first();
        int size = rs.getInt(1);
        System.out.println("Records: " + size);
        rs.close();
        dc.closeConnection();
        if (size < 381) {
            System.out.println("One season");
            int id = 380;
            for (int i = 1; i <= 38; i++) {
                System.out.println("Round " + i);
                html = downloadRound(i);
                System.out.println("Downloaded " + html.length() + " characters.");
                for (int j = 1; j <= 10; j++) {
                    rs = dc.openConnection("MATCHES");
                    int homeTeamId = getHomeTeamId(html, i, j);
                    int awayTeamId = getAwayTeamId(html, i, j);
                    Timestamp matchDate = getDate(html, i, j);
                    String matchResult = getResult(html, i, j);
                    System.out.println("ID: " + id + "; H: " + homeTeamId + "; A: " + awayTeamId + "; Round: " + i + "; Date: " + matchDate + "; Result: " + matchResult);
                    rs.moveToInsertRow();
                    rs.updateInt("ID", id);
                    rs.updateInt("HOME_TEAM_ID", homeTeamId);
                    rs.updateInt("AWAY_TEAM_ID", awayTeamId);
                    rs.updateInt("ROUND", i);
                    rs.updateTimestamp("MATCH_DATE", matchDate);
                    rs.updateString("RESULT", matchResult);
                    rs.insertRow();
                    rs.close();
                    dc.closeConnection();
                    id++;
                    System.out.println("Inserted");
                }
            }
        }
        else {
            System.out.println("Two seasons");
            rs = dc.openConnection("MATCHES");
            while (rs.next()) {
                result = rs.getString("RESULT");
                System.out.println("New match, result: " + result);
                if ("N/A".equals(result)) {
                    System.out.println("No result");
                    int round = rs.getInt("ROUND");
                    html = downloadRound(round);
                    System.out.println("Downloaded " + html.length() + " characters.");
                    for (int i = 1; i <= 10; i++) {
                        Timestamp matchDate = getDate(html, round, i);
                        String matchResult = getResult(html, round, i);
                        System.out.println("Date: " + matchDate + "; Result: " + matchResult);
                        rs.updateString("RESULT", matchResult);
                        rs.updateTimestamp("MATCH_DATE", matchDate);
                        rs.updateRow();
                        rs.next();
                    }
                    rs.previous();
                }
            }
            rs.close();
            dc.closeConnection();
        }
    }
    
    //URL felépítése egy adott fordulóhoz
    private static URL MatchesURLBuilder(int round) throws MalformedURLException{
        StringBuilder matchesURL = new StringBuilder();
        matchesURL.append("http://www.nemzetisport.hu/adatbank/labdarugas/" +
                          "premier_league/2015_2016/");
        matchesURL.append(String.valueOf(round));
        URL url = new URL(matchesURL.toString());
        return url;
    }
    
    //html-ből egy adott meccs dátumának kinyerése
    private static Timestamp getDate(String html, int round, int match) throws ParseException{
        Timestamp ts = new Timestamp(0);
        Elements dates;
        String datesString;
        Document doc;
        doc = Jsoup.parse(html);
        dates = doc.select("div.col1");
        datesString = dates.get(match-1).text();
        String year;
        String month;
        String day;
        String hour;
        String min;
        switch (datesString.length()) {
            case 8:
                year = "20" + datesString.substring(0, 2);
                month = datesString.substring(3, 5);
                day = datesString.substring(6, 8);
                hour = "00";
                min = "00";
                break;
            case 0:
                year = "1970";
                month = "01";
                day = "01";
                hour = "00";
                min = "00";
                break;
            default:
                hour = datesString.substring(0, 2);
                min = datesString.substring(3, 5);
                month = datesString.substring(6, 8);
                day = datesString.substring(9, 11);
                if (Integer.parseInt(month) > 7) {
                    year = "2015";
                }
                else {
                    year = "2016";
                }   break;
        }
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
        ts.setTime(df.parse(year + "/" + month + "/" + day + " " + hour + ":" + min + ":00").getTime());
        return ts;
    }
    
    //html-ből egy adott meccs eredményének kinyerése
    private static String getResult(String html, int round, int match){
        String result = "N/A";
        Elements results;
        Document doc;
        doc = Jsoup.parse(html);
        results = doc.select("div.col_result");
        System.out.println("results size: " + results.size());
        System.out.println("match: " + match);
        if (!results.isEmpty() && results.size() >= match) {
            result = results.get(match-1).text();
        }
        return result;
    }
    
    //html-ből egy adott meccs hazai csapatának kinyerése (majd a csapat ID-jének visszaadása)
    private static int getHomeTeamId(String html, int round, int match) throws SQLException {
        int id = -1;
        Elements homeTeam;
        String homeTeamString;
        Document doc;
        ResultSet rs;
        DatabaseConnection dc;
        doc = Jsoup.parse(html);
        dc = new DatabaseConnection();
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
        return id;
    }
    
    //html-ből egy adott meccs vendég csapatának kinyerése (majd a csapat ID-jének visszaadása)
    private static int getAwayTeamId(String html, int round, int match) throws SQLException{
        int id = -1;
        Elements awayTeam;
        String awayTeamString;
        Document doc;
        ResultSet rs;
        DatabaseConnection dc;
        doc = Jsoup.parse(html);
        dc = new DatabaseConnection();
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
        return id;
    }
    
    //adott forduló letöltése egy String-be
    private static String downloadRound(int round) throws IOException{
        URL url = MatchesURLBuilder(round);
        System.out.println(url);
        StringBuilder sb = new StringBuilder();
        InputStream is = url.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line=br.readLine()) != null) {                
            sb.append(line);
        }
        is.close();
        br.close();
        return sb.toString();
    }
}
