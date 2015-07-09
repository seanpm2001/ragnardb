package ragnardb.runtime;

import gw.lang.reflect.IType;
import ragnardb.RagnarDB;
import ragnardb.api.ISQLResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SQLRecord implements ISQLResult
{
  private ValMap _values = new ValMap();
  private String _tableName;
  private boolean _persisted;
  private String _idColumn;

  public SQLRecord( String tableName, String idColumn ) {
    _tableName = tableName;
    _idColumn = idColumn;
  }

  @Override
  public Object getRawValue( String property )
  {
    return _values.get( property );
  }

  @Override
  public void setRawValue( String property, Object value )
  {
    _values.put( property, value );
  }

  @Override
  public String getTableName()
  {
    return _tableName;
  }

  public boolean save() {
    if(_persisted) {
      return update();
    } else {
      return create();
    }
  }

  public boolean create()
  {
    StringBuilder valNames = new StringBuilder();
    StringBuilder valPlaceholders = new StringBuilder();
    LinkedList<Object> vals = new LinkedList<>();

    for( Iterator<Map.Entry<String, Object>> values = _values.entrySet().iterator(); values.hasNext(); )
    {
      Map.Entry<String, Object> pair = values.next();

      valNames.append( pair.getKey() );
      valPlaceholders.append( "?" );
      vals.add( pair.getValue() );

      if( values.hasNext() )
      {
        valNames.append( "," );
        valPlaceholders.append( "," );
      }
    }

    String sql = "INSERT INTO " + _tableName + " (" + valNames + ")" + " VALUES (" + valPlaceholders + ")";

    try
    {
      PreparedStatement preparedStatement = RagnarDB.prepareStatement( sql, vals, Statement.RETURN_GENERATED_KEYS );
      preparedStatement.executeUpdate();
      ResultSet tableKeys = preparedStatement.getGeneratedKeys();
      if(tableKeys.next()) {
        long autoGeneratedID = tableKeys.getInt( 1 );
        setRawValue( _idColumn, autoGeneratedID );
      }
    }
    catch( SQLException e )
    {
      e.printStackTrace();
    }
    return true;
  }

  public boolean update()
  {
    StringBuilder valNames = new StringBuilder();
    LinkedList<Object> vals = new LinkedList<>();

    for( Iterator<Map.Entry<String, Object>> values = _values.entrySet().iterator(); values.hasNext(); )
    {
      Map.Entry<String, Object> pair = values.next();
      if( !pair.getKey().equals( _idColumn ) )
      {
        valNames.append( pair.getKey() ).append("=?");
        vals.add(pair.getValue());
        if( values.hasNext() )
        {
          valNames.append( "," );
        }
      }
    }

    String sql = "UPDATE " + _tableName + " SET " + valNames + " WHERE " + _idColumn + "=?" ;
    vals.add( getRawValue( _idColumn ) );

    try
    {
      PreparedStatement preparedStatement = RagnarDB.prepareStatement( sql, vals );
      preparedStatement.executeUpdate();
    }
    catch( SQLException e )
    {
      e.printStackTrace();
    }

    return true;
  }

  public static SQLRecord read( String tableName, String idColumn, Object idValue ) throws SQLException
  {
    PreparedStatement preparedStatement = RagnarDB.prepareStatement( "SELECT * FROM " + tableName + " WHERE " + idColumn + "=?", Collections.singletonList( idValue ) );
    ResultSet resultSet = preparedStatement.executeQuery();
    if( resultSet.next() )
    {
      SQLRecord record = new SQLRecord( tableName, idColumn );
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();
      int i = 1;
      while( i <= columnCount )
      {
        int columnType = metaData.getColumnType( i );
        String columnName = metaData.getColumnName( i );
        switch( columnType )
        {
          case Types.INTEGER:
            record.setRawValue( columnName, resultSet.getInt( i ) );
            break;
          case Types.BIGINT:
            record.setRawValue( columnName, resultSet.getLong( i ) );
            break;
          default:
            record.setRawValue( columnName, resultSet.getObject( i ) );
            break;
        }
        i++;
      }
      return record;
    }
    else
    {
      return null;
    }
  }

  static <T> Iterable<T> select(String sql, List vals, IType impl) throws SQLException
  {
    PreparedStatement preparedStatement = RagnarDB.prepareStatement( sql, vals );
    ResultSet resultSet = preparedStatement.executeQuery();
    List<T> results = new LinkedList<>();
    while( resultSet.next() )
    {
      SQLRecord record = (SQLRecord)impl.getTypeInfo().getCallableConstructor().getConstructor().newInstance();
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();
      int i = 1;
      while( i <= columnCount )
      {
        int columnType = metaData.getColumnType( i );
        String columnName = metaData.getColumnName( i );
        switch( columnType )
        {
          case Types.INTEGER:
            record.setRawValue( columnName, resultSet.getInt( i ) );
            break;
          case Types.BIGINT:
            record.setRawValue( columnName, resultSet.getLong( i ) );
            break;
          default:
            record.setRawValue( columnName, resultSet.getObject( i ) );
            break;
        }
        i++;
      }
      results.add( (T) record );
    }
    return results;
  }

  public boolean delete()
  {
    LinkedList<Object> vals = new LinkedList<>();
    String sql = "DELETE FROM " + _tableName + " WHERE " + _idColumn + "=?" ;
    vals.add( getRawValue( _idColumn ) );
    try
    {
      PreparedStatement preparedStatement = RagnarDB.prepareStatement( sql, vals );
      preparedStatement.executeUpdate();
    }
    catch( SQLException e )
    {
      e.printStackTrace();
    }
    return true;
  }

  static class ValMap extends HashMap<String, Object>
  {
    @Override
    public Object put( String key, Object value )
    {
      return super.put( key.toLowerCase(), value );
    }

    @Override
    public Object get( Object key )
    {
      return super.get( ((String) key).toLowerCase() );
    }
  }
}