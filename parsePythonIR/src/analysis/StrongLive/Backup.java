package analysis.StrongLive;

public class Backup {
    /*
    package analysis.StrongLive;
import ir.JPStmt;
import ir.Stmt.AssignmentStmtSoot;
import soot.options.*;

import soot.*;
import java.util.*;

import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
public class StrongLiveLocals implements LiveLocals {

    Map<Unit, List> unitToLocalsAfter;
    Map<Unit, List> unitToLocalsBefore;

    public StrongLiveLocals(UnitGraph graph) {
        if(Options.v().time())
            Timers.v().liveTimer.start();

        if(Options.v().verbose())
            G.v().out.println("[" + graph.getBody().getMethod().getName() +
                    "]     Constructing StrongLiveLocals...");


        StrongLiveLocalsAnalysis analysis = new StrongLiveLocalsAnalysis(graph);

        if(Options.v().time())
            Timers.v().livePostTimer.start();
        //TODO check timers and use if required
        unitToLocalsAfter = new HashMap<Unit, List>(graph.size() * 2 + 1, 0.7f);
        unitToLocalsBefore = new HashMap<Unit, List>(graph.size() * 2 + 1, 0.7f);
        Iterator unitIterator = graph.iterator();

        //generate flowset for each unit
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            FlowSet set = (FlowSet) analysis.getFlowBefore(unit);
            unitToLocalsBefore.put(unit, Collections.unmodifiableList(set.toList()));

            set = (FlowSet) analysis.getFlowAfter(unit);
            unitToLocalsAfter.put(unit, Collections.unmodifiableList(set.toList()));
        }

    }
        @Override
        public List getLiveLocalsBefore (Unit unit){
            return unitToLocalsBefore.get(unit);
        }

        @Override
        public List getLiveLocalsAfter (Unit unit)
        {
            return unitToLocalsAfter.get(unit);

        }


}
    class StrongLiveLocalsAnalysis extends BackwardFlowAnalysis
    {
        FlowSet emptyFlowSet;
        Map<Unit, FlowSet> unitToGenerateSet;
        Map<Unit, FlowSet> unitToKillSet;
        Map<Unit, FlowSet> unitToRealUseSet;

        StrongLiveLocalsAnalysis(UnitGraph graph) {

            super(graph);
            emptyFlowSet = new ArraySparseSet();
            Iterator unitIterator;
            // Create kill sets.
            unitToKillSet = new HashMap<Unit, FlowSet>(graph.size() * 2 + 1, 0.7f);
            unitIterator = graph.iterator();
            while(unitIterator.hasNext())
            {
                Unit unit = (Unit) unitIterator.next();
                FlowSet killSet = emptyFlowSet.clone();
                Iterator boxIterator = unit.getDefBoxes().iterator();
                while(boxIterator.hasNext())
                {
                    ValueBox box = (ValueBox) boxIterator.next();
                    if(box.getValue() instanceof Local)
                        killSet.add(box.getValue(), killSet);
                }
                unitToKillSet.put(unit, killSet);
            }

            // Create generate sets
            unitToGenerateSet = new HashMap<Unit, FlowSet>(graph.size() * 2 + 1, 0.7f);
            unitIterator = graph.iterator();
            //int i=0;
            while(unitIterator.hasNext())
            {
                Unit unit = (Unit) unitIterator.next();
                FlowSet genSet = emptyFlowSet.clone();
                Iterator boxIterator = unit.getUseBoxes().iterator();

                while(boxIterator.hasNext())
                {
                    ValueBox box = (ValueBox) boxIterator.next();
                    if(box.getValue() instanceof Local)
                        genSet.add(box.getValue(), genSet);
                }
                unitToGenerateSet.put(unit, genSet);
            }

            // Create RealUse Set
            unitToRealUseSet = new HashMap<Unit, FlowSet>(graph.size() * 2 + 1, 0.7f);
            unitIterator = graph.iterator();
            while(unitIterator.hasNext())
            {
                Unit unit = (Unit) unitIterator.next();
                FlowSet realUseSet = emptyFlowSet.clone();
                if (!(unit instanceof JPStmt)) {
                    // FIXME: Approximate use-set as real use
                    unitToRealUseSet.put(unit, unitToGenerateSet.get(unit));
                    continue;
                }
                //TODO not working
                //Iterator boxIt = ((JPStmt)unit).getRealUseBoxes().iterator();
                Iterator boxIt;
//                while(boxIt.hasNext())
//                {
//                    ValueBox box = (ValueBox) boxIt.next();
//                    if(box.getValue() instanceof Local)
//                        realUseSet.add(box.getValue(), realUseSet);
//                }
//                unitToRealUseSet.put(unit, realUseSet);
            }

            if(Options.v().time())
                Timers.v().liveSetupTimer.end();

            if(Options.v().time())
                Timers.v().liveAnalysisTimer.start();

            doAnalysis();

            if(Options.v().time())
                Timers.v().liveAnalysisTimer.end();

    }
        protected Object newInitialFlow()
        {
            return emptyFlowSet.clone();
        }

        protected Object entryInitialFlow()
        {
            return emptyFlowSet.clone();
        }

        protected void flowThrough(Object outValue, Object unit, Object inValue)
        {
            FlowSet out = (FlowSet) outValue, in = (FlowSet) inValue;
            FlowSet defs = unitToKillSet.get(unit);
            FlowSet uses = unitToGenerateSet.get(unit);
            FlowSet realUses;
            FlowSet transRealUses = emptyFlowSet.clone();

            // Perform kill
            out.difference(defs, in);

            // Perform generation

            // First include all REAL uses.
            realUses = unitToRealUseSet.get(unit);
            //realUses = expandStructs(realUses);
            in.union(realUses, in);

            // Now, check if any definition has a real
            // use at OUT. If so include corresponding
            // uses as well.
            defs.intersection(out, transRealUses);

            if (transRealUses.isEmpty()) {
                return;
            }

            // There are some defs having real use on OUT.
            // We must include a use x-f in the gen set
            // if it is used to assign y-f which is in
            // defs INTERSET out (i.e., in tansRealUses).
            // TODO: The following logic is temporary. We
            // must implement this in a robust manner using
            // rmap etc.

            Value lhs = null;
            Value rhs = null;
            if (unit instanceof AssignmentStmtSoot) {
                // for an assignment
                // x = y
                // transfer any fields x-f used through x to y
                AssignmentStmtSoot assignmentStmt = (AssignmentStmtSoot)unit;
                lhs = assignmentStmt.getLeftOp();
                rhs = assignmentStmt.getRightOp();
            }
            //TODO add other statement types her

            else {
                return;
            }


            if (!(lhs instanceof Local))
                return;

            if (!(rhs instanceof Local))
                return;


           // in.union(transfers);
        }
        //merge operation of analysis
        protected void merge(Object inOne, Object inTwo, Object out)
        {
            FlowSet inFlowSetOne = (FlowSet) inOne;
            FlowSet inFlowSetTwo = (FlowSet) inTwo;
            FlowSet outSet = (FlowSet) out;
            inFlowSetOne.union(inFlowSetTwo, outSet);
        }

        protected void copy(Object source, Object destination)
        {
            FlowSet sourceFlowSet = (FlowSet) source;
            FlowSet destinationFlowSet = (FlowSet) destination;

            sourceFlowSet.copy(destinationFlowSet);
        }

//        private FlowSet expandStructs(FlowSet s)
//        {
//            Iterator eltIt = s.iterator();
//            FlowSet resultSet = emptyFlowSet.clone();
//            while (eltIt.hasNext()) {
//                Local elt = (Local) eltIt.next();
//                FlowSet expanded = expandOne(elt);
//                resultSet.union(expanded);
//            }
//            return resultSet;
//        }



    }


     */
}
