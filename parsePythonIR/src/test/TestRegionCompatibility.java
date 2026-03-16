package test;

import analysis.LiveVariable.LiveVariableAnalysis;
import cfg.CFG;
import ir.JPMethod;
import ir.Stmt.ImportStmt;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.toolkits.scalar.FlowSet;

import java.util.Iterator;

public class TestRegionCompatibility {

    CFG cfg ;
    JPMethod jpMethod;
    Iterator unitIterator;

    public TestRegionCompatibility(JPMethod jpMethod) {
        this.cfg = new CFG(jpMethod);
        this.jpMethod=jpMethod;
        this.unitIterator=cfg.getUnitGraph().iterator();
    }



    public void test(){
     while (unitIterator.hasNext())
        {
            Unit unit = (Unit) unitIterator.next();
            if (unit instanceof GotoStmt) {
                System.out.println("Go to works");
            }
        }
        //System.exit(0);

    }
}
