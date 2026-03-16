package analysis.PredicatePushdown;

import soot.Unit;

public class CompositePredicate extends Predicate {
    String operator;
    Predicate left;
    Predicate right;

    public CompositePredicate(String operator, Predicate left, Predicate right, Unit unit, String df_var) {
        super(null,unit,df_var);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator.toUpperCase() + " " + right + ")";
    }
}
