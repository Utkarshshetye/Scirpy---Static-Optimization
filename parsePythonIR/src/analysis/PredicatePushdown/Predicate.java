package analysis.PredicatePushdown;

import ir.IExpr;
import ir.expr.Constant;
import ir.expr.ListComp;
import soot.Unit;

import java.util.ArrayList;
import java.util.List;

public class Predicate {
    String df_var;
    Unit unit;
    Unit parentUnit;
    Condition cond;
    // String cond;
    String col;
    FilterTypes fTypes;
    Integer iter;

    // TODO: List of column, complete and simple examples
    public Predicate(String df_var, Unit unit, Condition cond) {
        this.df_var = df_var;
        this.unit = unit;
        this.cond = cond;
    }

    public Predicate(Condition cond, Unit parentUnit, Unit unit, String df_var) {
        this.cond = cond;
        this.parentUnit = parentUnit;
        this.unit = unit;
        this.df_var = df_var;
    }

    public Predicate(Condition cond, Unit unit, String df_var) {
        this.cond = cond;
        this.unit = unit;
        this.df_var = df_var;
    }

    public String getDf_var() {
        return df_var;
    }

    public void setDf_var(String df_var) {
        this.df_var = df_var;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Unit getParentUnit() {
        return parentUnit;
    }

    public void setParentUnit(Unit parentUnit) {
        this.parentUnit = parentUnit;
    }

    public Condition getCond() {
        return cond;
    }

    public void setCond(Condition cond) {
        this.cond = cond;
    }

    public String getCol() {
        return col;
    }

    public void setCol(String col) {
        this.col = col;
    }

    public Integer getIter() {
        return iter;
    }

    public void setIter(Integer iter) {
        this.iter = iter;
    }

    @Override
    public String toString() {

//        if (cond!=null && !cond.getParam().isEmpty()) {
//            IExpr firstParam = cond.getParam().get(0);
//
//            if (firstParam instanceof ListComp) {
//                ListComp comp = (ListComp) firstParam;
//                List<IExpr> exp = comp.getElts();
//                String s = "";
//
//                for (int i = 0; i < exp.size(); i++) {
//                    IExpr expr = exp.get(i);
//
//                    if (expr instanceof Constant) {
//                        Constant constExpr = (Constant) expr;
//                        String expValue = constExpr.getValue();
//
//                        s += "('" + cond.getOn() + "', '==', '" + expValue + "')";
//
//                        if (i < exp.size()) {
//                            s += ", ";
//                        }
//                    }
//                }
//
//                return s;
//            }
//        }

        return "('" + cond.getOn() + "', '" + cond.getOp() + "', " + cond.getWith() + ")";
    }
}
