package ragnardb.parser.ast;

import ragnardb.parser.TokenType;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Factor {
  private ArrayList<Term> _terms;

  /*Contains data about operators in this factor: 0-TIMES; 1-DIVIDE; 2-MOD*/
  private ArrayList<Integer> _operators;

  public Factor(){
    _terms = new ArrayList<Term>();
    _operators = new ArrayList<Integer>();
  }

  public Factor(Term t){
    _terms = new ArrayList<Term>();
    _terms.add(t);
    _operators = new ArrayList<Integer>();
  }

  private void addTerm(Term t){
    _terms.add(t);
  }

  private void addOperator(char operator){
    switch (operator){
      case '*':
        _operators.add(0);
        break;
      case '/':
        _operators.add(1);
        break;
      case '%':
        _operators.add(2);
        break;
      default:
        _operators.add(-1);
        break;
    }
  }

  public void add(String operator, Term t){
    char o = operator.charAt(0);
    addOperator(o);
    addTerm(t);
  }

  public ArrayList<Term> getTerms(){return _terms;}

  public ArrayList<Integer> getOperators(){return _operators;}

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Factor>\n");
    sb.append('\t');
    sb.append(_terms.get(0));
    for(int i=0;i<_operators.size();i++){
      switch (_operators.get(i)){
        case 0:
          sb.append("\t * ");
          break;
        case 1:
          sb.append("\t / ");
          break;
        case 2:
          sb.append("\t % ");
          break;
      }
      sb.append(_terms.get(i+1));
    }
    return sb.toString();
  }


}
