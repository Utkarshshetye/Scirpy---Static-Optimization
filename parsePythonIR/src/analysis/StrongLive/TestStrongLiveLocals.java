package analysis.StrongLive;


import analysis.LiveVariable.LiveVariableAnalysis;
import cfg.JPUnitGraph;
import ir.JPBody;
import ir.JPMethod;
import soot.Unit;
import cfg.CFG;
import soot.toolkits.scalar.FlowSet;

import java.util.Iterator;

public class TestStrongLiveLocals {
	public static void testStrongLiveLocals(JPMethod m){
		System.out.println("Strong Live Locals For: " + m.getName());
		JPBody body = m.getBody();
		JPUnitGraph unitGraph = new JPUnitGraph(body);
		StrongLiveLocals lid =  new StrongLiveLocals(unitGraph);
		for (Unit u : body.getUnits()) {
			System.out.println("  for " + u);
			System.out.println("\tBefore: " + lid.getLiveLocalsBefore(u));
			System.out.println("\tAfter: " + lid.getLiveLocalsAfter(u));
		}
	}

	public static synchronized void analyzeMethod(JPMethod method) {
		CFG cfg ;
		cfg = new CFG(method);

		StrongLiveLocals liveness = new StrongLiveLocals(cfg.getUnitGraph());
//		livenessAnalysisMap.put(method.getName(), liveness);
//
//		LoopInvAnalysis loopInvAnalysis = new LoopInvAnalysis(cfg.getUnitGraph());
//		loopInvAnalysisMap.put(method.getName(), loopInvAnalysis);
	}

	public static void analyzeMyLiveness(JPMethod method) {
		CFG cfg ;
		cfg = new CFG(method);

		LiveVariableAnalysis lva = new LiveVariableAnalysis(cfg.getUnitGraph());

		Iterator unitIterator = cfg.getUnitGraph().iterator();

		//generate flowset for each unit
		while (unitIterator.hasNext()) {
			Unit unit = (Unit) unitIterator.next();
			System.out.println("  for " + unit);
			FlowSet set = (FlowSet) lva.getFlowBefore(unit);
			System.out.println("\tBefore: " + set);

			set = (FlowSet) lva.getFlowAfter(unit);
			System.out.println("\tAfter: " + set);
		}
//		livenessAnalysisMap.put(method.getName(), liveness);
//
//		LoopInvAnalysis loopInvAnalysis = new LoopInvAnalysis(cfg.getUnitGraph());
//		loopInvAnalysisMap.put(method.getName(), loopInvAnalysis);
	}
}
