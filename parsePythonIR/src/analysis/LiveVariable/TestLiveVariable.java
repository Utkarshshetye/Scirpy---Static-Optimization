package analysis.LiveVariable;
import cfg.JPUnitGraph;
import ir.JPBody;
import ir.JPMethod;
import soot.Unit;
import cfg.CFG;
import soot.toolkits.scalar.FlowSet;

import java.util.Collections;
import java.util.Iterator;

public class TestLiveVariable {

    public static void analyzeLiveVariable(JPMethod method) {
        CFG cfg ;
        cfg = new CFG(method);

        LiveVariableAnalysis lva = new LiveVariableAnalysis(cfg.getUnitGraph());

        Iterator unitIterator = cfg.getUnitGraph().iterator();

        //generate flowset for each unit
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            System.out.println("  for " + unit);
            FlowSet set = (FlowSet) lva.getFlowBefore(unit);
           // System.out.println("\tBefore: " + set);

            set = (FlowSet) lva.getFlowAfter(unit);
           // System.out.println("\tAfter: " + set);
        }

        System.out.println("Attribute Live Variable Analysis");
        AttributeLiveVariableAnalysis alva = new AttributeLiveVariableAnalysis(cfg.getUnitGraph());

        unitIterator = cfg.getUnitGraph().iterator();

        //generate flowset for each unit
        int i=0;
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            i++;
            System.out.println(i+" "+   unit);
            FlowSet set = (FlowSet) alva.getFlowBefore(unit);
            //System.out.println("\tatt Before: " + set);

            set = (FlowSet) alva.getFlowAfter(unit);
            //System.out.println("\t att After: " + set);
        }
//		livenessAnalysisMap.put(method.getName(), liveness);
//
//		LoopInvAnalysis loopInvAnalysis = new LoopInvAnalysis(cfg.getUnitGraph());
//		loopInvAnalysisMap.put(method.getName(), loopInvAnalysis);
    }
}
