package analysis.interprocedural;

import analysis.PythonScene;
import ir.IExpr;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.expr.Call;
import ir.expr.Name;
import ir.expr.Str;
import ir.expr.Subscript;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.toolkits.scalar.FlowSet;

import java.util.Iterator;

public class Backup {
    /*
    package analysis.interprocedural;

import analysis.LiveVariable.LiveVariableAnalysis;
import analysis.Pandas.PandasAPIs;
import analysis.PythonScene;
import ir.IExpr;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.Stmt.CallExprStmt;
import ir.expr.Call;
import ir.expr.Name;
import ir.expr.Str;
import ir.expr.Subscript;
import soot.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

import java.util.Iterator;
import java.util.List;

public class IPLiveAttribute extends LiveVariableAnalysis {
    List<SootClass> sootClasses=null;
    public IPLiveAttribute (DirectedGraph g, List<SootClass> sootClasses) {
            super(g);
            this.sootClasses=sootClasses;
            doAnalysis();

        }
    public IPLiveAttribute (DirectedGraph g) {
        super(g);
        doAnalysis();
    }
    @Override
    protected void merge(Object in1Value, Object in2Value, Object outValue) {
        FlowSet in1 = (FlowSet) in1Value, in2 = (FlowSet) in2Value, out=(FlowSet) outValue;
        in1.union(in2, out);
    }
    @Override
    protected void flowThrough(Object inValue, Object nodeValue, Object outValue) {
        FlowSet out = (FlowSet) outValue, in = (FlowSet) inValue;
        Unit node = (Unit) nodeValue;
        FlowSet writes = new ArraySparseSet();
        //TODO::::why getUseAndDefBoxes and not DefBoxes only
        for (Object defObj : node.getUseAndDefBoxes()) {
            ValueBox def = (ValueBox) defObj;
            if (def.getValue() instanceof Local) {
                writes.add((Local) def.getValue());
                //modification from live variable started
                //Get defined value, see if it is dataframe and if it is remove all attributes of that frame from liveness
                //String outName=((Local) def.getValue()).getName();
                //System.out.println("Written:" +outName);
            }
        }
        in.difference(writes, out);

        for (ValueBox use : node.getUseBoxes()) {
            //Modified to avoid Pandas APIS
            //if (use.getValue() instanceof Local) out.add((Local) use.getValue());
            if (use.getValue() instanceof Local && ! PandasAPIs.getAPIs().contains(((Local) use.getValue()).getName()) ){
                out.add((Local) use.getValue());
            }
        }

    boolean canKill = true;
    canKill = canKillAttributes(node);
        if (canKill){
        for (Object defObj : node.getDefBoxes()) {
            ValueBox def = (ValueBox) defObj;
            String outName = getDFName(def);

            for (Iterator<Local> itr = out.toList().iterator(); itr.hasNext(); ) {
                //for(Local l:outIt){
                Local l = itr.next();
                Name nameOb = null;
                if (l instanceof Name) {
                    nameOb = (Name) l;
                    //Kill l if parent of l is killed
                    //TODO for groupby, temp fix, work it out later
//                        if (PythonScene.isGroupBy && PythonScene.groupbyStmts.contains(node)) {
//                        } else
                    if( ((Local) def.getValue()) instanceof Name ){
                        if (nameOb.getParent() != null && nameOb.getParent().equals(outName)) {
                            out.remove(l);
                            updateDFCopy(l,out,node);
                        }
                    }//if
                    else if(((Local) def.getValue()) instanceof Subscript){
                        if (nameOb.getParent() != null && nameOb.getParent().equals(outName)) {
                            if(nameOb.id.equals(getSubscriptDFColumnName((Subscript)((Local) def.getValue())))) {
                                out.remove(l);
                            }
                        }

                    }
                }

            }//for Iterator

        }//for defObj
    }//cankill if
        if(sootClasses!=null){
        IPAttributeAnalysis.insertIPLiveAttributes(node,out,sootClasses);
    }

}//flowThrough


    private boolean canKillAttributes(Unit node){
        //TODO improve this to kill other attributes
        if(PythonScene.isGroupBy && PythonScene.groupbyStmts.contains(node)){
            return false;
        }
        if(PythonScene.compareStmtsDFFiltering.contains(node)){
            return false;
        }
        return true;


    }

    private String getDFName(ValueBox def ){
        if( ((Local) def.getValue()) instanceof Subscript){
            IExpr value=((Subscript)(Local) def.getValue()).getValue();
            if(value instanceof Name){
                return ((Name)value).id;
            }
        }
        return ((Local) def.getValue()).getName();


    }


    private String getSubscriptDFColumnName(Subscript subscript){
        IExpr index=subscript.getSlice().getIndex();
        if(index instanceof Str){
            return ((Str) index).getS();
        }
        return null;

    }

    //TODO complete this
    private void updateDFCopy(Local l, FlowSet out, Unit unit){
        if(unit instanceof AssignmentStmtSoot){
            AssignmentStmtSoot assignmentStmtSoot =(AssignmentStmtSoot)unit;
            AssignStmt assignStmt =assignmentStmtSoot.getAssignStmt();
            IExpr dfName=assignStmt.getTargets().get(0);
            IExpr rhs=assignStmt.getRHS();
            if(rhs instanceof Call){

            }


        }
    }

}




//OLD CODE
//    CallGraph cg;
//    public IPLiveAttribute(CallGraph cg) {
//        this.cg=cg;
//        analyze();
//    }

//    private void analyze(){
//        Iterator<Edge> edgeIterator= cg.iterator();
//        while (edgeIterator.hasNext()){
//            Edge edge=edgeIterator.next();
//            SootMethod sootMethod=edge.getSrc().method();
//            JPMethod jpMethod=(JPMethod)sootMethod;
//            CFG cfg ;
//            cfg = new CFG(jpMethod);
//            AttributeLiveVariableAnalysis alva=new AttributeLiveVariableAnalysis(cfg.getUnitGraph());
//
//        }
//        for( )
//        CFG cfg ;
////        cfg = new CFG(method);
//    }
//}

     */
}
