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
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author egg
 */
public class DownloadMatches {
    
    //letöltés indítása, adatok kinyerése, kiírás adatbázisba.
    public static void startDownload() throws SQLException, IOException, ParseException{
        ResultSet rs;
        DatabaseConnection dc = new DatabaseConnection();
        String result;
        String html = "";
        rs = dc.executeCommand("SELECT COUNT(*) FROM MATCHES");
        Timestamp oneWeekAfter = new Timestamp(new Date().getTime() + 7*24*60*60*1000);
        
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
                int match = 1;
                for (int j = 1; j <= 10; j++) {
                    rs = dc.openConnection("MATCHES");
                    int homeTeamId = getHomeTeamId(html, i, j);
                    int awayTeamId = getAwayTeamId(html, i, j);
                    Timestamp matchDate = getDate(html, i, j);
                    String matchResult;
                    if (getMatchState(html, i, j) == 0) {
                        System.out.println("van eredmeny");
                        matchResult = getResult(html, i, match);
                        match++;
                    }
                    else {
                        System.out.println("nincs eredmeny");
                        matchResult = "N/A";
                    }
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
            int downloadedRound = 0;
            while (rs.next()) {
                result = rs.getString("RESULT");
                int homeTeam = rs.getInt("HOME_TEAM_ID");
                int awayTeam = rs.getInt("AWAY_TEAM_ID");
                System.out.println("New match, " + homeTeam + " - " + awayTeam + ", result: " + result);
                int actRound = rs.getInt("ROUND");
                Timestamp dbMatchDate = rs.getTimestamp("MATCH_DATE");
                System.out.println("Round(double): " + downloadedRound);
                System.out.println("Round(int): " + actRound);
                if ("N/A".equals(result) && dbMatchDate.before(oneWeekAfter)) {
                    System.out.println("No result");
                    int id = rs.getInt("ID");
                    id = id/10;
                    id = id*10;
                    System.out.println("ID: " + id);
                    if (actRound != downloadedRound) {
                        html = downloadRound(actRound);
                        downloadedRound = actRound;
                    }
                    int pos = getMatchPosition(html, actRound, homeTeam);
                    System.out.println("Downloaded " + html.length() + " characters.");
                    System.out.println("Position: " + pos);
                    Timestamp matchDate = getDate(html, actRound, pos);
                    String matchResult;
                    switch (getMatchState(html, actRound, pos)) {
                        case 0:
                            System.out.println("van eredmeny");
                            matchResult = getResult(html, actRound, pos);
                            break;
                        case 2:
                            System.out.println("live match");
                            matchResult = "N/A";
                            break;
                        default:
                            System.out.println("nincs eredmeny");
                            matchResult = "N/A";
                            break;
                    }
                    System.out.println("Date: " + matchDate + "; Result: " + matchResult);
                    rs.updateString("RESULT", matchResult);
                    rs.updateTimestamp("MATCH_DATE", matchDate);
                    rs.updateRow();
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
        if (!results.isEmpty()) {
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
    
    private static String getTeamName(int id) throws SQLException{
        String name = "";
        DatabaseConnection dc = new DatabaseConnection();
        ResultSet rs = dc.openConnection("TEAMS");
        boolean found = false;
        rs.first();
        do {
            if (id == rs.getInt("ID")) {
                name = rs.getString("NAME");
                found = true;
            }
        } while (!found && rs.next());
        rs.close();
        return name;
    }
    
    private static int getMatchState(String html, int round, int match){
        int state = 0;
        Elements states;
        String statesString;
        Document doc;
        doc = Jsoup.parse(html);
        states = doc.select("div.event_line");
        statesString = states.get(match-1).className();
        System.out.println(statesString);
        if (statesString.equals("event_line high")) {
            state = 1;
        }
        if (statesString.equals("event_line live")) {
            state = 2;
        }
        return state;
    }
    
    private static int getMatchPosition(String html, int round, int homeTeam) throws SQLException{
        int pos = 1;
        Elements homeTeams;
        String statesString;
        Document doc;
        doc = Jsoup.parse(html);
        String home = getTeamName(homeTeam);
        homeTeams = doc.select("div.col2");
        for (int i = 0; i < homeTeams.size(); i++) {
            String homeTeamString = homeTeams.get(i).text();
            if (homeTeamString.equals(home)) {
                pos = i+1;
                break;
            }
        }
        return pos;
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
