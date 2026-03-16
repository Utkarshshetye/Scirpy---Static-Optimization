package statistic.model;
import java.util.List;

public class DataSetInfo {
    long ntuples;
    long nblocks;
    long tupleSize;

    public long getNtuples() {
        return ntuples;
    }

    public void setNtuples(long ntuples) {
        this.ntuples = ntuples;
    }

    public long getNblocks() {
        return nblocks;
    }

    public void setNblocks(long nblocks) {
        this.nblocks = nblocks;
    }

    public long getTupleSize() {
        return tupleSize;
    }

    public void setTupleSize(long tupleSize) {
        this.tupleSize = tupleSize;
    }

    public int getBlockingFactor() {
        return blockingFactor;
    }

    public void setBlockingFactor(int blockingFactor) {
        this.blockingFactor = blockingFactor;
    }

    public List getAttributes() {
        return attributes;
    }

    public void setAttributes(List attributes) {
        this.attributes = attributes;
    }

    //no of tuples in a block
    int blockingFactor;
    List attributes;



}
