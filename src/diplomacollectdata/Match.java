package diplomacollectdata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author egg
 */

//tároló osztály meccseknek
public class Match {
    private final int teamAId;
    private final int teamBId;
    private final int matchId;
    private final int round;
    private DatabaseConnection dc;

    public Match(int teamAId, int teamBId, int matchId, int round) {
        this.teamAId = teamAId;
        this.teamBId = teamBId;
        this.matchId = matchId;
        this.round = round;
        dc = new DatabaseConnection();
    }

    public String getHomeTeamURLString(){
        return getTeamURLString(teamAId);
    }
    
    public String getAwayTeamURLString(){
        return getTeamURLString(teamBId);
    }

    public int getMatchId() {
        return matchId;
    }
    
    public int getRound(){
        return round;
    }
    
    private String getTeamURLString(int id){
        String teamURLString = null;
        try {
            ResultSet rs = dc.openConnection("TEAMS");
            while (rs.next()) {                
                if (rs.getInt("ID") == id) {
                    teamURLString = rs.getString("URL_NAME");
                }
            }
            rs.close();
            dc.closeConnection();
        } catch (SQLException ex) {
            Logger.getLogger(Match.class.getName()).log(Level.SEVERE, null, ex);
        }
        return teamURLString;
    }
}
