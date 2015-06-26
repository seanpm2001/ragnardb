package ragnardb.parser.ast;

import ragnardb.plugin.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pjennings on 6/23/2015.
 */
public class CreateTable {
  private List<ColumnDefinition> columns;
  private List<Constraint> constraints;
  private String _name;
  private int _line;
  private int _col;
  private int _offset;
  private int _length;
  //private List<Constraint> Constraints;

  public CreateTable(String name){
    columns = new ArrayList<>();
    constraints = new ArrayList<>();
    _name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  public void append(ColumnDefinition c){
    columns.add(c);
  }

  public void append(Constraint c){
    constraints.add(c);
  }

  public List<ColumnDefinition> getColumnDefinitions(){
    return columns;
  }
  public List<Constraint> getConstraints(){
    return constraints;
  }

  public String getName(){
    return _name;
  }

  public ColumnDefinition getColumnDefinitionByName(String name){
    for(ColumnDefinition col : this.getColumnDefinitions()){
      if(col.getColumnName().equals(name)){
        return col;
      }
    }
    return null;
  }

  public void setLoc(int line, int col, int offset, int length) {
    _line = line;
    _col = col;
    _offset = offset;
    _length = length;
  }

  public int getLine(){
    return _line;
  }

  public int getCol(){
    return _col;
  }

  public int getOffset() {
    return _offset;
  }

}
