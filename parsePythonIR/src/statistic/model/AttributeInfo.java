package statistic.model;

import DataFileAnalysis.model.DataType;

public class AttributeInfo {
    DataType type;
    String minVal;
    String maxVal;
    long distinctVals;

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public String getMinVal() {
        return minVal;
    }

    public void setMinVal(String minVal) {
        this.minVal = minVal;
    }

    public String getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(String maxVal) {
        this.maxVal = maxVal;
    }

    public long getDistinctVals() {
        return distinctVals;
    }

    public void setDistinctVals(long distinctVals) {
        this.distinctVals = distinctVals;
    }
}
