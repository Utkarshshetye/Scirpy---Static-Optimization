package ir.expr;

import ir.IExpr;
import ir.JPExpr;
import soot.Local;

import java.util.ArrayList;
import java.util.List;

public class Starred extends JPExpr implements IExpr {
    IExpr value;

    public Starred(IExpr value) {
        this.value = value;
    }

    public IExpr getValue() {
        return value;
    }

    public void setValue(IExpr value) {
        this.value = value;
    }

    public String toString() {
        return "*" + value.toString();
    }

    @Override
    public Object clone() {
        return new Starred((IExpr) value.clone());
    }

    @Override
    public List<Local> getLocals() {
        return value.getLocals();
    }

    @Override
    public List<Name> getDataFrames() {
        return value.getDataFrames();
    }

    @Override
    public boolean isDataFrame() {
        return value.isDataFrame();
    }
}
