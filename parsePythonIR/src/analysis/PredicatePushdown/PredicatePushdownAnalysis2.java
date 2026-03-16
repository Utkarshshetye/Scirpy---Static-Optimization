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

public class PredicatePushdownAnalysis2 extends BackwardFlowAnalysis<Unit, FlowSet<Predicate>> {

    JPMethod jpMethod;
    PatchingChain<Unit> unitChain;

    // User defined data structures
    // Unit,Predicate mapping for filter statements
    Map<Unit, PredicateModel> unitToPredMapping;

    // For storing, df -> List of Predicates for complex predicates
    Map<String, List<Set<Predicate>>> complexPreds;
    Map<String, FlowSet<Predicate>> simplePreds;
    Map<String, String> dfAliasMap;

    // Track which predicates have been killed (for complex predicates)
    Set<Predicate> killedPredicates;

    public PredicatePushdownAnalysis2(JPMethod jpMethod) {
        super(new CFG(jpMethod).getUnitGraph());
        this.jpMethod = jpMethod;
        this.unitToPredMapping = new LinkedHashMap<>();
        this.unitChain = jpMethod.getActiveBody().getUnits();
        this.killedPredicates = new HashSet<>();
        this.complexPreds = new LinkedHashMap<>();
        this.dfAliasMap = new HashMap<>();
        this.simplePreds = new LinkedHashMap<>();

        prePass();
        doAnalysis();
    }

    private void prePass() {
        for (Unit u : unitChain) {
            collectPrintDfs(u);
            collectFilterOperations(u);
        }
    }

    private void storeFilterUnitToUsedColumnsMapping(Unit unit, IExpr expr){
        Compare filterComp = (Compare) expr;

        if (filterComp.getLeft() instanceof Attribute) {
            Attribute attribute = (Attribute) filterComp.getLeft();

            String attr = attribute.getAttr();
            int initial_line_no = filterComp.getLineno();

            List<String> usedCols = new ArrayList<>();
            usedCols.add(attr);

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

            PredicateModel pred = new PredicateModel(initial_line_no, -1, usedCols);
            unitToPredMapping.putIfAbsent(unit, pred);
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

                // Follow alias chain to find original source
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
                return op.equals("read_csv") || op.equals("read_parquet");
            }
        }
        return false;
    }

    private List<Set<Predicate>> extractFilterFromBinaryExpr(IExpr expr, String df_var, Unit unit, FlowSet<Predicate> genSet, int levels) {

        if (expr instanceof BinOp) {
            BinOp binOp = (BinOp) expr;
            String op = binOp.getOp().getSymbol();

            List<Set<Predicate>> leftGroups = extractFilterFromBinaryExpr(binOp.getLeft(), df_var, unit, genSet, levels+1);

            List<Set<Predicate>> rightGroups = new ArrayList<>();
            IExpr rhtExpr = binOp.getRight();

            if ((op.equals("&")) && rhtExpr instanceof BinOp) {
                BinOp rhsBinOp = (BinOp) rhtExpr;
                Operator Op = rhsBinOp.getOp();
                String OpSymbol = Op.getSymbol();

                if (OpSymbol.equals("|")) {
                    rightGroups = leftGroups; // Skip the third level
                } else {
                    rightGroups = extractFilterFromBinaryExpr(binOp.getRight(), df_var, unit, genSet, levels+1);
                }
            }
            else if ((op.equals("|")) && rhtExpr instanceof BinOp) {
                BinOp rhsBinOp = (BinOp) rhtExpr;
                Operator Op = rhsBinOp.getOp();
                String OpSymbol = Op.getSymbol();

                if (OpSymbol.equals("|")) {
                    // Not supported, don't do anything
                } else {
                    rightGroups = extractFilterFromBinaryExpr(binOp.getRight(), df_var, unit, genSet, levels+1);
                }
            }
            else {
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

                Slice colSlice = leftSubscript.getSlice();
                if (colSlice.getIndex() instanceof Constant) {
                    Constant colConst = (Constant) colSlice.getIndex();
                    String filterColumn = colConst.getValue();

                    String operator = comp.getOps().get(0).getSymbol();

                    List<IExpr> comparators = comp.getComparators();
                    if (!comparators.isEmpty() && comparators.get(0) instanceof Constant) {
                        Constant valueConst = (Constant) comparators.get(0);
                        String with = valueConst.toString();

                        Condition cond = new Condition(filterColumn, operator, with);
                        Predicate pred = new Predicate(cond, null, unit, df_var);
                        genSet.add(pred);

                        return Collections.singletonList(Collections.singleton(pred));
                    }
                }
            }
        }
        else if (expr instanceof Call) {
            Call call = (Call) expr;
            Attribute funcAttr = (Attribute) call.getFunc();
            String func = funcAttr.getAttr();

            if (func.equals("isin")){
                IExpr colExp = funcAttr.getValue();

                if (colExp instanceof Subscript){
                    Subscript colSubscript = (Subscript) colExp;
                    Slice colSlice = colSubscript.getSlice();

                    if (colSlice.getIndex() instanceof Constant) {
                        String col = ((Constant) colSlice.getIndex()).getValue();
                        List<IExpr> options = call.getArgs();

                        Condition cond = new Condition(col, func, options.toString());
                        Predicate pred = new Predicate(cond, null, unit, df_var);

                        genSet.add(pred);
                        return Collections.singletonList(Collections.singleton(pred));
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    // FIXED: Now properly removes killed predicates from complex predicate groups
    private void removeComplexPreds(List<Set<Predicate>> groups, String dfName, Set<Predicate> killedPreds){
        if (groups == null || groups.isEmpty() || killedPreds.isEmpty()) {
            return;
        }

        Iterator<Set<Predicate>> groupIterator = groups.iterator();

        while (groupIterator.hasNext()) {
            Set<Predicate> group = groupIterator.next();

            // Remove killed predicates from this group
            group.removeIf(p -> dfName.equals(p.getDf_var()) && killedPreds.contains(p));

            // If group is now empty, remove the entire group
            if (group.isEmpty()) {
                groupIterator.remove();
            }
        }
    }

    // FIXED: Doesn't modify outSet, just returns boolean
    private boolean isRowPreserving(AssignmentStmtSoot assignStmt, String dfName) {
        AssignStmt asStmt = assignStmt.getAssignStmt();
        IExpr rhs = asStmt.getRHS();

        if (rhs instanceof Call){
            Call call = (Call) rhs;

            if (call.getFunc() instanceof Attribute) {
                Attribute funcAttr = (Attribute) call.getFunc();
                String func = funcAttr.getAttr();

                // Check if this is a non-row-preserving operation
                if (PythonScene.nonRowPreservingOps.contains(func)){
                    return false;
                }
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

                        String originalDf = df_var;
                        while (dfAliasMap.containsKey(originalDf) && !dfAliasMap.get(originalDf).equals(originalDf)) {
                            originalDf = dfAliasMap.get(originalDf);
                        }

                        Slice colSlice = leftPred.getSlice();
                        Constant colOp = (Constant) colSlice.getIndex();
                        String filterColumn = colOp.getValue();

                        String ops = compExp.getOps().get(0).getSymbol();

                        List<IExpr> exprs = compExp.getComparators();
                        Constant constWith = (Constant) exprs.get(0);
                        String with = constWith.toString();

                        Condition conds = new Condition(filterColumn, ops, with);
                        Predicate genPred = new Predicate(conds, null, unit, originalDf);

                        genSet.add(genPred);

                    } else if(leftExp instanceof Attribute) {
                        Attribute leftEx = (Attribute) leftExp;
                        String filter_Column = leftEx.getAttr();
                        String ops = compExp.getOps().get(0).getSymbol();
                        String df_var = leftEx.getBaseName();

                        String originalDf = df_var;
                        while (dfAliasMap.containsKey(originalDf) && !dfAliasMap.get(originalDf).equals(originalDf)) {
                            originalDf = dfAliasMap.get(originalDf);
                        }

                        List<IExpr> exprs = compExp.getComparators();
                        Constant constWith = (Constant) exprs.get(0);
                        String with = constWith.getValue();

                        Condition conds = new Condition(filter_Column, ops, with);
                        Predicate genPred = new Predicate(conds, null, unit, originalDf);

                        genSet.add(genPred);
                    }
                }
                else if(compExpr instanceof Call) {
                    Call call = (Call) compExpr;

                    if (call.getFunc() instanceof Attribute) {
                        Attribute funAttr = (Attribute) call.getFunc();
                        String fun = funAttr.getName();

                        if (fun.equals("isin")){
                            Subscript colSubscript = (Subscript) funAttr.getValue();
                            String df_name = colSubscript.getBaseName();
                            Slice colSlice = colSubscript.getSlice();

                            String originalDf = df_name;
                            while (dfAliasMap.containsKey(originalDf) && !dfAliasMap.get(originalDf).equals(originalDf)) {
                                originalDf = dfAliasMap.get(originalDf);
                            }

                            if (colSlice.getIndex() instanceof Constant) {
                                String col = ((Constant) colSlice.getIndex()).getValue();
                                List<IExpr> options = call.getArgs();

                                Condition cond = new Condition(col, fun, options.toString());
                                Predicate new_pred = new Predicate(cond, null, unit, originalDf);
                                genSet.add(new_pred);
                            }
                        }
                    }
                }
                else if(compExpr instanceof BinOp) {
                    List<Name> df = as.getDataFramesUsed();
                    String df_var = df.get(0).getName();

                    String originalDf = df_var;
                    while (dfAliasMap.containsKey(originalDf) && !dfAliasMap.get(originalDf).equals(originalDf)) {
                        originalDf = dfAliasMap.get(originalDf);
                    }

                    List<Set<Predicate>> res = extractFilterFromBinaryExpr(compExpr, originalDf, unit, genSet, 0);

                    if (res.size() > 1) {
                        // This BinOp has OR operator - creates multiple groups
                        // Must store in complexPreds immediately (not wait for merge)

                        System.out.println("DEBUG: BinOp with OR creates " + res.size() + " groups");

                        // Check if complexPreds already exists for this df
                        if (complexPreds.containsKey(df_var) && !complexPreds.get(df_var).isEmpty()) {
                            // Already have complex predicates - do Cartesian product
                            List<Set<Predicate>> existingGroups = complexPreds.get(df_var);
                            List<Set<Predicate>> combinedGroups = new ArrayList<>();

                            for (Set<Predicate> newGroup : res) {
                                for (Set<Predicate> existingGroup : existingGroups) {
                                    Set<Predicate> merged = new LinkedHashSet<>();
                                    merged.addAll(newGroup);
                                    merged.addAll(existingGroup);
                                    combinedGroups.add(merged);
                                }
                            }

                            complexPreds.put(df_var, deduplicateGroups(combinedGroups));
                            System.out.println("DEBUG: Combined with existing, now " + complexPreds.get(df_var).size() + " groups");
                        } else {
                            // First complex predicate for this df
                            complexPreds.put(df_var, res);
                            System.out.println("DEBUG: Stored " + res.size() + " new groups in complexPreds");
                        }

                        // Remove from genSet since they're in complexPreds
                        Set<Predicate> toRemove = new HashSet<>();
                        for (Set<Predicate> group : res) {
                            toRemove.addAll(group);
                        }
                        for (Predicate p : toRemove) {
                            genSet.remove(p);
                        }
                    } else {
                        // BinOp with only AND - single group, treat as simple filter
                        System.out.println("DEBUG: BinOp with only AND - treating as simple filter");
                        // Predicates already in genSet, let them flow backward
                    }

//                    if (complexPreds.containsKey(df_var) && !complexPreds.get(df_var).isEmpty()) {
//                        // Complex predicates already exist - we need to MERGE, not replace!
//                        // This happens when both branches of if/else have BinOp filters
//
//                        List<Set<Predicate>> existingGroups = complexPreds.get(df_var);
//                        List<Set<Predicate>> newGroups = res;
//
//                        System.out.println("DEBUG: BinOp filter for df=" + df_var + " which already has " +
//                                existingGroups.size() + " complex groups");
//                        System.out.println("  Existing groups: " + existingGroups.stream()
//                                .map(g -> g.stream().map(p -> getConditionKey(p.getCond()))
//                                        .collect(java.util.stream.Collectors.toList()))
//                                .collect(java.util.stream.Collectors.toList()));
//                        System.out.println("  New groups: " + newGroups.stream()
//                                .map(g -> g.stream().map(p -> getConditionKey(p.getCond()))
//                                        .collect(java.util.stream.Collectors.toList()))
//                                .collect(java.util.stream.Collectors.toList()));
//
//                        // Add new groups to existing groups (OR semantics)
//                        List<Set<Predicate>> mergedGroups = new ArrayList<>(existingGroups);
//                        mergedGroups.addAll(newGroups);
//
//                        // Deduplicate groups
//                        mergedGroups = deduplicateGroups(mergedGroups);
//
//                        complexPreds.put(df_var, mergedGroups);
//
//                        System.out.println("  Merged result: " + mergedGroups.size() + " groups");
//                    } else {
//                        // No existing complex predicates - just store the new ones
//                        complexPreds.put(df_var, res);
//                    }

//                    Set<Predicate> toRemove = new HashSet<>();
//                    for (Set<Predicate> group : res) {
//                        toRemove.addAll(group);
//                    }
//                    for (Predicate p : toRemove) {
//                        genSet.remove(p);
//                    }
//
//                    System.out.println("DEBUG: Removed " + toRemove.size() + " predicates from genSet (they're in complexPreds)");

//                    System.out.println("DEBUG: BinOp generated " + res.size() + " group(s), genSet size: " + genSet.size());
                }
                else if(compExpr instanceof Subscript || compExpr instanceof UnaryOp){
                    // Handle unary operations like df['remote'] or ~df['remote']

                    List<OpsType> opsType = new ArrayList<>();
                    IExpr check = null;
                    Subscript leftPred = null;

                    if (compExpr instanceof UnaryOp) {
                        // ~ case (negation)
                        opsType.add(OpsType.NotEq);
                        UnaryOp op = (UnaryOp) compExpr;
                        leftPred = (Subscript) op.getLeft();
                        check = new Constant(assignStmt.lineno, assignStmt.col_offset, "False", "Boolean");
                    } else {
                        // Direct boolean column reference
                        opsType.add(OpsType.Eq);
                        leftPred = (Subscript) compExpr;
                        check = new Constant(assignStmt.lineno, assignStmt.col_offset, "True", "Boolean");
                    }

                    String df_var = leftPred.getValue().toString();
                    String originalDf = df_var;
                    while (dfAliasMap.containsKey(originalDf) && !dfAliasMap.get(originalDf).equals(originalDf)) {
                        originalDf = dfAliasMap.get(originalDf);
                    }

                    Slice colSlice = leftPred.getSlice();
                    Constant colOp = (Constant) colSlice.getIndex();
                    String filterColumn = colOp.getValue();

                    String ops = opsType.get(0).getSymbol();
                    String with = ((Constant) check).getValue();

                    Condition conds = new Condition(filterColumn, ops, with);
                    Predicate genPred = new Predicate(conds, null, unit, originalDf);

                    genSet.add(genPred);
                }
            }
        }

        return genSet;
    }

    // FIXED: Clearer logic, doesn't modify outSet
    private FlowSet<Predicate> getKillSet(Unit unit, FlowSet<Predicate> outSet) {
        FlowSet<Predicate> killSet = new ArraySparseSet<>();

        if (!(unit instanceof AssignmentStmtSoot)){
            return killSet;
        }

        AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = as.getAssignStmt();

        List<Name> definedDfs = as.getDataFramesDefined();
        if (definedDfs.isEmpty()) {
            return killSet;
        }

        String assignedDf = definedDfs.get(0).getName();
        IExpr target = assignStmt.getTargets().get(0);

        List<Name> usedDfs = as.getDataFramesUsed();
        String sourceDf = usedDfs.isEmpty() ? assignedDf : usedDfs.get(0).getName();

        String originalSourceDf = sourceDf;
        while (dfAliasMap.containsKey(originalSourceDf) &&
                !dfAliasMap.get(originalSourceDf).equals(originalSourceDf)) {
            originalSourceDf = dfAliasMap.get(originalSourceDf);
        }

        String modifiedColumn = null;
        if (target instanceof Subscript){
            Subscript lhsScript = (Subscript) target;
            IExpr exprType = lhsScript.getSlice().getIndex();

            if (exprType instanceof Constant){
                modifiedColumn = ((Constant) exprType).getValue();
            }
        }

        boolean isFilter = unitToPredMapping.containsKey(unit);
        boolean isRowPreserving = isRowPreserving(as, assignedDf);

        // Case 1: Non-row-preserving operation kills all predicates on this dataframe
        if (!isFilter && !isRowPreserving) {
            for (Predicate pred : outSet) {
                if (pred.getDf_var().equals(originalSourceDf)) {
                    killSet.add(pred);
                }
            }
        }
        // Case 2: Row-preserving but column-modifying operation
        else if (!isFilter && isRowPreserving && modifiedColumn != null) {
            for (Predicate pred : outSet) {
                // Kill predicates on the same dataframe and same column
                if (pred.getDf_var().equals(originalSourceDf) &&
                        modifiedColumn.equals(pred.getCond().getOn())) {
                    killSet.add(pred);
                }
            }
        }
        // Case 3: Alias assignment (df2 = df1) - handled in flowThrough

        return killSet;
    }

    // FIXED: Creates new Predicate objects instead of mutating
    private void handleAliasOperation(Unit unit, FlowSet<Predicate> outSet) {
        if (!(unit instanceof AssignmentStmtSoot)) {
            return;
        }

        AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = as.getAssignStmt();
        IExpr rhs = assignStmt.getRHS();

        // Check if this is an alias assignment: df2 = df1
        if (rhs instanceof Name) {
            List<Name> defined = as.getDataFramesDefined();
            List<Name> used = as.getDataFramesUsed();

            if (defined.isEmpty() || used.isEmpty()) {
                return;
            }

            String aliasDf = defined.get(0).getName();
            String sourceDf = used.get(0).getName();

            // Follow alias chain to find original source
            String orgDf = sourceDf;
            while (dfAliasMap.containsKey(orgDf) && !dfAliasMap.get(orgDf).equals(orgDf)) {
                orgDf = dfAliasMap.get(orgDf);
            }

            // Copy predicates from source to alias, creating NEW predicate objects
            for (Predicate pred : outSet) {
                if (pred.getDf_var().equals(sourceDf)) {
                    // Create new predicate with updated df_var
                    Predicate newPred = new Predicate(
                            pred.getCond(),
                            pred.getParentUnit(),
                            pred.getUnit(),
                            aliasDf  // Use alias name
                    );
                    outSet.add(newPred);
                }
            }

            // Handle complex predicates
            if (complexPreds.containsKey(sourceDf)) {
                List<Set<Predicate>> sourceComplexPreds = complexPreds.get(sourceDf);
                List<Set<Predicate>> newComplexPreds = new ArrayList<>();

                for (Set<Predicate> group : sourceComplexPreds) {
                    Set<Predicate> newGroup = new LinkedHashSet<>();
                    for (Predicate pred : group) {
                        Predicate newPred = new Predicate(
                                pred.getCond(),
                                pred.getParentUnit(),
                                pred.getUnit(),
                                aliasDf
                        );
                        newGroup.add(newPred);
                    }
                    newComplexPreds.add(newGroup);
                }

                complexPreds.put(aliasDf, newComplexPreds);
            }
        }
    }

    // Helper to get condition string representation
    // Since Condition.toString() is not overridden, we need to build it from fields
    private String getConditionKey(Condition cond) {
        // Format: (column, operator, value)
        return "(" + cond.getOn() + ", " + cond.getOp() + ", " + cond.getWith() + ")";
    }

    // Helper method to deduplicate predicates based on their conditions
    // Since Predicate.equals() is not properly implemented, we use condition strings as keys
    private Set<Predicate> deduplicatePredicates(Collection<Predicate> predicates) {
        Map<String, Predicate> dedupMap = new LinkedHashMap<>();
        for (Predicate p : predicates) {
            String key = p.getDf_var() + ":" + getConditionKey(p.getCond());
            System.out.println("DEBUG deduplicatePredicates: key=" + key);
            if (dedupMap.containsKey(key)) {
                System.out.println("  DUPLICATE FOUND! Skipping...");
            }
            dedupMap.put(key, p);
        }
        System.out.println("DEBUG deduplicatePredicates: Input size=" + predicates.size() + ", Output size=" + dedupMap.size());
        return new LinkedHashSet<>(dedupMap.values());
    }

    // Helper to deduplicate a list of predicate groups
    private List<Set<Predicate>> deduplicateGroups(List<Set<Predicate>> groups) {
        // First deduplicate within each group
        List<Set<Predicate>> dedupGroups = new ArrayList<>();
        Set<String> seenGroupSignatures = new HashSet<>();

        for (Set<Predicate> group : groups) {
            // Deduplicate predicates within this group
            Set<Predicate> dedupGroup = deduplicatePredicates(group);

            // Create a signature for this group (sorted list of condition keys)
            List<String> conditionKeys = dedupGroup.stream()
                    .map(p -> getConditionKey(p.getCond()))
                    .sorted()
                    .collect(Collectors.toList());
            String groupSignature = conditionKeys.toString();

            // Only add this group if we haven't seen this exact combination before
            if (!seenGroupSignatures.contains(groupSignature)) {
                seenGroupSignatures.add(groupSignature);
                dedupGroups.add(dedupGroup);
                System.out.println("DEBUG deduplicateGroups: Added unique group with signature: " + groupSignature);
            } else {
                System.out.println("DEBUG deduplicateGroups: SKIPPED duplicate group with signature: " + groupSignature);
            }
        }

        System.out.println("DEBUG deduplicateGroups: Input=" + groups.size() + " groups, Output=" + dedupGroups.size() + " unique groups");
        return dedupGroups;
    }

    @Override
    protected void flowThrough(FlowSet<Predicate> inSet, Unit unit, FlowSet<Predicate> outSet) {
        // Standard backward dataflow equations: out = (in - kill) ∪ gen
        // For backward analysis:
        // - inSet contains predicates flowing backward from successors
        // - outSet will contain predicates flowing backward to predecessors
        //
        // The framework automatically handles if/else branches:
        // - When a unit has multiple successors (if/else), the framework calls merge()
        // - merge() unions the flows from both branches
        // - This gives us: "predicates needed if we take true branch OR false branch"

        System.out.println("flowThrough: " + unit.getClass().getSimpleName());
        // Step 1: Copy input to output
        inSet.copy(outSet);

        // Step 2: Add generated predicates
        FlowSet<Predicate> genSet = getGenSet(unit);
        outSet.union(genSet);

        // Step 3: Remove killed predicates
        FlowSet<Predicate> killSet = getKillSet(unit, outSet);
        outSet.difference(killSet);

        // Track killed predicates for complex predicate cleanup
        for (Predicate pred : killSet) {
            killedPredicates.add(pred);
        }

        // Step 4: Handle alias operations (after main flow equations)
        handleAliasOperation(unit, outSet);

        // Step 5: Update side data structures for later use
        updateSideDataStructures(unit, outSet, genSet, killSet);

        System.out.println("  After flowThrough, complexPreds state:");
        for (String dfKey : complexPreds.keySet()) {
            List<Set<Predicate>> groups = complexPreds.get(dfKey);
            System.out.println("    df=" + dfKey + ", groups=" + groups.size());
            for (int i = 0; i < groups.size(); i++) {
                System.out.print("      Group " + i + ": [");
                int j = 0;
                for (Predicate p : groups.get(i)) {
                    if (j > 0) System.out.print(", ");
                    System.out.print(getConditionKey(p.getCond()));
                    j++;
                }
                System.out.println("]");
            }
        }
    }

    private void updateSideDataStructures(Unit unit, FlowSet<Predicate> outSet,
                                          FlowSet<Predicate> genSet, FlowSet<Predicate> killSet) {
        System.out.println("DEBUG updateSideDataStructures: Called for " + unit.getClass().getSimpleName());

        if (!(unit instanceof AssignmentStmtSoot)) {
//            System.out.println("  Not AssignmentStmtSoot, skipping");
            return;
        }

        AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
        List<Name> definedDfs = as.getDataFramesDefined();

        if (definedDfs.isEmpty()) {
            return;
        }

        String dfName = definedDfs.get(0).getName();

        if (!genSet.isEmpty()) {
            List<Name> usedDfs = as.getDataFramesUsed();
            String sourceDf =  usedDfs.isEmpty() ? dfName : usedDfs.get(0).getName();
            String originalDf = sourceDf;
            while (dfAliasMap.containsKey(originalDf) && !dfAliasMap.get(originalDf).equals(originalDf)) {
                originalDf = dfAliasMap.get(originalDf);
            }

            // Check if complex predicates exist for the SOURCE dataframe
            boolean hasComplexPreds = complexPreds.containsKey(originalDf) &&
                    !complexPreds.get(sourceDf).isEmpty();

            System.out.println("  hasComplexPreds for '" + originalDf + "': " + hasComplexPreds);

            if (hasComplexPreds) {
                System.out.println("  Existing complex groups:");
                List<Set<Predicate>> groups = complexPreds.get(originalDf);
                for (int i = 0; i < groups.size(); i++) {
                    System.out.println("    Group " + i + ": " + groups.get(i).stream()
                            .map(p -> getConditionKey(p.getCond()))
                            .collect(Collectors.toList()));
                }

                // Check if any predicate in genSet already exists in any complex group
                boolean predicateExistsInGroups = false;

                for (Predicate genPred : genSet) {
                    String genKey = getConditionKey(genPred.getCond());

                    for (Set<Predicate> group : groups) {
                        for (Predicate groupPred : group) {
                            String groupKey = getConditionKey(groupPred.getCond());
                            if (genKey.equals(groupKey)) {
                                predicateExistsInGroups = true;
                                break;
                            }
                        }
                        if (predicateExistsInGroups) break;
                    }
                    if (predicateExistsInGroups) break;
                }

                System.out.println("  predicateExistsInGroups: " + predicateExistsInGroups);

                if (predicateExistsInGroups) {
                    // These predicates are FROM an if/else branch
                    // Let merge() handle them - just add to simplePreds
                    System.out.println("  -> Predicates from if/else branch - adding to simplePreds");
                    FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());
                    for (Predicate pred : genSet) {
                        predSet.add(pred);
                    }
                } else {
                    // These predicates are AFTER the if/else
                    // Add to ALL complex groups
                    System.out.println("  -> Predicates after if/else - adding to ALL groups");

                    List<Set<Predicate>> existingGroups = complexPreds.get(originalDf);
                    List<Set<Predicate>> updatedGroups = new ArrayList<>();

                    for (Set<Predicate> group : existingGroups) {
                        Set<Predicate> newGroup = new LinkedHashSet<>(group);

                        // Add the new predicates
                        for (Predicate pred : genSet) {
                            newGroup.add(pred);
                        }

                        updatedGroups.add(newGroup);
                    }

                    complexPreds.put(originalDf, deduplicateGroups(updatedGroups));

                    System.out.println("  Updated complex groups:");
                    for (int i = 0; i < updatedGroups.size(); i++) {
                        System.out.println("    Group " + i + ": " + updatedGroups.get(i).stream()
                                .map(p -> getConditionKey(p.getCond()))
                                .collect(Collectors.toList()));
                    }

                    // Remove from outSet so they don't flow as simple predicates
                    for (Predicate pred : genSet) {
                        outSet.remove(pred);
                    }
                }
            } else {
                // Normal case: Add to simplePreds
                System.out.println("  -> No complex preds yet - adding to simplePreds");
                FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());
                for (Predicate pred : genSet) {
                    predSet.add(pred);
                }
            }
        } else {
            System.out.println("  genSet is empty, nothing to add");
        }

        // Remove killed predicates from simple predicates
        if (!killSet.isEmpty() && simplePreds.containsKey(dfName)) {
            FlowSet<Predicate> predSet = simplePreds.get(dfName);
            for (Predicate pred : killSet) {
                predSet.remove(pred);
            }
        }
    }

//    private void updateSideDataStructures(Unit unit, FlowSet<Predicate> outSet,
//                                          FlowSet<Predicate> genSet, FlowSet<Predicate> killSet) {
//        if (!(unit instanceof AssignmentStmtSoot)) {
//            return;
//        }
//
//        AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
//        List<Name> definedDfs = as.getDataFramesDefined();
//
//        if (definedDfs.isEmpty()) {
//            return;
//        }
//
//        String dfName = definedDfs.get(0).getName();
//
//        // CRITICAL FIX: Check if the USED dataframe has complex predicates
//        // Not the defined dataframe!
//        List<Name> usedDfs = as.getDataFramesUsed();
//
//        if (!genSet.isEmpty() && !usedDfs.isEmpty()) {
//            // Get the source dataframe being filtered
//            String sourceDf = usedDfs.get(0).getName();
//
//            // Check if SOURCE df has complex predicates
//            boolean sourceHasComplexPreds = complexPreds.containsKey(sourceDf) &&
//                    !complexPreds.get(sourceDf).isEmpty();
//
//            if (sourceHasComplexPreds) {
//                // The predicates in genSet are for the source df
//                // Add them to ALL complex groups of the source df
//
//                System.out.println("DEBUG updateSideDataStructures: Simple predicates generated for source df=" + sourceDf + " which has complex predicates");

//                List<Set<Predicate>> existingGroups = complexPreds.get(sourceDf);
//                List<Set<Predicate>> updatedGroups = new ArrayList<>();
//
//                for (Set<Predicate> group : existingGroups) {
//                    // Create a map for deduplication
//                    Map<String, Predicate> groupMap = new LinkedHashMap<>();
//
//                    // Add existing predicates from this group
//                    for (Predicate p : group) {
//                        String key = getConditionKey(p.getCond());
//                        groupMap.put(key, p);
//                    }
//
//                    // Add new simple predicates (with deduplication)
//                    for (Predicate genPred : genSet) {
//                        String key = getConditionKey(genPred.getCond());
//                        if (!groupMap.containsKey(key)) {
//                            groupMap.put(key, genPred);
//                            System.out.println("  Adding simple predicate " + key + " to complex group for " + sourceDf);
//                        }
//                    }
//
//                    Set<Predicate> newGroup = new LinkedHashSet<>(groupMap.values());
//                    updatedGroups.add(newGroup);
//                }
//
//                // Update complex predicates with the enhanced groups
//                complexPreds.put(sourceDf, updatedGroups);
//
//                System.out.println("DEBUG: Updated complex predicates for " + sourceDf + ":");
//                for (int i = 0; i < updatedGroups.size(); i++) {
//                    System.out.println("  Group " + i + ": " + updatedGroups.get(i).stream()
//                            .map(p -> getConditionKey(p.getCond()))
//                            .collect(java.util.stream.Collectors.toList()));
//                }
//
//                // DON'T add to simplePreds for source df - they're now in complex predicates
//
//                // Remove from outSet so they don't flow as simple predicates
//                for (Predicate genPred : genSet) {
//                    outSet.remove(genPred);
//                }
//
//                return;  // Exit early
//            }
//        }

    // Normal case: No complex predicates exist for source df, so add to simplePreds
//        if (!genSet.isEmpty()) {
//            FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());
//            for (Predicate pred : genSet) {
//                predSet.add(pred);
//            }
//        }
//
//        if (!genSet.isEmpty()) {
//            // Check if complex predicates already exist for this df
//            boolean hasComplexPreds = complexPreds.containsKey(dfName) &&
//                    !complexPreds.get(dfName).isEmpty();
//
//            if (hasComplexPreds) {
//                // Check if any predicate in genSet already exists in any complex group
//                boolean predicateExistsInGroups = false;
//                List<Set<Predicate>> groups = complexPreds.get(dfName);
//
//                for (Predicate genPred : genSet) {
//                    String genKey = getConditionKey(genPred.getCond());
//
//                    for (Set<Predicate> group : groups) {
//                        for (Predicate groupPred : group) {
//                            String groupKey = getConditionKey(groupPred.getCond());
//                            if (genKey.equals(groupKey)) {
//                                predicateExistsInGroups = true;
//                                break;
//                            }
//                        }
//                        if (predicateExistsInGroups) break;
//                    }
//                    if (predicateExistsInGroups) break;
//                }
//
//                if (predicateExistsInGroups) {
//                    // These predicates are FROM an if/else branch
//                    // Let merge() handle them - just add to simplePreds
//                    System.out.println("DEBUG: Predicates from if/else branch - letting merge() handle");
//                    FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());
//                    for (Predicate pred : genSet) {
//                        predSet.add(pred);
//                    }
//                } else {
//                    // These predicates are AFTER the if/else
//                    // Add to ALL complex groups
//                    System.out.println("DEBUG: Predicates after if/else - adding to all groups");
//
//                    List<Set<Predicate>> existingGroups = complexPreds.get(dfName);
//                    List<Set<Predicate>> updatedGroups = new ArrayList<>();
//
//                    for (Set<Predicate> group : existingGroups) {
//                        Set<Predicate> newGroup = new LinkedHashSet<>(group);
//
//                        // Add the new predicates
//                        for (Predicate pred : genSet) {
//                            newGroup.add(pred);
//                        }
//
//                        updatedGroups.add(newGroup);
//                    }
//
//                    complexPreds.put(dfName, deduplicateGroups(updatedGroups));
//
//                    System.out.println("DEBUG: Updated complex groups:");
//                    for (int i = 0; i < updatedGroups.size(); i++) {
//                        System.out.println("  Group " + i + ": " + updatedGroups.get(i).stream()
//                                .map(p -> getConditionKey(p.getCond()))
//                                .collect(java.util.stream.Collectors.toList()));
//                    }
//
//                    // Remove from outSet so they don't flow as simple predicates
//                    for (Predicate pred : genSet) {
//                        outSet.remove(pred);
//                    }
//                }
//            } else {
//                // Normal case: Add to simplePreds
//                FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());
//                for (Predicate pred : genSet) {
//                    predSet.add(pred);
//                }
//            }
//        }
//
//
//        // Remove killed predicates from simple predicates
//        if (!killSet.isEmpty() && simplePreds.containsKey(dfName)) {
//            FlowSet<Predicate> predSet = simplePreds.get(dfName);
//            for (Predicate pred : killSet) {
//                predSet.remove(pred);
//            }
//        }
//    }

//    // FIXED: Separated side-effect updates from main dataflow
//    private void updateSideDataStructures(Unit unit, FlowSet<Predicate> outSet,
//                                          FlowSet<Predicate> genSet, FlowSet<Predicate> killSet) {
//        if (!(unit instanceof AssignmentStmtSoot)) {
//            return;
//        }
//
//        AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
//        List<Name> definedDfs = as.getDataFramesDefined();
//
//        if (definedDfs.isEmpty()) {
//            return;
//        }
//
//        String dfName = definedDfs.get(0).getName();
//
//        // CRITICAL: Check if this df already has complex predicates
//        boolean hasComplexPreds = complexPreds.containsKey(dfName) && !complexPreds.get(dfName).isEmpty();
//
//        if (hasComplexPreds && !genSet.isEmpty()) {
//            // Case: Simple predicates generated AFTER complex predicates already exist
//            // We need to add these simple predicates to ALL existing complex groups
//
//            System.out.println("DEBUG updateSideDataStructures: Simple predicates generated for df=" + dfName + " which already has complex predicates");
//
//            List<Set<Predicate>> existingGroups = complexPreds.get(dfName);
//            List<Set<Predicate>> updatedGroups = new ArrayList<>();
//
//            for (Set<Predicate> group : existingGroups) {
//                // Create a map for deduplication
//                Map<String, Predicate> groupMap = new LinkedHashMap<>();
//
//                // Add existing predicates from this group
//                for (Predicate p : group) {
//                    String key = getConditionKey(p.getCond());
//                    groupMap.put(key, p);
//                }
//
//                // Add new simple predicates (with deduplication)
//                for (Predicate genPred : genSet) {
//                    String key = getConditionKey(genPred.getCond());
//                    if (!groupMap.containsKey(key)) {
//                        groupMap.put(key, genPred);
//                        System.out.println("  Adding simple predicate " + key + " to complex group");
//                    }
//                }
//
//                Set<Predicate> newGroup = new LinkedHashSet<>(groupMap.values());
//                updatedGroups.add(newGroup);
//            }
//
//            // Update complex predicates with the enhanced groups
//            complexPreds.put(dfName, updatedGroups);
//
//            System.out.println("DEBUG: Updated complex predicates for " + dfName + ":");
//            for (int i = 0; i < updatedGroups.size(); i++) {
//                System.out.println("  Group " + i + ": " + updatedGroups.get(i).stream()
//                        .map(p -> getConditionKey(p.getCond()))
//                        .collect(java.util.stream.Collectors.toList()));
//            }
//
//            // DON'T add to simplePreds - they're now in complex predicates
//            // DON'T add to outSet - they're in complex predicates
//
//            // Remove from outSet so they don't flow as simple predicates
//            for (Predicate genPred : genSet) {
//                outSet.remove(genPred);
//            }
//
//            return;  // Exit early - don't execute the normal simple predicate logic below
//        }
//
//        // Normal case: No complex predicates exist yet, so add to simplePreds
//        if (!genSet.isEmpty()) {
//            FlowSet<Predicate> predSet = simplePreds.computeIfAbsent(dfName, k -> new ArraySparseSet<>());
//            for (Predicate pred : genSet) {
//                predSet.add(pred);
//            }
//        }
//
//        // Remove killed predicates from simple predicates
//        if (!killSet.isEmpty() && simplePreds.containsKey(dfName)) {
//            FlowSet<Predicate> predSet = simplePreds.get(dfName);
//            for (Predicate pred : killSet) {
//                predSet.remove(pred);
//            }
//        }
//
//        // Clean up complex predicates (you have this commented out, but it might be needed)
//        // List<Set<Predicate>> complexPredList = complexPreds.get(dfName);
//        // if (complexPredList != null && !complexPredList.isEmpty() && !killedPredicates.isEmpty()) {
//        //     removeComplexPreds(complexPredList, dfName, killedPredicates);
//        // }
//    }

    private void filterSameDfPred(String readCsvDF, FlowSet<Predicate> preds){
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

//    private void readCSVReWriter(Unit u, FlowSet<Predicate> preds) {
//        if (!(u instanceof AssignmentStmtSoot)){
//            return;
//        }
//
//        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) u;
//        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
//        IExpr rhsExp = assignStmt.getRHS();
//
//        if (!(rhsExp instanceof Call)) {
//            return;
//        }
//
//        Call readCsvCall = (Call) rhsExp;
//        List<Keyword> kws = readCsvCall.getKeywords();
//
//        int lno = readCsvCall.getLineno();
//        int col_offset = readCsvCall.getCol_offset();
//
//        Name def_df_name = assignStmt.getDataFramesDefined().get(0);
//        String df = def_df_name.getName();
//
//        filterSameDfPred(df, preds);
//
//        List<Set<Predicate>> complePreds = complexPreds.get(df);
//
//        String predicates = "None";
//
//        // Combine simple predicates with complex predicates
//        if (complePreds != null && !complePreds.isEmpty()) {
//            // If we have complex predicates (OR groups), we need to add simple predicates to each group
//            // This implements: (simple1 AND simple2 AND ...) AND ((group1) OR (group2) OR ...)
//            // Which becomes: (simple1 AND simple2 AND group1) OR (simple1 AND simple2 AND group2)
//
//            List<Set<Predicate>> mergedGroups = new ArrayList<>();
//            List<Predicate> simplePredsList = preds.toList();
//
//            for (Set<Predicate> group : complePreds) {
//                Set<Predicate> combined = new LinkedHashSet<>();
//                combined.addAll(group);
//                combined.addAll(simplePredsList);
//
//                // Deduplicate the merged group
//                Set<Predicate> mergedGroup = deduplicatePredicates(combined);
//                mergedGroups.add(mergedGroup);
//            }
//
//            // Convert to string using ONLY conditions, not full Predicate.toString()
//            StringBuilder sb = new StringBuilder("[");
//            for (int i = 0; i < mergedGroups.size(); i++) {
//                if (i > 0) sb.append(", ");
//                sb.append("[");
//                int j = 0;
//                for (Predicate p : mergedGroups.get(i)) {
//                    if (j > 0) sb.append(", ");
//                    sb.append(getConditionKey(p.getCond()));  // Use proper condition string
//                    j++;
//                }
//                sb.append("]");
//            }
//            sb.append("]");
//            predicates = sb.toString();
//        }
//        else if (!preds.isEmpty()) {
//            // Only simple predicates, no complex ones - deduplicate them too
//            Set<Predicate> dedupPreds = deduplicatePredicates(preds.toList());
//
//            // Convert to string using ONLY conditions
//            StringBuilder sb = new StringBuilder("[");
//            int i = 0;
//            for (Predicate p : dedupPreds) {
//                if (i > 0) sb.append(", ");
//                sb.append(getConditionKey(p.getCond()));  // Use proper condition string
//                i++;
//            }
//            sb.append("]");
//            predicates = sb.toString();
//        }
//
//        Constant filterGrp = new Constant(lno, col_offset, predicates);
//        kws.add(new Keyword("filters", filterGrp));
//        readCsvCall.setKeywords(kws);
//    }

    private void readCSVReWriter(Unit u, FlowSet<Predicate> preds) {
        if (!(u instanceof AssignmentStmtSoot)){
            return;
        }

        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) u;
        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
        IExpr rhsExp = assignStmt.getRHS();

        if (!(rhsExp instanceof Call)) {
            return;
        }

        Call readCsvCall = (Call) rhsExp;
        List<Keyword> kws = readCsvCall.getKeywords();

        int lno = readCsvCall.getLineno();
        int col_offset = readCsvCall.getCol_offset();

        Name def_df_name = assignStmt.getDataFramesDefined().get(0);
        String df = def_df_name.getName();

        filterSameDfPred(df, preds);

        List<Set<Predicate>> complePreds = complexPreds.get(df);

        String predicates = "None";

        // Check if we have complex predicates (OR groups from if/else)
        if (complePreds != null && !complePreds.isEmpty()) {
            // USE COMPLEX PREDICATES AS-IS - they already have the correct structure!
            // Don't merge with simple predicates!

            System.out.println("DEBUG readCSVReWriter: Using complex predicates for df=" + df);
            for (int i = 0; i < complePreds.size(); i++) {
                System.out.println("  Complex Group " + i + ": " + complePreds.get(i).stream()
                        .map(p -> getConditionKey(p.getCond()))
                        .collect(Collectors.toList()));
            }

            // Convert to string using ONLY conditions
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < complePreds.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("[");
                int j = 0;
                for (Predicate p : complePreds.get(i)) {
                    if (j > 0) sb.append(", ");
                    sb.append(getConditionKey(p.getCond()));
                    j++;
                }
                sb.append("]");
            }
            sb.append("]");
            predicates = sb.toString();
        }
        else if (!preds.isEmpty()) {
            // Only simple predicates, no complex ones - use them
            Set<Predicate> dedupPreds = deduplicatePredicates(preds.toList());

            System.out.println("DEBUG readCSVReWriter: Using simple predicates for df=" + df);

            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (Predicate p : dedupPreds) {
                if (i > 0) sb.append(", ");
                sb.append(getConditionKey(p.getCond()));
                i++;
            }
            sb.append("]");
            predicates = sb.toString();
        }

        Constant filterGrp = new Constant(lno, col_offset, predicates);
        kws.add(new Keyword("filters", filterGrp));
        readCsvCall.setKeywords(kws);
    }

    public void insertFilters() {
        JPUnitGraph unitGraph = new CFG(jpMethod).getUnitGraph();
        Unit unit = unitGraph.getHeads().get(0);

        do {
            FlowSet<Predicate> flowNext = getFlowAfter(unit);

            if (isDataSource(unit)) {
                IStmt stmt = (IStmt) unit;
                String df_def = stmt.getDataFramesDefined().get(0).getName();

                readCSVReWriter(unit, flowNext);

                System.out.println("After " + df_def);
                System.out.println(flowNext);

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
            }

            unit = unitGraph.getSuccsOf(unit).isEmpty() ? null : unitGraph.getSuccsOf(unit).get(0);

        } while(unit != null);
    }

    @Override
    protected FlowSet<Predicate> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    private static int mergeCallCount = 0;

    @Override
    protected void merge(FlowSet<Predicate> in1, FlowSet<Predicate> in2, FlowSet<Predicate> out) {
        mergeCallCount++;
        System.out.println("========== MERGE CALLED (Call #" + mergeCallCount + ") ==========");

        List<Predicate> list1 = in1.toList();
        List<Predicate> list2 = in2.toList();

        if (list1.isEmpty() && list2.isEmpty()) {
            in1.union(in2, out);
            return;
        }

        // Group predicates by dataframe and condition
        Map<String, Map<String, Predicate>> branch1Map = new HashMap<>();
        Map<String, Map<String, Predicate>> branch2Map = new HashMap<>();

        for (Predicate p : list1) {
            String df = p.getDf_var();
            String key = getConditionKey(p.getCond());
            branch1Map.computeIfAbsent(df, k -> new HashMap<>()).put(key, p);
        }

        for (Predicate p : list2) {
            String df = p.getDf_var();
            String key = getConditionKey(p.getCond());
            branch2Map.computeIfAbsent(df, k -> new HashMap<>()).put(key, p);
        }

        Set<String> allDfs = new HashSet<>();
        allDfs.addAll(branch1Map.keySet());
        allDfs.addAll(branch2Map.keySet());

        for (String df : allDfs) {
            Map<String, Predicate> preds1 = branch1Map.getOrDefault(df, new HashMap<>());
            Map<String, Predicate> preds2 = branch2Map.getOrDefault(df, new HashMap<>());

//            if (complexPreds.containsKey(df) && !complexPreds.get(df).isEmpty()) {
//                Set<String> existingKeys = new HashSet<>();
//                for (Set<Predicate> group : complexPreds.get(df)) {
//                    for (Predicate p : group) {
//                        existingKeys.add(getConditionKey(p.getCond()));
//                    }
//                }
//
//                // Remove existing predicate keys from both branches
//                preds1.keySet().removeAll(existingKeys);
//                preds2.keySet().removeAll(existingKeys);
//
//                System.out.println("  Removed " + existingKeys.size() + " existing complex pred keys from branches");
//            }

            System.out.println("DEBUG merge for df=" + df);
            System.out.println("  Branch1 keys: " + preds1.keySet());
            System.out.println("  Branch2 keys: " + preds2.keySet());

            // Find common predicates
            Set<String> commonKeys = new HashSet<>(preds1.keySet());
            commonKeys.retainAll(preds2.keySet());

            Set<String> branch1UniqueKeys = new HashSet<>(preds1.keySet());
            branch1UniqueKeys.removeAll(commonKeys);

            Set<String> branch2UniqueKeys = new HashSet<>(preds2.keySet());
            branch2UniqueKeys.removeAll(commonKeys);

            System.out.println("  Common: " + commonKeys);
            System.out.println("  Branch1 unique: " + branch1UniqueKeys);
            System.out.println("  Branch2 unique: " + branch2UniqueKeys);

            // If no unique predicates, everything is common - skip
            if (branch1UniqueKeys.isEmpty() && branch2UniqueKeys.isEmpty()) {
                System.out.println("  -> All predicates are common - no OR groups needed");
                continue;
            }

            // Collect predicates
            Set<Predicate> commonPreds = new LinkedHashSet<>();
            for (String key : commonKeys) {
                commonPreds.add(preds1.get(key));
            }

            Set<Predicate> branch1UniquePreds = new LinkedHashSet<>();
            for (String key : branch1UniqueKeys) {
                branch1UniquePreds.add(preds1.get(key));
            }

            Set<Predicate> branch2UniquePreds = new LinkedHashSet<>();
            for (String key : branch2UniqueKeys) {
                branch2UniquePreds.add(preds2.get(key));
            }

            // Create new groups for THIS if/else
            List<Set<Predicate>> newGroups = new ArrayList<>();

            if (!branch1UniqueKeys.isEmpty()) {
                Set<Predicate> group1 = new LinkedHashSet<>();
                group1.addAll(commonPreds);
                group1.addAll(branch1UniquePreds);
                newGroups.add(group1);
            }

            if (!branch2UniqueKeys.isEmpty()) {
                Set<Predicate> group2 = new LinkedHashSet<>();
                group2.addAll(commonPreds);
                group2.addAll(branch2UniquePreds);
                newGroups.add(group2);
            }

            System.out.println("  New groups from current if/else:");
            for (int i = 0; i < newGroups.size(); i++) {
                System.out.println("    Group " + i + ": " + newGroups.get(i).stream()
                        .map(p -> getConditionKey(p.getCond()))
                        .collect(Collectors.toList()));
            }

            List<Set<Predicate>> existingGroups = complexPreds.get(df);
            List<Set<Predicate>> finalGroups;

            if (existingGroups != null && !existingGroups.isEmpty()) {
                System.out.println("  Existing groups:");
                for (int i = 0; i < existingGroups.size(); i++) {
                    System.out.println("    Existing " + i + ": " + existingGroups.get(i).stream()
                            .map(p -> getConditionKey(p.getCond()))
                            .collect(Collectors.toList()));
                }

                // Check if identical (duplicate merge call)
                if (areGroupSetsEqual(newGroups, existingGroups)) {
                    System.out.println("  -> DUPLICATE merge - keeping existing groups");
                    finalGroups = existingGroups;
                } else {
                    // Do Cartesian product: (newGroup1 OR newGroup2) AND (existingGroup1 OR existingGroup2)
                    System.out.println("  -> DIFFERENT groups - doing Cartesian product");
                    List<Set<Predicate>> combinedGroups = new ArrayList<>();

                    for (Set<Predicate> newGroup : newGroups) {
                        for (Set<Predicate> existingGroup : existingGroups) {
                            Set<Predicate> merged = new LinkedHashSet<>();
                            merged.addAll(newGroup);
                            merged.addAll(existingGroup);
                            combinedGroups.add(merged);

                            System.out.println("     Cartesian: " + newGroup.stream()
                                    .map(p -> getConditionKey(p.getCond()))
                                    .collect(Collectors.toList()) +
                                    " × " + existingGroup.stream()
                                    .map(p -> getConditionKey(p.getCond()))
                                    .collect(Collectors.toList()) +
                                    " = " + merged.stream()
                                    .map(p -> getConditionKey(p.getCond()))
                                    .collect(Collectors.toList()));
                        }
                    }

                    finalGroups = deduplicateGroups(combinedGroups);

                    System.out.println("  Final groups after deduplication:");
                    for (int i = 0; i < finalGroups.size(); i++) {
                        System.out.println("    Final " + i + ": " + finalGroups.get(i).stream()
                                .map(p -> getConditionKey(p.getCond()))
                                .collect(Collectors.toList()));
                    }
                }
            } else {
                System.out.println("  -> No existing groups - first if/else for this df");
                finalGroups = newGroups;
            }

            // Store the result
            complexPreds.put(df, finalGroups);
            System.out.println("  Stored " + finalGroups.size() + " groups for df=" + df);
        }

        // Union for normal backward flow
        FlowSet<Predicate> tempUnion = new ArraySparseSet<>();
        in1.union(in2, tempUnion);

        // Don't flow predicates that are in complex groups
        Set<String> dfsWithComplexPreds = new HashSet<>();
        for (String df : allDfs) {
            if (complexPreds.containsKey(df) && !complexPreds.get(df).isEmpty()) {
                dfsWithComplexPreds.add(df);
            }
        }

        for (Predicate p : tempUnion) {
            if (!dfsWithComplexPreds.contains(p.getDf_var())) {
                out.add(p);
            }
        }

        System.out.println("========== END MERGE #" + mergeCallCount + " ==========\n");
    }

    private boolean areGroupSetsEqual(List<Set<Predicate>> groups1, List<Set<Predicate>> groups2) {
        if (groups1.size() != groups2.size()) {
            return false;
        }

        Set<String> signatures1 = new HashSet<>();
        for (Set<Predicate> group : groups1) {
            List<String> conditionKeys = group.stream()
                    .map(p -> getConditionKey(p.getCond()))
                    .sorted()
                    .collect(Collectors.toList());
            signatures1.add(conditionKeys.toString());
        }

        Set<String> signatures2 = new HashSet<>();
        for (Set<Predicate> group : groups2) {
            List<String> conditionKeys = group.stream()
                    .map(p -> getConditionKey(p.getCond()))
                    .sorted()
                    .collect(Collectors.toList());
            signatures2.add(conditionKeys.toString());
        }

        return signatures1.equals(signatures2);
    }

//    @Override
//    protected void merge(FlowSet<Predicate> in1, FlowSet<Predicate> in2, FlowSet<Predicate> out) {
//        mergeCallCount++;
//        System.out.println("========== MERGE CALLED (Call #" + mergeCallCount + ") ==========");
//        System.out.println("  in1 size: " + in1.size());
//        System.out.println("  in2 size: " + in2.size());
//
//        List<Predicate> list1 = in1.toList();
//        List<Predicate> list2 = in2.toList();
//
//        if (list1.isEmpty() && list2.isEmpty()) {
//            in1.union(in2, out);
//            return;
//        }
//
//        // Group predicates by dataframe and condition
//        Map<String, Map<String, Predicate>> branch1Map = new HashMap<>();
//        Map<String, Map<String, Predicate>> branch2Map = new HashMap<>();
//
//        for (Predicate p : list1) {
//            String df = p.getDf_var();
//            String key = getConditionKey(p.getCond());
//            branch1Map.computeIfAbsent(df, k -> new HashMap<>()).put(key, p);
//        }
//
//        for (Predicate p : list2) {
//            String df = p.getDf_var();
//            String key = getConditionKey(p.getCond());
//            branch2Map.computeIfAbsent(df, k -> new HashMap<>()).put(key, p);
//        }
//
//        Set<String> allDfs = new HashSet<>();
//        allDfs.addAll(branch1Map.keySet());
//        allDfs.addAll(branch2Map.keySet());
//
//        for (String df : allDfs) {
//            Map<String, Predicate> preds1 = branch1Map.getOrDefault(df, new HashMap<>());
//            Map<String, Predicate> preds2 = branch2Map.getOrDefault(df, new HashMap<>());
//
//            System.out.println("DEBUG merge for df=" + df);
//            System.out.println("  Branch1 keys: " + preds1.keySet());
//            System.out.println("  Branch2 keys: " + preds2.keySet());
//
//            // CRITICAL: Make DEEP COPY of existing groups FIRST
//            List<Set<Predicate>> existingGroups = complexPreds.get(df);
//            if (existingGroups != null && !existingGroups.isEmpty()) {
//                List<Set<Predicate>> copiedGroups = new ArrayList<>();
//                for (Set<Predicate> group : existingGroups) {
//                    Set<Predicate> copiedGroup = new LinkedHashSet<>(group);
//                    copiedGroups.add(copiedGroup);
//                }
//                existingGroups = copiedGroups;
//            }
//
//            // Get inherited predicate keys from existing groups
//            Set<String> inheritedKeys = new HashSet<>();
//            if (existingGroups != null && !existingGroups.isEmpty()) {
//                for (Set<Predicate> group : existingGroups) {
//                    for (Predicate p : group) {
//                        inheritedKeys.add(getConditionKey(p.getCond()));
//                    }
//                }
//            }
//
//            System.out.println("  Inherited keys from existing groups: " + inheritedKeys);
//
//            // Find common predicates in CURRENT if/else
//            Set<String> commonKeys = new HashSet<>(preds1.keySet());
//            commonKeys.retainAll(preds2.keySet());
//
//            // IMPORTANT: Use ALL common keys (including inherited) for new groups
//            // Common predicates should appear in ALL final groups
//            Set<String> localCommonKeys = new HashSet<>(commonKeys);
//
//            Set<String> branch1UniqueKeys = new HashSet<>(preds1.keySet());
//            branch1UniqueKeys.removeAll(commonKeys);
//
//            Set<String> branch2UniqueKeys = new HashSet<>(preds2.keySet());
//            branch2UniqueKeys.removeAll(commonKeys);
//
//            System.out.println("  Common keys: " + commonKeys);
//            System.out.println("  Branch1 unique keys: " + branch1UniqueKeys);
//            System.out.println("  Branch2 unique keys: " + branch2UniqueKeys);
//
//            // If no unique predicates in either branch, everything is common - skip
//            if (branch1UniqueKeys.isEmpty() && branch2UniqueKeys.isEmpty()) {
//                System.out.println("DEBUG: All predicates are common - skipping (no OR groups needed)");
//                continue;
//            }
//
//            // Collect predicates - use ALL common (including inherited)
//            Set<Predicate> localCommonPreds = new LinkedHashSet<>();
//            for (String key : localCommonKeys) {
//                localCommonPreds.add(preds1.get(key));
//            }
//
//            Set<Predicate> branch1UniquePreds = new LinkedHashSet<>();
//            for (String key : branch1UniqueKeys) {
//                branch1UniquePreds.add(preds1.get(key));
//            }
//
//
//            Set<Predicate> branch2UniquePreds = new LinkedHashSet<>();
//            for (String key : branch2UniqueKeys) {
//                branch2UniquePreds.add(preds2.get(key));
//            }
//
//            // ADDED: Get simple predicates for this dataframe
//            Set<Predicate> simplePredsForDf = new LinkedHashSet<>();
//            FlowSet<Predicate> simpleFlow = simplePreds.get(df);
//            if (simpleFlow != null && !simpleFlow.isEmpty()) {
//                for (Predicate p : simpleFlow) {
//                    simplePredsForDf.add(p);
//                }
//                System.out.println("  Found " + simplePredsForDf.size() + " simple predicates to add to ALL groups:");
//                for (Predicate p : simplePredsForDf) {
//                    System.out.println("    " + getConditionKey(p.getCond()));
//                }
//            }
//
//            // Create OR groups for current if/else using ALL common
//            List<Set<Predicate>> newGroups = new ArrayList<>();
//
//            if (!branch1UniqueKeys.isEmpty()) {
//                Map<String, Predicate> group1Map = new LinkedHashMap<>();
//                for (Predicate p : localCommonPreds) {
//                    group1Map.put(getConditionKey(p.getCond()), p);
//                }
//                // ADDED: Add simple predicates
//                for (Predicate p : simplePredsForDf) {
//                    group1Map.put(getConditionKey(p.getCond()), p);
//                }
//                for (Predicate p : branch1UniquePreds) {
//                    group1Map.put(getConditionKey(p.getCond()), p);
//                }
//                newGroups.add(new LinkedHashSet<>(group1Map.values()));
//            }
//
//            if (!branch2UniqueKeys.isEmpty()) {
//                Map<String, Predicate> group2Map = new LinkedHashMap<>();
//                for (Predicate p : localCommonPreds) {
//                    group2Map.put(getConditionKey(p.getCond()), p);
//                }
//                // ADDED: Add simple predicates
//                for (Predicate p : simplePredsForDf) {
//                    group2Map.put(getConditionKey(p.getCond()), p);
//                }
//                for (Predicate p : branch2UniquePreds) {
//                    group2Map.put(getConditionKey(p.getCond()), p);
//                }
//                newGroups.add(new LinkedHashSet<>(group2Map.values()));
//            }
//
//
//
//            System.out.println("  New groups created:");
//            for (int i = 0; i < newGroups.size(); i++) {
//                System.out.println("    New Group " + i + ": " + newGroups.get(i).stream()
//                        .map(p -> getConditionKey(p.getCond()))
//                        .collect(java.util.stream.Collectors.toList()));
//            }
//
//            List<Set<Predicate>> finalGroups;
//
//            if (existingGroups != null && !existingGroups.isEmpty()) {
//                System.out.println("DEBUG: Found " + existingGroups.size() + " existing groups");
//                for (int i = 0; i < existingGroups.size(); i++) {
//                    System.out.println("  Existing Group " + i + ": " + existingGroups.get(i).stream()
//                            .map(p -> getConditionKey(p.getCond()))
//                            .collect(java.util.stream.Collectors.toList()));
//                }
//
//                // Check if new groups already contain ALL inherited predicates
//                boolean newGroupsHaveAllInherited = true;
//                for (Set<Predicate> newGroup : newGroups) {
//                    Set<String> newGroupKeys = newGroup.stream()
//                            .map(p -> getConditionKey(p.getCond()))
//                            .collect(java.util.stream.Collectors.toSet());
//
//                    if (!newGroupKeys.containsAll(inheritedKeys)) {
//                        newGroupsHaveAllInherited = false;
//                        System.out.println("  New group " + newGroupKeys + " is missing some inherited keys");
//                        break;
//                    }
//                }
//
//                if (newGroupsHaveAllInherited && existingGroups.size() == 1) {
//                    // New groups already contain all inherited predicates
//                    // AND there's only ONE existing group (not an OR relationship)
//                    // This means new groups are complete - use as-is!
//                    System.out.println("DEBUG: New groups already contain all inherited from single existing group - using as-is");
//                    finalGroups = deduplicateGroups(newGroups);
//                    complexPreds.put(df, finalGroups);
//                    // ADDED: Clear simple predicates
//                    if (simplePreds.containsKey(df)) {
//                        System.out.println("DEBUG: Clearing simple predicates for df=" + df);
//                        simplePreds.get(df).clear();
//                    }
//                } else if (newGroupsHaveAllInherited && existingGroups.size() > 1) {
//                    // New groups have all inherited, BUT inherited come from MULTIPLE existing groups (OR relationship)
//                    // Example: inherited {salary>40000, age>30} comes from [{salary>40000}, {age>30}]
//                    // Need Cartesian product to preserve OR semantics
//                    System.out.println("DEBUG: Inherited predicates from multiple existing groups (OR) - need Cartesian product");
//                    System.out.println("  Rebuilding new groups WITHOUT inherited common to avoid duplication");
//
//                    // Simply use ALL common predicates from current if/else
//                    // Even if they're inherited - they're still required by current filters
//                    List<Set<Predicate>> newGroupsForCartesian = newGroups;  // Use the original new groups with all common
//
//                    System.out.println("  Using new groups with all common (including inherited):");
//                    for (int i = 0; i < newGroupsForCartesian.size(); i++) {
//                        System.out.println("    Group " + i + ": " + newGroupsForCartesian.get(i).stream()
//                                .map(p -> getConditionKey(p.getCond()))
//                                .collect(java.util.stream.Collectors.toList()));
//                    }
//
//                    // Do Cartesian product - but DON'T add existing groups if their predicates are already in new groups!
//                    List<Set<Predicate>> combinedGroups = new ArrayList<>();
//
//                    for (Set<Predicate> newGroup : newGroupsForCartesian) {
//                        // Check which existing groups have predicates NOT already in newGroup
//                        for (Set<Predicate> existingGroup : existingGroups) {
//                            Set<String> newGroupKeys = newGroup.stream()
//                                    .map(p -> getConditionKey(p.getCond()))
//                                    .collect(java.util.stream.Collectors.toSet());
//
//                            Set<String> existingKeys = existingGroup.stream()
//                                    .map(p -> getConditionKey(p.getCond()))
//                                    .collect(java.util.stream.Collectors.toSet());
//
//                            // Only combine if existing group has predicates not in new group
//                            boolean hasNewPredicates = false;
//                            for (String existingKey : existingKeys) {
//                                if (!newGroupKeys.contains(existingKey)) {
//                                    hasNewPredicates = true;
//                                    break;
//                                }
//                            }
//
//                            if (hasNewPredicates || existingKeys.isEmpty()) {
//                                Map<String, Predicate> mergedMap = new LinkedHashMap<>();
//
//                                for (Predicate p : newGroup) {
//                                    mergedMap.put(getConditionKey(p.getCond()), p);
//                                }
//
//                                for (Predicate p : existingGroup) {
//                                    mergedMap.put(getConditionKey(p.getCond()), p);
//                                }
//
//                                Set<Predicate> merged = new LinkedHashSet<>(mergedMap.values());
//                                combinedGroups.add(merged);
//
//                                System.out.println("  Combined: " + merged.stream()
//                                        .map(p -> getConditionKey(p.getCond()))
//                                        .collect(java.util.stream.Collectors.toList()));
//                            } else {
//                                // New group already has all predicates from this existing group
//                                // Just use new group as-is
//                                combinedGroups.add(new LinkedHashSet<>(newGroup));
//                                System.out.println("  Using new group as-is (already contains existing): " + newGroup.stream()
//                                        .map(p -> getConditionKey(p.getCond()))
//                                        .collect(java.util.stream.Collectors.toList()));
//                            }
//                        }
//                    }
//
//                    finalGroups = deduplicateGroups(combinedGroups);
//                    complexPreds.put(df, finalGroups);
//                    // ADDED: Clear simple predicates
//                    if (simplePreds.containsKey(df)) {
//                        System.out.println("DEBUG: Clearing simple predicates for df=" + df);
//                        simplePreds.get(df).clear();
//                    }
//                } else {
//                    // New groups don't have all inherited predicates
//                    // Check if this is nested if/else or multiple merges of same if/else
//
//                    // Build expected groups from current if/else
//                    Set<String> expectedKeys1 = new HashSet<>(commonKeys);
//                    expectedKeys1.addAll(branch1UniqueKeys);
//
//                    Set<String> expectedKeys2 = new HashSet<>(commonKeys);
//                    expectedKeys2.addAll(branch2UniqueKeys);
//
//                    System.out.println("  Expected group 1 from current if/else: " + expectedKeys1);
//                    System.out.println("  Expected group 2 from current if/else: " + expectedKeys2);
//
//                    boolean isNestedIfElse = false;
//                    for (Set<Predicate> existingGroup : existingGroups) {
//                        Set<String> existingKeys = existingGroup.stream()
//                                .map(p -> getConditionKey(p.getCond()))
//                                .collect(java.util.stream.Collectors.toSet());
//
//                        if (!existingKeys.equals(expectedKeys1) && !existingKeys.equals(expectedKeys2)) {
//                            isNestedIfElse = true;
//                            System.out.println("  Existing group " + existingKeys + " doesn't match expected - this is nested!");
//                            break;
//                        }
//                    }
//
//                    if (isNestedIfElse) {
//                        // TRUE NESTED CASE: Combine using Cartesian product
//                        System.out.println("DEBUG: True nested if/else - combining with Cartesian product");
//
//                        List<Set<Predicate>> combinedGroups = new ArrayList<>();
//
//                        for (Set<Predicate> newGroup : newGroups) {
//                            for (Set<Predicate> existingGroup : existingGroups) {
//                                Map<String, Predicate> mergedMap = new LinkedHashMap<>();
//
//                                for (Predicate p : newGroup) {
//                                    mergedMap.put(getConditionKey(p.getCond()), p);
//                                }
//
//                                for (Predicate p : existingGroup) {
//                                    mergedMap.put(getConditionKey(p.getCond()), p);
//                                }
//
//                                Set<Predicate> merged = new LinkedHashSet<>(mergedMap.values());
//                                combinedGroups.add(merged);
//
//                                System.out.println("  Combined: " + merged.stream()
//                                        .map(p -> getConditionKey(p.getCond()))
//                                        .collect(java.util.stream.Collectors.toList()));
//                            }
//                        }
//
//                        finalGroups = deduplicateGroups(combinedGroups);
//                        complexPreds.put(df, finalGroups);
//                        // ADDED: Clear simple predicates
//                        if (simplePreds.containsKey(df)) {
//                            System.out.println("DEBUG: Clearing simple predicates for df=" + df);
//                            simplePreds.get(df).clear();
//                        }
//                    } else {
//                        // SAME IF/ELSE CASE: Replace
//                        System.out.println("DEBUG: Same if/else (multiple merge calls) - replacing existing groups");
//                        finalGroups = deduplicateGroups(newGroups);
//                        complexPreds.put(df, finalGroups);
//                        // ADDED: Clear simple predicates
//                        if (simplePreds.containsKey(df)) {
//                            System.out.println("DEBUG: Clearing simple predicates for df=" + df);
//                            simplePreds.get(df).clear();
//                        }
//                    }
//                }
//            } else {
//                System.out.println("DEBUG: No existing groups - first if/else");
//                finalGroups = deduplicateGroups(newGroups);
//                complexPreds.put(df, finalGroups);
//                // ADDED: Clear simple predicates
//                if (simplePreds.containsKey(df)) {
//                    System.out.println("DEBUG: Clearing simple predicates for df=" + df);
//                    simplePreds.get(df).clear();
//                }
//            }
//
//            System.out.println("DEBUG: Storing " + finalGroups.size() + " final groups for df=" + df);
//            for (int i = 0; i < finalGroups.size(); i++) {
//                System.out.println("  Group " + i + ": " + finalGroups.get(i).stream()
//                        .map(p -> getConditionKey(p.getCond()))
//                        .collect(java.util.stream.Collectors.toList()));
//            }
//
//            complexPreds.put(df, finalGroups);
//
//            System.out.println("DEBUG: Immediately after storing, complexPreds.get(\"" + df + "\") contains:");
//            List<Set<Predicate>> verification = complexPreds.get(df);
//            if (verification != null) {
//                for (int i = 0; i < verification.size(); i++) {
//                    Set<Predicate> group = verification.get(i);
//                    System.out.print("  Verified Stored Group " + i + ": [");
//                    int j = 0;
//                    for (Predicate p : group) {
//                        if (j > 0) System.out.print(", ");
//                        System.out.print(getConditionKey(p.getCond()));
//                        j++;
//                    }
//                    System.out.println("]");
//                }
//            } else {
//                System.out.println("  NULL!");
//            }
//        }
//
//        // If we created complex predicates for a df, don't flow those predicates as simple
//        Set<String> dfsWithComplexPreds = new HashSet<>();
//        for (String df : allDfs) {
//            if (complexPreds.containsKey(df) && !complexPreds.get(df).isEmpty()) {
//                dfsWithComplexPreds.add(df);
//            }
//        }
//
//        FlowSet<Predicate> tempUnion = new ArraySparseSet<>();
//        in1.union(in2, tempUnion);
//
//        for (Predicate p : tempUnion) {
//            if (!dfsWithComplexPreds.contains(p.getDf_var())) {
//                // This predicate's df doesn't have complex predicates, flow it normally
//                out.add(p);
//            }
//            // If df has complex predicates, don't flow - they're already in complexPreds
//        }
//
//        System.out.println("========== END OF MERGE #" + mergeCallCount + " ==========\n");
//    }

//    @Override
//    protected void merge(FlowSet<Predicate> in1, FlowSet<Predicate> in2, FlowSet<Predicate> out) {
//        System.out.println("========== MERGE CALLED ==========");
//        // CRITICAL: For backward analysis with if/else branches:
//        // - in1 = flow from first successor (e.g., true branch)
//        // - in2 = flow from second successor (e.g., false branch)
//        // - out = merged result
//
//        // Step 1: Convert to lists for comparison
//        List<Predicate> list1 = in1.toList();
//        List<Predicate> list2 = in2.toList();
//
//        // If both branches have no predicates, just return
//        if (list1.isEmpty() && list2.isEmpty()) {
//            in1.union(in2, out);
//            return;
//        }
//
//        // Step 2: Group predicates by dataframe and condition (using condition for comparison)
//        // This handles cases where Predicate.equals() might not be properly implemented
//        // Use condition-based keys to deduplicate from the start
//        Map<String, Map<String, Predicate>> branch1Map = new HashMap<>();
//        Map<String, Map<String, Predicate>> branch2Map = new HashMap<>();
//
//        for (Predicate p : list1) {
//            String df = p.getDf_var();
//            String key = getConditionKey(p.getCond());  // Use proper condition string
//            System.out.println("DEBUG merge branch1: df=" + df + ", key=[" + key + "]");
//            branch1Map.computeIfAbsent(df, k -> new HashMap<>()).put(key, p);
//        }
//
//        for (Predicate p : list2) {
//            String df = p.getDf_var();
//            String key = getConditionKey(p.getCond());  // Use proper condition string
//            System.out.println("DEBUG merge branch2: df=" + df + ", key=[" + key + "]");
//            branch2Map.computeIfAbsent(df, k -> new HashMap<>()).put(key, p);
//        }
//
//        System.out.println("DEBUG merge: branch1Map size=" + branch1Map.values().stream().mapToInt(Map::size).sum());
//        System.out.println("DEBUG merge: branch2Map size=" + branch2Map.values().stream().mapToInt(Map::size).sum());
//
//        // Step 3: For each dataframe, determine common vs branch-specific predicates
//        Set<String> allDfs = new HashSet<>();
//        allDfs.addAll(branch1Map.keySet());
//        allDfs.addAll(branch2Map.keySet());
//
//        for (String df : allDfs) {
//            Map<String, Predicate> preds1 = branch1Map.getOrDefault(df, new HashMap<>());
//            Map<String, Predicate> preds2 = branch2Map.getOrDefault(df, new HashMap<>());
//
//            // Keys are already condition-based, so we can directly compare
//            // Find common predicate keys (intersection)
//            Set<String> commonKeys = new HashSet<>(preds1.keySet());
//            commonKeys.retainAll(preds2.keySet());
//
//            // Find branch-specific keys (difference)
//            Set<String> onlyIn1Keys = new HashSet<>(preds1.keySet());
//            onlyIn1Keys.removeAll(commonKeys);
//
//            Set<String> onlyIn2Keys = new HashSet<>(preds2.keySet());
//            onlyIn2Keys.removeAll(commonKeys);
//
//            // Collect predicates
//            Set<Predicate> commonPreds = new LinkedHashSet<>();
//            for (String key : commonKeys) {
//                commonPreds.add(preds1.get(key));
//            }
//
//            Set<Predicate> branch1Preds = new LinkedHashSet<>();
//            for (String key : onlyIn1Keys) {
//                branch1Preds.add(preds1.get(key));
//            }
//
//            Set<Predicate> branch2Preds = new LinkedHashSet<>();
//            for (String key : onlyIn2Keys) {
//                branch2Preds.add(preds2.get(key));
//            }
//
//            // Create groups based on what we found
//            List<Set<Predicate>> newGroups = new ArrayList<>();
//
//            // Case 1: Only common predicates exist (both branches have same filters)
//            // DON'T create complex predicates - let them flow through as simple predicates
//            // via the UNION at the end
//            if (!commonPreds.isEmpty() && branch1Preds.isEmpty() && branch2Preds.isEmpty()) {
//                // Do nothing - these common predicates will be UNIONed in step 4
//                // and will flow backward as simple predicates
//            }
//            // Case 2: Branches have different predicates
//            // Store as separate OR groups
//            else if (!branch1Preds.isEmpty() || !branch2Preds.isEmpty()) {
//                // Each branch gets its own group (including any common predicates)
//                if (!branch1Preds.isEmpty()) {
//                    // Use condition-based deduplication
//                    Map<String, Predicate> group1Map = new HashMap<>();
//                    for (Predicate p : commonPreds) {
//                        String key = getConditionKey(p.getCond());
//                        group1Map.put(key, p);
//                    }
//                    for (Predicate p : branch1Preds) {
//                        String key = getConditionKey(p.getCond());
//                        group1Map.put(key, p);
//                    }
//                    Set<Predicate> group1 = new LinkedHashSet<>(group1Map.values());
//                    newGroups.add(group1);
//                }
//
//                if (!branch2Preds.isEmpty()) {
//                    // Use condition-based deduplication
//                    Map<String, Predicate> group2Map = new HashMap<>();
//                    for (Predicate p : commonPreds) {
//                        String key = getConditionKey(p.getCond());
//                        group2Map.put(key, p);
//                    }
//                    for (Predicate p : branch2Preds) {
//                        String key = getConditionKey(p.getCond());
//                        group2Map.put(key, p);
//                    }
//                    Set<Predicate> group2 = new LinkedHashSet<>(group2Map.values());
//                    newGroups.add(group2);
//                }
//
//                // Combine with existing complex predicates if they exist
//                if (!newGroups.isEmpty()) {
//                    List<Set<Predicate>> existingGroups = complexPreds.get(df);
//
//                    if (existingGroups != null && !existingGroups.isEmpty()) {
//                        // We have multiple if/else branches for the same dataframe
//                        // Need to combine using Cartesian product (AND semantics)
//                        // (G1 OR G2) AND (G3 OR G4) = (G1 AND G3) OR (G1 AND G4) OR (G2 AND G3) OR (G2 AND G4)
//                        System.out.println("DEBUG merge: Found existing " + existingGroups.size() + " groups:");
//                        for (int i = 0; i < existingGroups.size(); i++) {
//                            System.out.println("  Existing Group " + i + ": " + existingGroups.get(i).stream()
//                                    .map(p -> getConditionKey(p.getCond()))
//                                    .collect(java.util.stream.Collectors.toList()));
//                        }
//                        System.out.println("DEBUG merge: New groups being combined:");
//                        for (int i = 0; i < newGroups.size(); i++) {
//                            System.out.println("  New Group " + i + ": " + newGroups.get(i).stream()
//                                    .map(p -> getConditionKey(p.getCond()))
//                                    .collect(java.util.stream.Collectors.toList()));
//                        }
//
//                        List<Set<Predicate>> combinedGroups = new ArrayList<>();
//
//                        for (Set<Predicate> existingGroup : existingGroups) {
//                            for (Set<Predicate> newGroup : newGroups) {
//                                // Merge groups using condition-based deduplication
//                                // Force deep copy by rebuilding from scratch
//                                Map<String, Predicate> mergedMap = new LinkedHashMap<>();
//
//                                // Add all predicates from existing group
//                                for (Predicate p : existingGroup) {
//                                    String key = getConditionKey(p.getCond());
//                                    mergedMap.put(key, p);
//                                }
//
//                                // Add predicates from new group (automatically deduplicates by condition)
//                                for (Predicate p : newGroup) {
//                                    String key = getConditionKey(p.getCond());
//                                    mergedMap.put(key, p);
//                                }
//
//                                // Convert to new set - force copy
//                                Set<Predicate> merged = new LinkedHashSet<>();
//                                for (Predicate p : mergedMap.values()) {
//                                    merged.add(p);  // Add each predicate individually
//                                }
//
//                                System.out.println("DEBUG: Created merged group with " + merged.size() + " predicates: " +
//                                        merged.stream().map(p -> getConditionKey(p.getCond())).collect(java.util.stream.Collectors.toList()));
//
//                                combinedGroups.add(merged);
//                            }
//                        }
//
//                        System.out.println("DEBUG: Total combined groups before dedup: " + combinedGroups.size());
//
//                        // Final deduplication of all groups
//                        List<Set<Predicate>> finalGroups = deduplicateGroups(combinedGroups);
//                        System.out.println("DEBUG merge: Storing " + finalGroups.size() + " combined groups for df=" + df);
//                        for (int i = 0; i < finalGroups.size(); i++) {
//                            System.out.println("  Group " + i + ": " + finalGroups.get(i).stream()
//                                    .map(p -> getConditionKey(p.getCond()))
//                                    .collect(java.util.stream.Collectors.toList()));
//                        }
//                        complexPreds.put(df, finalGroups);
//                    } else {
//                        // First time seeing complex predicates for this dataframe
//                        List<Set<Predicate>> finalGroups = deduplicateGroups(newGroups);
//                        System.out.println("DEBUG merge: Storing " + finalGroups.size() + " new groups for df=" + df);
//                        for (int i = 0; i < finalGroups.size(); i++) {
//                            System.out.println("  Group " + i + ": " + finalGroups.get(i).stream()
//                                    .map(p -> getConditionKey(p.getCond()))
//                                    .collect(java.util.stream.Collectors.toList()));
//                        }
//                        complexPreds.put(df, finalGroups);
//                    }
//                }
//            }
//        }
//
//        // Step 4: UNION all predicates for continued backward flow
//        in1.union(in2, out);
//    }

    @Override
    protected void copy(FlowSet<Predicate> src, FlowSet<Predicate> dest) {
        src.copy(dest);
    }
}