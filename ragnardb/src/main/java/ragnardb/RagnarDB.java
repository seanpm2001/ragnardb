package ragnardb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class RagnarDB
{
  private static String g_DBURL = "";

  public static void setDBUrl(String url) {
    g_DBURL = url;
  }

  public static String getDBUrl() {
    return g_DBURL;
  }

  public static Connection getConnection() throws SQLException
  {
    return DriverManager.getConnection(g_DBURL);
  }

  public static PreparedStatement prepareStatement( String sql, List vals) throws SQLException
  {
    Connection conn = getConnection();
    maybeLog(sql, vals);
    PreparedStatement stmt = conn.prepareStatement( sql );
    setVals(vals, stmt);
    return stmt;
  }

  public static PreparedStatement prepareStatement( String sql, List vals, int autoGeneratedKeys ) throws SQLException
  {
    Connection conn = getConnection();
    maybeLog(sql, vals);
    PreparedStatement stmt = conn.prepareStatement( sql, autoGeneratedKeys );
    setVals(vals, stmt);
    return stmt;
  }

  private static void maybeLog( String sql, List vals )
  {
    System.out.println("RagnarDB SQL : " + sql + " : " + vals);
  }

  private static void setVals( List vals, PreparedStatement stmt ) throws SQLException
  {
    for( int i = 0; i < vals.size(); i++ )
    {
      Object obj = vals.get(i);
      if(obj instanceof String) {
        stmt.setString(i + 1, (String) obj);
      } else {
        stmt.setObject( i + 1, obj );
      }
    }
  }

  public static boolean execStatement( String setup ) throws SQLException
  {
    PreparedStatement stmt = prepareStatement( setup, Collections.emptyList() );
    return stmt.execute();
  }

  public static int count( String tableName ) throws SQLException
  {
    Connection conn = getConnection();
    PreparedStatement stmt = prepareStatement( "SELECT COUNT(1) FROM " + tableName, Collections.emptyList() );
    ResultSet resultSet = stmt.executeQuery();
    resultSet.next();
    return resultSet.getInt( 1 );
  }
}
