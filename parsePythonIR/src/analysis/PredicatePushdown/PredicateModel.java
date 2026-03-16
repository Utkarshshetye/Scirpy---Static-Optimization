package analysis.PredicatePushdown;

import soot.Unit;

import java.util.List;
import java.util.Map;

public class PredicateModel {
    int intialLine;
    int destLine;

    List<String> predColumns;
    // Map<Unit, List<String>> unitToColMapping;

    Unit filterUnit;
    Unit insertBeforeUnit;
    boolean reorderDone;

    public PredicateModel(int intialLine, int destLine, List<String> predColumns) {
        this.intialLine = intialLine;
        this.destLine = destLine;
        this.predColumns = predColumns;
    }

    public PredicateModel(Unit filter, Unit Op, List<String> predCols, boolean status){
        this.filterUnit = filter;
        this.insertBeforeUnit = Op;
        this.predColumns = predCols;
        this.reorderDone = status;
    }

    public int getDestLine() {
        return destLine;
    }

    public void setDestLine(int destLine) {
        this.destLine = destLine;
    }

    public int getIntialLine() {
        return intialLine;
    }

    public void setIntialLine(int intialLine) {
        this.intialLine = intialLine;
    }

    public List<String> getPredColumns() {
        return predColumns;
    }

    public void setPredColumns(List<String> predColumns) {
        this.predColumns = predColumns;
    }

    public Unit getFilterUnit() {
        return filterUnit;
    }

    public void setFilterUnit(Unit filterUnit) {
        this.filterUnit = filterUnit;
    }

    public Unit getInsertBeforeUnit() {
        return insertBeforeUnit;
    }

    public void setInsertBeforeUnit(Unit insertBeforeUnit) {
        this.insertBeforeUnit = insertBeforeUnit;
    }

    public boolean isReorderDone() {
        return reorderDone;
    }

    public void setReorderDone(boolean reorderDone) {
        this.reorderDone = reorderDone;
    }

}
