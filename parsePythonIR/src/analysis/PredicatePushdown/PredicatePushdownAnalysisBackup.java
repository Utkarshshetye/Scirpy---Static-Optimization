package analysis.PredicatePushdown;

import analysis.PythonScene;
import cfg.CFG;
import cfg.JPUnitGraph;
import ir.IExpr;
import ir.IStmt;
import ir.JPMethod;
import ir.Operator;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.Stmt.CallExprStmt;
import ir.expr.*;
import ir.internalast.Keyword;
import ir.internalast.Slice;
import soot.PatchingChain;
import soot.Unit;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

import java.util.*;
import java.util.stream.Collectors;

public class PredicatePushdownAnalysisBackup extends BackwardFlowAnalysis<Unit, FlowSet<Predicate>> {

    JPMethod jpMethod;
    PatchingChain<Unit> unitChain;

    // User defined data structures

    // Unit,Predicate mapping for filter statements
    Map<Unit, PredicateModel> unitToPredMapping;

    // For storing, df -> List of Predicates for complex predicates
    Map<String, List<Set<Predicate>>> complexPreds;
    Map<String, FlowSet<Predicate>> simplePreds;
    Map<String, String> dfAliasMap;
    FlowSet<Predicate> killSetForComplexPredicates;

    public PredicatePushdownAnalysisBackup(JPMethod jpMethod) {
        super(new CFG(jpMethod).getUnitGraph());
        this.jpMethod = jpMethod;
        this.unitToPredMapping = new LinkedHashMap<>();
        this.unitChain = jpMethod.getActiveBody().getUnits();
        this.killSetForComplexPredicates = new ArraySparseSet<>();
        this.complexPreds = new LinkedHashMap<>();
        this.dfAliasMap = new HashMap<>();
        this.simplePreds = new LinkedHashMap<>();

        prePass();
        doAnalysis();
    }

    private void prePass() {

        for (Unit u : unitChain) {
//            aliasOperations(u);
            collectPrintDfs(u);
            collectFilterOperations(u);
        }
    }

    private void storeFilterUnitToUsedColumnsMapping(Unit unit, IExpr expr){
        // System.out.println(expr);
        Compare filterComp = (Compare) expr;

        if (filterComp.getLeft() instanceof Attribute) {
            Attribute attribute = (Attribute) filterComp.getLeft();

            String attr = attribute.getAttr();
            String op = filterComp.getOps().get(0).getSymbol();
            String comp = filterComp.getComparators().get(0).toString();

            int initial_line_no = filterComp.getLineno();

            List<String> usedCols = new ArrayList<>();
            usedCols.add(attr);

            // Default destination line is -1
            PredicateModel pred = new PredicateModel(initial_line_no, -1, usedCols);

            unitToPredMapping.putIfAbsent(unit, pred);
        } else {

            Subscript leftExpr = (Subscript) filterComp.getLeft();
            Slice filterSlice = leftExpr.getSlice();
            Constant colExp = (Constant) filterSlice.getIndex();
            String col_used = colExp.getValue();
            List<String> usedCols = new ArrayList<>();
            usedCols.add(col_used);

            int initial_line_no = filterComp.getLineno();

            // Default destination line is -1
            PredicateModel pred = new PredicateModel(initial_line_no, -1, usedCols);

            unitToPredMapping.putIfAbsent(unit, pred);
        }
    }

    private void  aliasOpMapping(Unit unit, FlowSet<Predicate> outSet) {

        if (unit instanceof AssignmentStmtSoot) {
            AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
            AssignStmt assignStmt = as.getAssignStmt();
            IExpr rhs = assignStmt.getRHS();

            if (rhs instanceof Name) {
                String aliasDf = as.getDataFramesDefined().get(0).getName();

                String orgDf = dfAliasMap.get(aliasDf);

                FlowSet<Predicate> predToCopy = simplePreds.getOrDefault(aliasDf,new ArraySparseSet<>());

                if (!predToCopy.isEmpty()){
                    for(Predicate pred : predToCopy){
                        pred.setDf_var(orgDf); // df_var changed to the original source
                        outSet.add(pred);
                    }
                }

                if (complexPreds.containsKey(aliasDf)){
                    complexPreds.put(orgDf, complexPreds.get(aliasDf));
                }
            }
        }
    }

    private void collectPrintDfs(Unit u) {
        if (u instanceof CallExprStmt) {
            CallExprStmt callExprStmt = (CallExprStmt) u;
            IExpr expr = callExprStmt.getCallExpr();

            if (expr instanceof Call) {
                Call callExp = (Call) expr;

                if (callExp.getBaseName().equals("print")) {
                    List<IExpr> callArgs = callExp.getArgs();
                    IExpr param = callArgs.get(0);

                    if (param instanceof Name) {
                        Name paramName = (Name) param;

                        PythonScene.printDfs.add(paramName.getName());
                    }
                }
            }
        }
    }


    private void collectFilterOperations(Unit unit) {
        if (unit instanceof AssignmentStmtSoot){
            AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
            AssignStmt assignStmt = as.getAssignStmt();
            IExpr exp = assignStmt.getRHS();
            List<IExpr> targets = assignStmt.getTargets();

            if (exp instanceof Subscript) {
                Subscript sub = (Subscript) exp;
                Slice filterSlice = sub.getSlice();
                IExpr filterExpr = filterSlice.getIndex();

                // Filter operation
                if (filterExpr instanceof Compare){
                    storeFilterUnitToUsedColumnsMapping(unit, filterExpr);
                }
            }

            else if (exp instanceof Name){
                List<Name> alias = as.getDataFramesDefined();
                List<Name> prev = as.getDataFramesUsed();

                String aliasName = alias.get(0).getName();
                String src = prev.get(0).getName();

                while (dfAliasMap.containsKey(src) && !dfAliasMap.get(src).equals(src)) {
                    src = dfAliasMap.get(src);
                }

                dfAliasMap.put(aliasName, src);
            }
        }
    }

    private boolean isDataSource(Unit unit) {
        if (unit instanceof AssignmentStmtSoot) {
            AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
            AssignStmt assignStmt = as.getAssignStmt();
            IExpr rhs = assignStmt.getRHS();

            if (rhs instanceof Call) {

                Call call = (Call) rhs;
                Attribute srcAttr = (Attribute) call.getFunc();
                String op = srcAttr.getAttr();
                boolean isSupported = op.equals("read_csv") || op.equals("read_parquet");

                if (isSupported){
                    return true;
                }
            }
        }
        return false;
    }

    private void isInRewriter(BinOp binOp) {
        System.out.println(binOp);
    }

    private List<Set<Predicate>> extractFilterFromBinaryExpr(IExpr expr, String df_var, Unit unit, FlowSet<Predicate> genSet, int levels) {

        if (expr instanceof BinOp) {
            BinOp binOp = (BinOp) expr;
            String op = binOp.getOp().getSymbol();

            List<Set<Predicate>> leftGroups = extractFilterFromBinaryExpr(binOp.getLeft(), df_var, unit, genSet, levels+1);

            List<Set<Predicate>> rightGroups = new ArrayList<>();
            IExpr rhtExpr = binOp.getRight();



            if ((op.equals("&")) && rhtExpr instanceof BinOp) {
                // 3rd level
                BinOp rhsBinOp = (BinOp) rhtExpr;
                Operator Op = rhsBinOp.getOp();
                String OpSymbol = Op.getSymbol();

                if (OpSymbol.equals("|")) {
                    rightGroups = leftGroups; // Skip the third level
                }else{
                    // Copy rhs to lhs[Incomplete]
                    rightGroups = extractFilterFromBinaryExpr(binOp.getRight(), df_var, unit, genSet, levels+1);
                }
            }

            else if ((op.equals("|")) && rhtExpr instanceof BinOp) {

                BinOp rhsBinOp = (BinOp) rhtExpr;
                Operator Op = rhsBinOp.getOp();
                String OpSymbol = Op.getSymbol();

                if (OpSymbol.equals("|")) {
                    // Not supported, don't do anything
                } else{
                    rightGroups = extractFilterFromBinaryExpr(binOp.getRight(), df_var, unit, genSet, levels+1);
                }
            }

            else{
                rightGroups = extractFilterFromBinaryExpr(binOp.getRight(), df_var, unit, genSet, levels+1);
            }

            if (op.equals("|")) {
                List<Set<Predicate>> result = new LinkedList<>(leftGroups);
                result.addAll(rightGroups);
                return result;
            } else if (op.equals("&")) {
                List<Set<Predicate>> result = new LinkedList<>();
                for (Set<Predicate> leftGroup : leftGroups) {
                    for (Set<Predicate> rightGroup : rightGroups) {
                        Set<Predicate> merged = new LinkedHashSet<>(leftGroup);
                        merged.addAll(rightGroup);
                        result.add(merged);
                    }
                }
                return result;
            }
        }

        else if (expr instanceof Compare) {
            Compare comp = (Compare) expr;
            IExpr left = comp.getLeft();

            if (left instanceof Subscript) {
                Subscript leftSubscript = (Subscript) left;

                // Extract column name
                Slice colSlice = leftSubscript.getSlice();
                if (colSlice.getIndex() instanceof Constant) {
                    Constant colConst = (Constant) colSlice.getIndex();
                    String filterColumn = colConst.getValue();

                    // Extract operator
                    String operator = comp.getOps().get(0).getSymbol();

                    // Extract comparator value
                    List<IExpr> comparators = comp.getComparators();
                    if (!comparators.isEmpty() && comparators.get(0) instanceof Constant) {
                        Constant valueConst = (Constant) comparators.get(0);
                        String with = valueConst.toString();

                        // Build Predicate
                        Condition cond = new Condition(filterColumn, operator, with);
                        Predicate pred = new Predicate(cond, null, unit, df_var);
                        genSet.add(pred);

                        return Collections.singletonList(Collections.singleton(pred));
                    }
                }
            }

        } else if (expr instanceof Call) {
            Call call = (Call) expr;
            Attribute funcAttr = (Attribute) call.getFunc();
            String func = funcAttr.getAttr();

            if (func.equals("isin")){
                IExpr colExp = funcAttr.getValue();

                if (colExp instanceof Subscript){
                    Subscript colSubscript = (Subscript) colExp;
                    Slice colSlice = colSubscript.getSlice();
                    Condition cond = null;

                    if (colSlice.getIndex() instanceof Constant) {
                        String col = ((Constant) colSlice.getIndex()).getValue();
                        // col, isin, options[List]
                        List<IExpr> options = call.getArgs();

                        if (options instanceof ArrayList){
                            // values passed as a List
                            cond = new Condition(col, func, options.toString());
                        } else {
                            cond = new Condition(col, func, options.toString());
                        }

                        Predicate pred = new Predicate(cond, null, unit, df_var);

                        genSet.add(pred);

                        return Collections.singletonList(Collections.singleton(pred));
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    // TODO:
    private void removeComplexPreds(List<Set<Predicate>> groups, String dfName, FlowSet<Predicate> killSetForComplexPredicates){
        Iterator<Set<Predicate>> groupIterator = groups.iterator();

        while (groupIterator.hasNext()) {
            Set<Predicate> group = groupIterator.next();

            group.removeIf(p -> dfName.equals(p.getDf_var()) && killSetForComplexPredicates.contains(p));

            if (group.isEmpty()) {
                groupIterator.remove();
            }
        }

        //        List<Predicate> preds = new ArrayList<>(killSetForComplexPredicates.toList());
//
//        if (groups != null && !groups.isEmpty()) {
//
//            for (Set<Predicate> group : groups) {
//                group.removeIf(p -> dfName.equals(p.getDf_var()) && preds.contains(p));
//
//                if (group.isEmpty()) {
//                    groups.remove(group);
//                }
//            }
//        }
    }

    private boolean isRowPreserving(AssignmentStmtSoot assignStmt, FlowSet<Predicate> outSet) {
        AssignStmt asStmt = assignStmt.getAssignStmt();
        IExpr rhs = asStmt.getRHS();

        if (rhs instanceof Call){
            Call call = (Call) rhs;
            Attribute funcAttr = (Attribute) call.getFunc();
            String func = funcAttr.getAttr();

            if (PythonScene.nonRowPreservingOps.contains(func)){
                String baseName = call.getBaseName();

                List<Predicate> toRemove = new ArrayList<>();

                for (Predicate pred : outSet) {
                    if (pred.getDf_var().equals(baseName)) {
                        toRemove.add(pred);
                    }
                }

                for (Predicate pred : toRemove) {
                    outSet.remove(pred);
                }

                return false;
            }
        }
        return true;
    }

    private FlowSet<Predicate> getGenSet(Unit unit) {
        FlowSet<Predicate> genSet = new ArraySparseSet<>();

        if (unit instanceof AssignmentStmtSoot){
            AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
            AssignStmt assignStmt = as.getAssignStmt();
            IExpr expr = assignStmt.getRHS();

            // Getting the used dataframe
            if (!expr.getDataFrames().isEmpty()) {
                PythonScene.usedDFs.add(expr.getDataFrames().get(0).getName());
            }

            if (expr instanceof Subscript) {
                Subscript pred = (Subscript) expr;
                Slice predSlice = pred.getSlice();
                IExpr compExpr = predSlice.getIndex();

                if (compExpr instanceof Compare){
                    Compare compExp = (Compare) compExpr;

                    IExpr leftExp = compExp.getLeft();

                    if (leftExp instanceof Subscript){
                        Subscript leftPred = (Subscript) leftExp;
                        String df_var = leftPred.getValue().toString();
                        Unit parentUnit = null;
                        String cond = compExp.toString();

                        // filter column
                        Slice colSlice =  leftPred.getSlice();
                        Constant colOp =  (Constant) colSlice.getIndex();
                        String filterColumn =  colOp.getValue();

                        // operation
                        String ops = ((Compare) compExpr).getOps().get(0).getSymbol();

                        // with
                        List<IExpr> exprs = ((Compare) compExpr).getComparators();
                        Constant constWith = (Constant) exprs.get(0);
                        String with = constWith.toString();

                        Condition conds = new Condition(filterColumn,ops,with);

                        Predicate genPred = new Predicate(conds,parentUnit,unit,df_var);

                        FlowSet<Predicate> genNewPredSet = new ArraySparseSet<>();
                        genNewPredSet.add(genPred);

                        simplePreds.put(df_var, genNewPredSet);
                        genSet.add(genPred);

                    } else if(leftExp instanceof Attribute) {
                        Attribute leftEx =  (Attribute) leftExp;
                        String filter_Column = leftEx.getAttr();
                        String ops = ((Compare) compExpr).getOps().get(0).getSymbol();
                        String df_var = ((Attribute) leftExp).getBaseName();

                        List<IExpr> exprs = ((Compare) compExpr).getComparators();
                        Constant constWith = (Constant) exprs.get(0);
                        String with = constWith.getValue();

                        Condition conds = new Condition(filter_Column,ops,with);

                        Predicate genPred = new Predicate(conds,null,unit,df_var);

                        FlowSet<Predicate> genNewPredSet = new ArraySparseSet<>();
                        genNewPredSet.add(genPred);

                        simplePreds.put(df_var, genNewPredSet);
                        genSet.add(genPred);
                    }
                } else if(compExpr instanceof Call) {
                    Call call = (Call) compExpr;

                    if (call.getFunc() instanceof Attribute) {
                        Attribute funAttr = (Attribute) call.getFunc();
                        String fun = funAttr.getName();

                        if (fun.equals("isin")){
                            Subscript colSubscript = (Subscript) funAttr.getValue();
                            String df_name = colSubscript.getBaseName();
                            Slice colSlice = colSubscript.getSlice();
                            Condition cond = null;

                            if (colSlice.getIndex() instanceof Constant) {
                                String col = ((Constant) colSlice.getIndex()).getValue();
                                List<IExpr> options = call.getArgs();

                                if (options instanceof ArrayList){
                                    cond = new Condition(col, fun, options);
                                }
                            }

                            Predicate new_pred = new Predicate(cond, unit, df_name);
                            genSet.add(new_pred);
                        }

                    }

                } else if(compExpr instanceof BinOp) {
                    List<Name> df = as.getDataFramesUsed();

                    String df_var = df.get(0).getName();

                    List<Set<Predicate>> res = extractFilterFromBinaryExpr(compExpr,df_var, unit, genSet,0);

                    complexPreds.put(df_var,res);
                } else if(compExpr instanceof Subscript || compExpr instanceof UnaryOp){
                    // In the case of unary operation, if filters like df['remote'] is passed
                    // so, rewriting with df['remote'] == True
                    // for ~df['remote'], setting it to False

                    List<OpsType> opsType = new ArrayList<>();
                    IExpr check = null;
                    IExpr leftExp = compExpr;
                    Subscript leftPred = null;

                    if (compExpr instanceof UnaryOp) {
                        // ~ case
                        opsType.add(OpsType.NotEq);
                        UnaryOp op = (UnaryOp) compExpr;

                        leftPred = (Subscript) op.getLeft();

                        check = new Constant(assignStmt.lineno, assignStmt.col_offset, "False", "Boolean");
                    } else {
                        opsType.add(OpsType.Eq);

                        leftPred = (Subscript) leftExp;
                        check = new Constant(assignStmt.lineno, assignStmt.col_offset, "True", "Boolean");
                    }

                    List<IExpr> comparators = new ArrayList<>();
                    Compare newComp = new Compare();

                    comparators.add(check);

                    newComp.setOps(opsType);
                    newComp.setComparators(comparators);
                    newComp.setLeft(compExpr);

                    String df_var = leftPred.getValue().toString();
                    Unit parentUnit = null;
                    String cond = compExpr.toString();

                    // filter column
                    Slice colSlice =  leftPred.getSlice();
                    Constant colOp =  (Constant) colSlice.getIndex();
                    String filterColumn =  colOp.getValue();

                    // operation
                    String ops = newComp.getOps().get(0).getSymbol();

                    // with
                    List<IExpr> exprs = newComp.getComparators();
                    Constant constWith = (Constant) exprs.get(0);
                    String with = constWith.getValue();

                    Condition conds = new Condition(filterColumn,ops,with);

                    Predicate genPred = new Predicate(conds,parentUnit,unit,df_var);

                    FlowSet<Predicate> genNewPredSet = new ArraySparseSet<>();
                    genNewPredSet.add(genPred);

                    simplePreds.put(df_var, genNewPredSet);
                    genSet.add(genPred);

                }
            }

//            else{
//                System.out.println("asdads");
//                Subscript leftPred = (Subscript) leftExp;
//                String df_var = leftPred.getValue().toString();
//                Unit parentUnit = null;
//                String cond = compExp.toString();
//
//                // filter column
//                Slice colSlice =  leftPred.getSlice();
//                Constant colOp =  (Constant) colSlice.getIndex();
//                String filterColumn =  colOp.getValue();
//
//                // operation
//                String ops = ((Compare) compExpr).getOps().get(0).getSymbol();
//
//                // with
//                List<IExpr> exprs = ((Compare) compExpr).getComparators();
//                Constant constWith = (Constant) exprs.get(0);
//                String with = constWith.getValue();
//
//                Condition conds = new Condition(filterColumn,ops,with);
//
//                Predicate genPred = new Predicate(conds,parentUnit,unit,df_var);
//
//                FlowSet<Predicate> genNewPredSet = new ArraySparseSet<>();
//                genNewPredSet.add(genPred);
//
//                simplePreds.put(df_var, genNewPredSet);
//                genSet.add(genPred);
//            }
        }

        return genSet;
    }

    private FlowSet<Predicate> getKillSet(Unit unit, FlowSet<Predicate> outSet) {
        FlowSet<Predicate> killSet = new ArraySparseSet<>();

        if (unit instanceof AssignmentStmtSoot){
            AssignStmt assignStmt = ((AssignmentStmtSoot) unit).getAssignStmt();
            String assignedDf = ((AssignmentStmtSoot) unit).getDataFramesDefined().get(0).getName();
            IExpr target = assignStmt.getTargets().get(0);
            String attr = "";
            Subscript lhsScript = null;

            if (target instanceof Subscript){
                lhsScript = (Subscript) target;
                IExpr exprType = lhsScript.getSlice().getIndex();

                if (exprType instanceof Constant){
                    attr = ((Constant) exprType).getValue();
                }
            }

            boolean isFilter = unitToPredMapping.containsKey(unit);

            if (!isFilter && !isRowPreserving((AssignmentStmtSoot) unit,outSet)){
                for (Predicate pred : outSet) {
                    if (pred.getDf_var().equals(assignedDf)) {
                        killSet.add(pred);
                        killSetForComplexPredicates.add(pred);
                    }
                }
            }else if(!isFilter &&  isRowPreserving((AssignmentStmtSoot) unit,outSet)){
                for (Predicate pred : outSet) {
                    if ((pred.getDf_var().equals(assignedDf) && !dfAliasMap.containsKey(pred.getDf_var())) || (pred.getDf_var().equals(assignedDf) && attr.equals(pred.getCond().getOn()))) {
                        if (lhsScript!=null && lhsScript.getSlice()!=null && lhsScript.getSlice().getIndex()!=null){
                            IExpr predScript = (IExpr) lhsScript.getSlice().getIndex();

                            if (predScript instanceof Constant){
                                Constant col = (Constant) predScript;

                                if (col.getValue().equals(pred.getCond().getOn())) {
                                    killSet.add(pred);

                                    if (simplePreds.containsKey(assignedDf)) {
                                        simplePreds.get(assignedDf).remove(pred);
                                    }

                                    killSetForComplexPredicates.add(pred);
                                }
                            }

                        } else if(!pred.getDf_var().equals(assignedDf)){
                            // Skip, don't add in the killSet
                        }else{
                            killSet.add(pred);
                            killSetForComplexPredicates.add(pred);
                        }
                    } else if(pred.getDf_var().equals(assignedDf)){

                    }
                }
            }
        }

        return killSet;
    }

    @Override
    protected void flowThrough(FlowSet<Predicate> inSet, Unit unit, FlowSet<Predicate> outSet) {

        inSet.copy(outSet);

        FlowSet<Predicate> genSet = getGenSet(unit);
        outSet.union(genSet);

        FlowSet<Predicate> killSet = getKillSet(unit, outSet);
        outSet.difference(killSet);

        // Modify the map which stores dfName to List<Predicates> applied on it

        if (!outSet.isEmpty()) {
            String dfName = "";

            if (!((IStmt) unit).getDataFramesDefined().isEmpty()){
                dfName = ((IStmt) unit).getDataFramesDefined().get(0).getName();
            }

            FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());

            List<Set<Predicate>> pred = complexPreds.get(dfName);

            if (pred!=null && !pred.isEmpty() && !killSetForComplexPredicates.isEmpty()) {
                removeComplexPreds(pred, dfName, killSetForComplexPredicates);
            }

            if (pred != null && !pred.isEmpty()){
                genSet.forEach(predSet::add);
            }
        }

        aliasOpMapping(unit, outSet);
    }

    private void filterSameDfPred(String readCsvDF, FlowSet<Predicate> preds){
        // removeIf is unsupported for FlowSet<Predicate> to List<Predicate> converted List, because it will become Immutable
        // preds.removeIf(t->!t.getDf_var().equals(readCsvDF));

        List<Predicate> mutList = new ArrayList<>(preds.toList());

        mutList.removeIf(t -> !t.getDf_var().isEmpty() && !t.getDf_var().equals(readCsvDF));

        if(!preds.isEmpty()){
            preds.clear();

            if (!mutList.isEmpty()){
                for(Predicate p : mutList){
                    preds.add(p);
                }
            }

        }
    }

    private void readCSVReWriter(Unit u, FlowSet<Predicate> preds) {
        if (u instanceof AssignmentStmtSoot){
            AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) u;
            AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
            IExpr rhsExp = assignStmt.getRHS();

            if (rhsExp instanceof Call) {
                Call readCsvCall = (Call) rhsExp;
                List<Keyword> kws = readCsvCall.getKeywords();

                int lno = readCsvCall.getLineno();
                int col_offset = readCsvCall.getCol_offset();

                Name def_df_name = assignStmt.getDataFramesDefined().get(0);
                String df = def_df_name.getName();

                filterSameDfPred(df, preds);

                Constant filterGrp = null;
                String predicates = "";

                List<Set<Predicate>> complePreds = complexPreds.getOrDefault(df,new ArrayList<>());

                // System.out.println(complePreds);

                if (complePreds!=null && !complePreds.isEmpty()){
                    predicates = complePreds.toString();
                }

                if(!preds.isEmpty()){
                    Set<Predicate> allPredicatesToRemove = new HashSet<>();

                    if (complePreds!=null && !complePreds.isEmpty()){
                        for (Set<Predicate> predicateSet : complePreds) {
                            allPredicatesToRemove.addAll(predicateSet);
                        }

                        for (Predicate p : allPredicatesToRemove) {
                            preds.remove(p);
                        }

                        if (!preds.isEmpty()) {
                            complePreds.add(new HashSet<>(preds.toList()));
                            predicates = complePreds.toString();
                        }
                    } else{
                        predicates = preds.toList().toString();
                    }
                }

                if (predicates.isEmpty()){
                    predicates = "None";
                }

                filterGrp = new Constant(lno,col_offset,predicates);

                kws.add(new Keyword("filters",filterGrp));
                readCsvCall.setKeywords(kws);
            }
        }
    }

    public void insertFilters() {
        JPUnitGraph unitGraph = new CFG(jpMethod).getUnitGraph();

        Unit unit = unitGraph.getHeads().get(0);


        do {
            FlowSet<Predicate> flowNext = getFlowAfter(unit);

            if (isDataSource(unit)) {

                IStmt stmt = (IStmt) unit;

                String df_def = stmt.getDataFramesDefined().get(0).getName();

//                if (!PythonScene.usedDFs.contains(df_def)) {
//                    unitChain.remove(unit);
//                }
//                else{

                readCSVReWriter(unit, flowNext);

                System.out.println("After"+ stmt.getDataFramesDefined().get(0).getName());

                System.out.println(flowNext);

                Set<String> usedDfs = PythonScene.usedDFs;
                FlowSet<Predicate> predToDelete = new ArraySparseSet<>();

                if (!flowNext.isEmpty()) {
                    if (flowNext.size() == 1) {
                        Predicate flow = flowNext.toList().get(0);

                        flowNext.clear();

                        Unit u = flow.getUnit();

                        IExpr rhsExpr = ((AssignmentStmtSoot) u).getAssignStmt().getRHS();

                        if (rhsExpr instanceof Subscript) {
                            ((Subscript) rhsExpr).setSlice(new Slice());

                            IExpr idx = ((Subscript) ((AssignmentStmtSoot) u).getAssignStmt().getRHS()).getSlice().getIndex();

                            if (idx != null) {
                                flow.setUnit(u);
                                flowNext.add(flow);
                            }
                        }
                    }



                    if (flowNext.size() > 1) {

                        unit = unitGraph.getSuccsOf(unit).isEmpty() ? null : unitGraph.getSuccsOf(unit).get(0);
                        continue;
                    }
                }

                unitChain.removeAll(flowNext.toList().stream().map(Predicate::getUnit).collect(Collectors.toList()));
//                }

//                if (!PythonScene.printDfs.contains(df_def)) {


                // Keep only Units from the flowNext where the read_csv rewriting is done, remove others

                //}
            }

            unit = unitGraph.getSuccsOf(unit).isEmpty() ? null : unitGraph.getSuccsOf(unit).get(0);

        } while(unit != null);
    }

    @Override
    protected FlowSet<Predicate> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<Predicate> predicates, FlowSet<Predicate> a1, FlowSet<Predicate> a2) {
        a1.union(a2);
    }

    @Override
    protected void copy(FlowSet<Predicate> src, FlowSet<Predicate> dest) {
        src.copy(dest);
    }
}