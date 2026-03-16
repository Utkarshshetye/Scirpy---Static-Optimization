package analysis.ReadOnly;


import analysis.PythonScene;
import analysis.interprocedural.IPLiveAttribute;
import cfg.CFG;
import cfg.JPUnitGraph;
import ir.*;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.expr.*;
import ir.JPMethod;
import ir.expr.Attribute;
import ir.internalast.Keyword;
import ir.internalast.Slice;
import rewrite.pd1.Pd1Elt;
import soot.*;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

import java.sql.Array;
import java.util.*;

public class ReadOnlyAttributeAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<String>> {

    JPMethod jpMethod;
    CFG cfg;
    Iterator unitIterator;
    List<Pd1Elt> pdLists;
    IPLiveAttribute alva;
    List<SootClass> sootclases;
    FlowSet<String> emptySet;

    public ReadOnlyAttributeAnalysis(JPMethod jpMethod, List<SootClass> sc) {
        super(new CFG(jpMethod).getUnitGraph());
        emptySet = new ArraySparseSet();
        this.cfg = new CFG(jpMethod);
        this.jpMethod = jpMethod;
        this.unitIterator = cfg.getUnitGraph().iterator();
        this.sootclases = sc;
        this.alva = new IPLiveAttribute(cfg.getUnitGraph(), sc);
        pdLists = new ArrayList<>();
        initialIndexMapping();
        doAnalysis();
    }

    public void analyze() {
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            FlowSet<String> flowBefore = getFlowBefore(unit);
            FlowSet<String> flowAfter = getFlowAfter(unit);

            // flowBefore.forEach(PythonScene.updatedColumns::add);
            // flowAfter.forEach(PythonScene.updatedColumns::add);
        }
        
        Set<String> ilocDfs = PythonScene.ilocDFs;
        List<String> keysInMap = new ArrayList<>(PythonScene.ilocDFIdxMap.keySet());

        propogateIndexMapping();
        // for (String key: keysInMap) {
        //    if (!ilocDfs.contains(key)){
        //        PythonScene.ilocDFIdxMap.remove(key);
        //    }
        // }

       // System.out.println(PythonScene.ilocDFs);

        Set<String> initiCols =  new LinkedHashSet<>(PythonScene.initialCols);
        Set<String> updatedCols = new LinkedHashSet<>(PythonScene.updatedColumns);

        initiCols.removeAll(updatedCols);


        System.out.println("Initial Columns Order in CSV: "+PythonScene.initialCols);
        System.out.println("Killed Columns: "+ PythonScene.updatedColumns);
        System.out.println("Read only attribute: "+initiCols);

        System.out.println("Updated Mapping:");

        printMap();
    }

    public void writer() {
        Set<String> ilocDFs =  PythonScene.ilocDFs;

        JPUnitGraph unitGraph = cfg.getUnitGraph();

        Unit unit = unitGraph.getHeads().get(0);

        do {

            if (unit instanceof AssignmentStmtSoot) {
                AssignmentStmtSoot as = (AssignmentStmtSoot) unit;

                if (as.getLeftOp() instanceof Name) {
                    Name name = (Name) as.getLeftOp();

                    if (ilocDFs.contains(name.getName())) {
                        // Consider different operations can be present in rhs, where we can disable the column selection optimization
                        if (as.getRightOp() instanceof Call) {
                            Call call = (Call) as.getRightOp();
                            IExpr funOp = call.getFunc();

                            // Handling read_csv() rewriting
                            if ( funOp.toString().equals(PythonScene.pandasAliasName+ "." + "read_csv")){

                                int lineno = as.getLineno(), col_offset = call.getCol_offset();

                                String dfValue = "False";

                                Constant colSelcn = new Constant(lineno, col_offset, dfValue);

                                Keyword kw = new Keyword();
                                kw.setArg("column_selection");
                                kw.setValue(colSelcn);

                                call.getKeywords().add(kw);
                            }
                        }
                    }
                }
            }

            unit = unitGraph.getSuccsOf(unit).isEmpty() ? null : unitGraph.getSuccsOf(unit).get(0);

        } while(unit != null);

        // System.out.println(PythonScene.ilocDFs);
    }

//    private void updateAssignDFtoDFMap(String df){
//        if (PythonScene.ilocDFs.contains(df) && !PythonScene.ilocDFs.contains(PythonScene.assignDFtoDFMap.get(df))) {
//
//            Set<String> ilocDf = new HashSet<>();
//            ilocDf.addAll(PythonScene.ilocDFs);
//
//            ilocDf.forEach(dfIter -> {
//                List<String> depends = PythonScene.assignDFtoDFsMap.getOrDefault(dfIter, new ArrayList<>());
//
//                if (!depends.isEmpty()) {
//                    PythonScene.ilocDFs.addAll(depends);
//                }
//            });
//
//            // PythonScene.ilocDFs.add(PythonScene.assignDFtoDFMap.get(df));
//            // System.out.println("DF is in iloc operation:: "+ PythonScene.ilocDFs);
//        }
//    }

    private void initialIndexMapping() {
        Set<String> cols = PythonScene.initialCols;
        Map<Integer,Integer> indexMap = new HashMap<>();

        for (int i=0; i<cols.size(); i++) {
            indexMap.put(i, i);
        }

        PythonScene.ilocDFIdxMap.put("df", indexMap);
    }

    private void ilocMapping(String col, List<String> colms) {
        Map<Integer,Integer> indexMap = new HashMap<>();

        colms.forEach(colIter -> {
            int currIndex = colms.indexOf(colIter);
            int oldIndex = new ArrayList<>(PythonScene.initialCols).indexOf(colIter);
            indexMap.put(currIndex, oldIndex);

            PythonScene.ilocDFIdxMap.putIfAbsent(col, indexMap);
        });
    }
    
    private void propogateIndexMapping() {
        List<String> ilocDfs = new ArrayList<>(PythonScene.ilocDFs);
        Map<String, Map<Integer, Integer>> indexMap = PythonScene.ilocDFIdxMap;
        String prevMatch = "";

        for (String src: ilocDfs) {
            if (indexMap.containsKey(src)) {
                prevMatch = src;
            }

            if (!indexMap.containsKey(src)) {
                if (!prevMatch.isEmpty()){
                    Map<Integer, Integer> matchedMap = indexMap.get(prevMatch);
                    PythonScene.ilocDFIdxMap.put(src, matchedMap);
                }
            }
        }
    }

    private void printMap() {

        Map<String, Map<Integer, Integer>> ilocDFIdxMap  = PythonScene.ilocDFIdxMap;

        ReadUtility.printMap(ilocDFIdxMap);
    }

    private FlowSet<String> genHandler(Unit unit) {
        FlowSet<String> genSet = newInitialFlow();

        if (unit instanceof AssignmentStmtSoot){
           AssignmentStmtSoot assignStmt = (AssignmentStmtSoot) unit;
           IExpr rhsExpr = (IExpr) assignStmt.getRightOp();

           if (rhsExpr instanceof BinOp){
               Subscript subscript = (Subscript) ((BinOp) rhsExpr).getLeft();
               Slice colSlice = subscript.getSlice();
               Constant con = (Constant) colSlice.getIndex();
               String readCol = con.getValue();
               genSet.add(readCol);
           }
           else if (rhsExpr instanceof Subscript){
               Subscript subscript = (Subscript) rhsExpr;

               Slice rhsSlice = subscript.getSlice();

               IExpr colExp = rhsSlice.getIndex();

               // constant on rhs is not part of genset
               if(!(colExp instanceof Constant) && !(colExp instanceof Compare)) {

                   String usedCol = colExp.toString();

                   // rhs contains the column selection or join
                   if (colExp instanceof ListComp){
                       usedCol = usedCol.replaceAll("[\\[\\]'\" ]", "");
                       List<String> sliceCols = Arrays.asList(usedCol.split(","));
                       sliceCols.forEach(col -> genSet.add(col.trim()));

                       Name dfName = assignStmt.getDataFramesDefined().get(0);

                       ilocMapping(dfName.toString(), sliceCols);
                   }else{
                       if (!usedCol.isEmpty()) genSet.add(usedCol);
                   }
               }
           }
           else if (rhsExpr instanceof Attribute){
              Attribute attribute = (Attribute) rhsExpr;
              String op = attribute.getAttr();

              if (op.equals("loc") || op.equals("iloc")){
//                      attribute.get
                 String rightIlocDf = attribute.getValue().toString();
                 PythonScene.ilocDFs.add(rightIlocDf);
              }

              Slice slice = attribute.getSlice();
              if (slice!=null){
                  IExpr expr = slice.getIndex();
                  // constant on rhs is not part of genset
                  if ((expr!=null) && !(expr instanceof Constant)){

                      if (expr instanceof ListComp){

                        List<IExpr> exprs = ((ListComp) expr).getElts();
                        int count = 0;

                          System.out.println(PythonScene.ilocDFIdxMap);
                          Map<Integer, Integer> indexMap = new HashMap<>();
                        for (IExpr iexpr: exprs) {
                            if (iexpr instanceof Constant) {
                                Constant constExpr = (Constant) iexpr;

                                // Assume constExpr.value holds a list or array-like structure
                                String tupleStr = constExpr.toString();  // returns "(2,3)"
                                tupleStr = tupleStr.replaceAll("[()\\s]", "");  // remove '(', ')' and spaces

                                String[] parts = tupleStr.split(",");

                                List<Integer> values = new ArrayList<>();
//                                for (String part : parts) {
                                    values.add(Integer.parseInt(parts[0].trim()));
//                                }




//                                for (int i = 0; i < values.size(); i++) {
                                    indexMap.put(count, values.get(0));     // 0 → 2, 1 → 3
                                    //PythonScene.indexMap.put(i, values.get(i));
//                                }

                                // Store the map for use later – assuming you have a key (col or expr ID)
                                 // Use count to give a name



                            }
                            count++;

                        }
                          Name dfName = assignStmt.getDataFramesDefined().get(0);
                          PythonScene.ilocDFIdxMap.put(dfName.toString(), indexMap);

                      }
                      else {
                          genSet.add(((Constant) expr).getValue());
                      }
                  }

                  // slice is there, but index is range
                  if(expr==null){
                      String assignedDf = ((AssignmentStmtSoot) unit).getDataFramesDefined().get(0).toString();

                      PythonScene.ilocDFs.add(assignedDf);
                  }
              }

           }
        }

        return genSet;
    }
//
//    import pandas as pd
//            df = pd.read_csv('/home/utkarsh/Desktop/Data/data.csv')
//    df['age'] = df['age']+1
//            # res1 = df['salary'].avg()
//    df = df[df['age']>30]
//    df = df[df['salary']>10000]
    private FlowSet<String> killHandler(Unit unit) {

        FlowSet<String> killSet = newInitialFlow();

        if (unit instanceof AssignmentStmtSoot) {
            AssignmentStmtSoot assignStmt = (AssignmentStmtSoot) unit;
            AssignStmt assign = assignStmt.getAssignStmt();
            List<IExpr> expr = assign.getTargets();

            if (assignStmt.getLeftOp() instanceof Attribute) {
                Attribute leftOp = (Attribute) assignStmt.getLeftOp();

                Slice slice = leftOp.getSlice();
                IExpr lhexpr = slice.getIndex();

                if (lhexpr instanceof ListComp) {
                    ListComp comp = (ListComp) lhexpr;
                    List<IExpr> exprs = comp.getElts();

                    if (!exprs.isEmpty()) {

                        Attribute attr = (Attribute) expr.get(0);
                        String operation = attr.getAttr();
                        String columnName = ((Constant) exprs.get(1)).getValue();

                        if (operation.equals("loc")) {
                            killSet.add(columnName);
                        }

                        if (operation.equals("iloc")) {
                            List<Name> dfForIloc =  assignStmt.getDataFramesDefined();
                            String defDF = dfForIloc.get(0).toString();
                            PythonScene.ilocDFs.add(defDF);

                            killSet.add(columnName);

                            if (exprs.size() > 1 && exprs.get(1) instanceof Constant) {

                                Constant col = (Constant) exprs.get(1);

                                if (!col.getConsType().equals("String")) {
                                    killSet.remove(columnName);
                                    PythonScene.ilocUpdatedColPos.add(columnName);
                                }
                            }
                        }
                    }
                }
            } else if (assignStmt.getLeftOp() instanceof Subscript) {

                Subscript leftOp = (Subscript) assignStmt.getLeftOp();
                Slice colSlice = leftOp.getSlice();
                IExpr iExpr = colSlice.getIndex();

                if (iExpr instanceof Constant) {
                    String colConst = ((Constant) iExpr).getValue();
                    killSet.add(colConst);
                }
            } else if (assignStmt.getLeftOp() instanceof Name) {

                    if (assignStmt.getRightOp() instanceof Name) {

                    if (PythonScene.ilocDFs.isEmpty()){
                        PythonScene.assignDFtoDFsMap.computeIfAbsent(assignStmt.getRightOp().toString(), k -> new ArrayList<>()).add(assignStmt.getLeftOp().toString());
                    }else{
                        if (!PythonScene.ilocDFs.contains(assignStmt.getRightOp().toString())) {

                        }else{
                            String src = assignStmt.getRightOp().toString();
                            String destn = assignStmt.getLeftOp().toString();
                            
                            PythonScene.ilocDFs.add(assignStmt.getLeftOp().toString());
                        }
                    }
                }
            }
        }

        return killSet;
    }

    @Override
    protected void flowThrough(FlowSet<String> inSet, Unit unit, FlowSet<String> outSet) {

        inSet.copy(outSet);

        FlowSet<String> killSet = killHandler(unit);

        PythonScene.updatedColumns.addAll(killSet.toList());

        FlowSet<String> genSet = genHandler(unit);

        outSet.difference(killSet);

        outSet.union(genSet);
    }

    @Override
    protected FlowSet<String> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<String> in1, FlowSet<String> in2, FlowSet<String> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<String> source, FlowSet<String> dest) {
        source.copy(dest);
    }
}