package analysis.PredicatePushdown;

import ir.IExpr;

import java.util.List;

public class Condition {
    String on;
    String op;
    String with;
    List<IExpr> param;

    public Condition(String on, String op, String with) {
        this.on = on;
        this.op = op;
        this.with = with;
    }

    public Condition(String on, String op, List<IExpr> param) {
        this.on = on;
        this.op = op;
        this.param = param;
    }


    public String getOn() {
        return on;
    }

    public void setOn(String on) {
        this.on = on;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getWith() {
        return with;
    }

    public void setWith(String with) {
        this.with = with;
    }

    public List<IExpr> getParam() {
        return param;
    }

    public void setParam(List<IExpr> param) {
        this.param = param;
    }
}