package diplomacollectdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 *
 * @author egg
 */
public class ExportImportData {
    
    public static void saveCSV(String table, String fileName) throws IOException, SQLException{
        System.out.println(fileName);
        FileWriter fw = new FileWriter(fileName);
        DatabaseConnection db = new DatabaseConnection();
        ResultSet rs = db.openConnection(table);
        ResultSetMetaData rsmd = rs.getMetaData();
        int j;
        for (j = 1; j < rsmd.getColumnCount(); j++) {
            fw.append(rsmd.getColumnName(j));
            fw.append(',');
        }
        fw.append(rsmd.getColumnName(j));
        fw.append('\n');
        
        int i;
        while (rs.next()) {
            for (i = 1; i < rsmd.getColumnCount(); i++) {
                fw.append(String.valueOf(rs.getObject(i)));
                fw.append(',');
            }
            fw.append(String.valueOf(rs.getObject(i)));
            fw.append('\n');
        }
        fw.flush();
        fw.close();
        rs.close();
        db.closeConnection();
        System.out.println("File saved");
    }
    
    public static void loadCSV(String table, String fileName) throws FileNotFoundException, SQLException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        DatabaseConnection db = new DatabaseConnection();
        db.openConnection(table);
        if (table.equals("MATCHES")) {
            db.deleteSpecificRows("MATCHES", "ID > 379");
        }
        else {
            db.deleteAllRows(table);
        }
        db.closeConnection();
        System.out.println("egy");
        ResultSet rs = db.openConnection(table);
        line = br.readLine();
        System.out.println("ketto");
        int columns = line.length() - line.replace(",", "").length() + 1;
        int k = 0;
        while ((line = br.readLine()) != null) {
            System.out.println("harom " + k);
            String[] values = line.split(",");
            rs.moveToInsertRow();
            for (int i = 1; i <= columns; i++) {
                rs.updateObject(i, values[i-1]);
            }
            rs.insertRow();
            k++;
        }
        System.out.println("negy");
        br.close();
        rs.close();
        db.closeConnection();
    }
    
}
