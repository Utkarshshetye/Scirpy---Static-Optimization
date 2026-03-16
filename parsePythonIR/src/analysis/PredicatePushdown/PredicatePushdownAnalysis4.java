package analysis.PredicatePushdown;

import analysis.PythonScene;
import cfg.CFG;
import cfg.JPUnitGraph;
import ir.IExpr;
import ir.IStmt;
import ir.JPMethod;
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

public class PredicatePushdownAnalysis4 extends BackwardFlowAnalysis<Unit, FlowSet<PredicateTree>> {

    JPMethod jpMethod;
    PatchingChain<Unit> unitChain;
    JPUnitGraph unitGraph;

    // Unit,Predicate mapping for filter statements
    Map<Unit, PredicateModel> unitToPredMapping;

    // Tree-based storage for all predicates (simple and complex)
    // Key: dataframe name, Value: predicate tree
    Map<String, PredicateTree> predicateTrees;

    // Alias tracking
    Map<String, String> dfAliasMap;

    // Track which predicates have been killed
    Set<Predicate> killedPredicates;

    public PredicatePushdownAnalysis4(JPMethod jpMethod) {
        super(new CFG(jpMethod).getUnitGraph());
        this.jpMethod = jpMethod;
        this.unitGraph = new CFG(jpMethod).getUnitGraph();
        this.unitToPredMapping = new LinkedHashMap<>();
        this.unitChain = jpMethod.getActiveBody().getUnits();
        this.killedPredicates = new HashSet<>();
        this.predicateTrees = new LinkedHashMap<>();
        this.dfAliasMap = new HashMap<>();

        // DEBUG: Print all units BEFORE analysis
//        System.out.println("\n========== INITIAL IR DUMP ==========");
//        for (Unit u : unitChain) {
//            System.out.println(u);
//            if (u instanceof AssignmentStmtSoot) {
//                AssignmentStmtSoot as = (AssignmentStmtSoot) u;
//                System.out.println("  RHS: " + as.getAssignStmt().getRHS());
//                System.out.println("  RHS class: " + as.getAssignStmt().getRHS().getClass().getName());
//            }
//        }
//        System.out.println("========== END IR DUMP ==========\n");

        prePass();
        doAnalysis();
        extractFinalTrees();
        printFinalPredicateTrees();
    }

    private void prePass() {
        for (Unit u : unitChain) {
            collectPrintDfs(u);
            collectFilterOperations(u);
        }
    }

    /**
     * Extract dataframe name from first leaf in tree
     */
    private String extractDataframeName(PredicateTree tree) {
        return tree.getDataframeName();
    }

    private void printFinalPredicateTrees() {
        System.out.println("\n=== FINAL PREDICATE TREES (after all nesting) ===");
        for (Map.Entry<String, PredicateTree> e : predicateTrees.entrySet()) {
            System.out.println("DataFrame: " + e.getKey());
            System.out.println(e.getValue().toDebugString(1));
            System.out.println();
        }
    }

    /**
     * Recursively build a predicate tree from an expression
     * Handles BinOp, Compare, Call, UnaryOp (negation), and Subscript (boolean columns)
     */
    private PredicateTree buildTreeFromExpr(IExpr expr, String df_var, Unit unit) {
        // Handle UnaryOp (negation: ~df['status'] or not df['status'])
        if (expr instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) expr;
            String op = unaryOp.getOp().getSymbol();

            if (op.equals("~") || op.equals("not")) {
                IExpr operand = unaryOp.getLeft();

                // Special case: ~df['column'] becomes df['column'] == False
                if (operand instanceof Subscript) {
                    Subscript subscript = (Subscript) operand;
                    Slice colSlice = subscript.getSlice();

                    if (colSlice.getIndex() instanceof Constant) {
                        Constant colConst = (Constant) colSlice.getIndex();
                        String filterColumn = colConst.getValue();

                        // Negated boolean column: column == False
                        Condition cond = new Condition(filterColumn, "==", "False");
                        Predicate pred = new Predicate(cond, null, unit, df_var);

                        return new PredicateLeaf(pred);
                    }
                }

                // General negation: build inner tree and negate it
                PredicateTree inner = buildTreeFromExpr(operand, df_var, unit);
                if (inner != null) {
                    return new PredicateNot(inner);
                }
            }
        }

        // Handle BinOp (& and |)
        if (expr instanceof BinOp) {
            BinOp binOp = (BinOp) expr;
            String op = binOp.getOp().getSymbol();

            // Recursively build left and right subtrees
            PredicateTree left = buildTreeFromExpr(binOp.getLeft(), df_var, unit);
            PredicateTree right = buildTreeFromExpr(binOp.getRight(), df_var, unit);

            if (left != null && right != null) {
                if (op.equals("|")) {
                    return new PredicateOr(left, right);
                } else if (op.equals("&")) {
                    return new PredicateAnd(left, right);
                }
            }
        }

        // Handle Compare
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

                        return new PredicateLeaf(pred);
                    }
                }
            }
            else if (left instanceof Attribute) {
                Attribute leftAttr = (Attribute) left;
                String filterColumn = leftAttr.getAttr();
                String operator = comp.getOps().get(0).getSymbol();

                List<IExpr> comparators = comp.getComparators();
                if (!comparators.isEmpty() && comparators.get(0) instanceof Constant) {
                    Constant valueConst = (Constant) comparators.get(0);
                    String with = valueConst.getValue();

                    Condition cond = new Condition(filterColumn, operator, with);
                    Predicate pred = new Predicate(cond, null, unit, df_var);

                    return new PredicateLeaf(pred);
                }
            }
        }

        // Handle Call (e.g., isin)
        else if (expr instanceof Call) {
            Call call = (Call) expr;
            if (call.getFunc() instanceof Attribute) {
                Attribute funcAttr = (Attribute) call.getFunc();
                String func = funcAttr.getAttr();

                if (func.equals("isin")) {
                    IExpr colExp = funcAttr.getValue();
                    if (colExp instanceof Subscript) {
                        Subscript colSubscript = (Subscript) colExp;
                        Slice colSlice = colSubscript.getSlice();

                        if (colSlice.getIndex() instanceof Constant) {
                            String col = ((Constant) colSlice.getIndex()).getValue();
                            List<IExpr> options = call.getArgs();

                            Condition cond = new Condition(col, func, options.toString());
                            Predicate pred = new Predicate(cond, null, unit, df_var);

                            return new PredicateLeaf(pred);
                        }
                    }
                }
            }
        }

        // Handle Subscript (boolean column reference like df['active'])
        // This becomes df['active'] == True
        else if (expr instanceof Subscript) {
            Subscript subscript = (Subscript) expr;
            Slice colSlice = subscript.getSlice();

            if (colSlice.getIndex() instanceof Constant) {
                Constant colConst = (Constant) colSlice.getIndex();
                String filterColumn = colConst.getValue();

                // Boolean column treated as == True
                Condition cond = new Condition(filterColumn, "==", "True");
                Predicate pred = new Predicate(cond, null, unit, df_var);

                return new PredicateLeaf(pred);
            }
        }

        // Fallback
        return null;
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
        } else if (filterComp.getLeft() instanceof Subscript) {
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
                    if (!callArgs.isEmpty()) {
                        IExpr param = callArgs.get(0);

                        if (param instanceof Name) {
                            Name paramName = (Name) param;
                            PythonScene.printDfs.add(paramName.getName());
                        }
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

                // Filter operation - mark this unit as a filter
                if (filterExpr instanceof Compare || filterExpr instanceof BinOp ||
                        filterExpr instanceof UnaryOp || filterExpr instanceof Call ||
                        (filterExpr instanceof Subscript)) {

                    // Extract columns used in this filter
                    List<String> usedCols = extractColumnsFromFilter(filterExpr);

                    if (!usedCols.isEmpty()) {
                        int initial_line_no = filterExpr instanceof Compare ?
                                ((Compare) filterExpr).getLineno() : assignStmt.lineno;

                        PredicateModel pred = new PredicateModel(initial_line_no, -1, usedCols);
                        unitToPredMapping.putIfAbsent(unit, pred);
                    }
                }
            }
            else if (exp instanceof Name){
                List<Name> alias = as.getDataFramesDefined();
                List<Name> prev = as.getDataFramesUsed();

                if (!alias.isEmpty() && !prev.isEmpty()) {
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
    }

    /**
     * Extract all column names used in a filter expression
     */
    private List<String> extractColumnsFromFilter(IExpr expr) {
        List<String> columns = new ArrayList<>();

        if (expr instanceof Compare) {
            Compare comp = (Compare) expr;
            if (comp.getLeft() instanceof Subscript) {
                Subscript sub = (Subscript) comp.getLeft();
                if (sub.getSlice().getIndex() instanceof Constant) {
                    columns.add(((Constant) sub.getSlice().getIndex()).getValue());
                }
            } else if (comp.getLeft() instanceof Attribute) {
                columns.add(((Attribute) comp.getLeft()).getAttr());
            }
        }
        else if (expr instanceof BinOp) {
            BinOp binOp = (BinOp) expr;
            columns.addAll(extractColumnsFromFilter(binOp.getLeft()));
            columns.addAll(extractColumnsFromFilter(binOp.getRight()));
        }
        else if (expr instanceof UnaryOp) {
            UnaryOp unaryOp = (UnaryOp) expr;
            columns.addAll(extractColumnsFromFilter(unaryOp.getLeft()));
        }
        else if (expr instanceof Call) {
            Call call = (Call) expr;
            if (call.getFunc() instanceof Attribute) {
                Attribute funcAttr = (Attribute) call.getFunc();
                if (funcAttr.getValue() instanceof Subscript) {
                    Subscript sub = (Subscript) funcAttr.getValue();
                    if (sub.getSlice().getIndex() instanceof Constant) {
                        columns.add(((Constant) sub.getSlice().getIndex()).getValue());
                    }
                }
            }
        }
        else if (expr instanceof Subscript) {
            // Boolean column reference like df['active']
            Subscript sub = (Subscript) expr;
            if (sub.getSlice().getIndex() instanceof Constant) {
                columns.add(((Constant) sub.getSlice().getIndex()).getValue());
            }
        }

        return columns;
    }

    private boolean isDataSource(Unit unit) {
        if (unit instanceof AssignmentStmtSoot) {
            AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
            AssignStmt assignStmt = as.getAssignStmt();
            IExpr rhs = assignStmt.getRHS();

            if (rhs instanceof Call) {
                Call call = (Call) rhs;
                if (call.getFunc() instanceof Attribute) {
                    Attribute srcAttr = (Attribute) call.getFunc();
                    String op = srcAttr.getAttr();
                    return op.equals("read_csv") || op.equals("read_parquet");
                }
            }
        }
        return false;
    }

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

    /**
     * Get the resolved original dataframe name (follow alias chain)
     */
    private String resolveAlias(String dfName) {
        String original = dfName;
        while (dfAliasMap.containsKey(original) && !dfAliasMap.get(original).equals(original)) {
            original = dfAliasMap.get(original);
        }
        return original;
    }

    /**
     * Flow function: propagate predicate trees backward
     * Keep separate trees for each dataframe with proper killing logic
     */
    protected void flowThrough(FlowSet<PredicateTree> in, Unit u, FlowSet<PredicateTree> out) {
        // Copy input to output (backward flow)
        in.copy(out);

        if (u instanceof AssignmentStmtSoot) {
            AssignmentStmtSoot as = (AssignmentStmtSoot) u;
            AssignStmt stmt = as.getAssignStmt();
            IExpr rhs = stmt.getRHS();

            List<Name> definedDfs = as.getDataFramesDefined();
            List<Name> usedDfs = as.getDataFramesUsed();

            // Get the kill set for this unit
            FlowSet<PredicateTree> killSet = getKillSet(u, out);

            // Apply kill set - remove killed trees
            for (PredicateTree killedTree : killSet) {
                out.remove(killedTree);
                collectKilledPredicates(killedTree);
            }

            // Check if this is a filter operation: target = source[filter_expr]
            if (rhs instanceof Subscript) {
                Subscript sub = (Subscript) rhs;
                Slice sl = sub.getSlice();
                IExpr filterExpr = sl.getIndex();

                // Check if it's actually a filter (not just column selection)
                if (filterExpr instanceof Compare || filterExpr instanceof BinOp ||
                        filterExpr instanceof UnaryOp || filterExpr instanceof Call ||
                        (filterExpr instanceof Subscript)) {

                    if (!usedDfs.isEmpty() && !definedDfs.isEmpty()) {
                        String sourceDf = usedDfs.get(0).getName();
                        String targetDf = definedDfs.get(0).getName();

                        System.out.println("[FlowThrough] Filter: " + targetDf + " = " + sourceDf + "[...]");

                        // Build tree for this filter operation
                        String resolvedSourceDf = resolveAlias(sourceDf);
                        PredicateTree newTree = buildTreeFromExpr(filterExpr, resolvedSourceDf, u);

                        if (newTree != null) {
                            System.out.println("  Generated tree for source df '" + resolvedSourceDf + "':");
                            System.out.println(newTree.toDebugString(2));

                            // Check if source df already has a tree in the flow
                            PredicateTree existingSourceTree = null;
                            Iterator<PredicateTree> it = out.iterator();
                            while (it.hasNext()) {
                                PredicateTree t = it.next();
                                String treeDf = extractDataframeName(t);
                                if (treeDf != null && resolveAlias(treeDf).equals(resolvedSourceDf)) {
                                    existingSourceTree = t;
                                    break;
                                }
                            }

                            if (existingSourceTree != null) {
                                // Source df already has predicates
                                // Multiple filters on same df → OR them (they create different derived dfs)
                                System.out.println("  Source df already has tree - OR-ing");
                                PredicateTree orTree = new PredicateOr(existingSourceTree, newTree);
                                out.remove(existingSourceTree);
                                out.add(orTree);
                                System.out.println(orTree.toDebugString(2));
                            } else {
                                // First filter on this source df
                                out.add(newTree);
                                System.out.println("  Added first tree for source df: " + resolvedSourceDf);
                            }
                        }
                    }
                }
            }
        }

        handleAliasOperation(u);
    }

    /**
     * Get kill set - predicates that should be killed at this unit
     * Based on the set-based implementation logic
     */
    private FlowSet<PredicateTree> getKillSet(Unit unit, FlowSet<PredicateTree> outSet) {
        FlowSet<PredicateTree> killSet = new ArraySparseSet<>();

        if (!(unit instanceof AssignmentStmtSoot)) {
            return killSet;
        }

        AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = as.getAssignStmt();
        IExpr rhs = assignStmt.getRHS();

        List<Name> definedDfs = as.getDataFramesDefined();
        if (definedDfs.isEmpty()) {
            return killSet;
        }

        String assignedDf = definedDfs.get(0).getName();
        IExpr target = assignStmt.getTargets().get(0);

        List<Name> usedDfs = as.getDataFramesUsed();
        String sourceDf = usedDfs.isEmpty() ? assignedDf : usedDfs.get(0).getName();
        String resolvedSourceDf = resolveAlias(sourceDf);

        // Get modified column (if column assignment)
        String modifiedColumn = null;
        if (target instanceof Subscript) {
            Subscript lhsScript = (Subscript) target;
            IExpr exprType = lhsScript.getSlice().getIndex();
            if (exprType instanceof Constant) {
                modifiedColumn = ((Constant) exprType).getValue();
            }
        }

        boolean isFilter = unitToPredMapping.containsKey(unit);
        boolean isRowPreserving = isRowPreserving(as, assignedDf);

        System.out.println("[getKillSet] Unit: " + unit);
        System.out.println("  assignedDf: " + assignedDf + ", sourceDf: " + resolvedSourceDf);
        System.out.println("  isFilter: " + isFilter + ", isRowPreserving: " + isRowPreserving);
        System.out.println("  modifiedColumn: " + modifiedColumn);

        // Case 1: Non-row-preserving operation kills all predicates on BOTH assigned and source dataframes
        if (!isFilter && !isRowPreserving) {
            System.out.println("  KILLING: Non-row-preserving operation");

            // Kill predicates for the assigned (output) dataframe
            for (PredicateTree tree : outSet) {
                String treeDf = extractDataframeName(tree);
                if (treeDf != null && treeDf.equals(assignedDf)) {
                    killSet.add(tree);
                    System.out.println("    Killing tree for assigned df: " + treeDf);
                }
            }

            // CRITICAL: Also kill predicates for the source dataframe
            // because it's being used in a non-row-preserving way
            for (PredicateTree tree : outSet) {
                String treeDf = extractDataframeName(tree);
                if (treeDf != null && resolveAlias(treeDf).equals(resolvedSourceDf)) {
                    killSet.add(tree);
                    System.out.println("    Killing tree for source df: " + treeDf);
                }
            }
        }
        // Case 2: Row-preserving but column-modifying operation
        else if (!isFilter && isRowPreserving && modifiedColumn != null) {
            System.out.println("  KILLING: Column modification on " + modifiedColumn);

            // Kill predicates on the assigned dataframe that use the modified column
            for (PredicateTree tree : outSet) {
                String treeDf = extractDataframeName(tree);
                if (treeDf != null && treeDf.equals(assignedDf)) {
                    if (treeContainsColumn(tree, modifiedColumn)) {
                        killSet.add(tree);
                        System.out.println("    Killing tree (uses modified column): " + treeDf);
                    }
                }
            }
        }

        return killSet;
    }

    /**
     * Check if a predicate tree contains any predicate on the given column
     */
    private boolean treeContainsColumn(PredicateTree tree, String column) {
        if (tree instanceof PredicateLeaf) {
            PredicateLeaf leaf = (PredicateLeaf) tree;
            return leaf.predicate.getCond().getOn().equals(column);
        } else if (tree instanceof PredicateNot) {
            return treeContainsColumn(((PredicateNot) tree).child, column);
        } else if (tree instanceof PredicateAnd) {
            for (PredicateTree child : ((PredicateAnd) tree).children) {
                if (treeContainsColumn(child, column)) {
                    return true;
                }
            }
        } else if (tree instanceof PredicateOr) {
            for (PredicateTree child : ((PredicateOr) tree).children) {
                if (treeContainsColumn(child, column)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Collect all predicates from a tree and mark them as killed
     */
    private void collectKilledPredicates(PredicateTree tree) {
        if (tree instanceof PredicateLeaf) {
            PredicateLeaf leaf = (PredicateLeaf) tree;
            killedPredicates.add(leaf.predicate);
            System.out.println("    Killed predicate: " + getConditionKey(leaf.predicate.getCond()));
        } else if (tree instanceof PredicateNot) {
            collectKilledPredicates(((PredicateNot) tree).child);
        } else if (tree instanceof PredicateAnd) {
            for (PredicateTree child : ((PredicateAnd) tree).children) {
                collectKilledPredicates(child);
            }
        } else if (tree instanceof PredicateOr) {
            for (PredicateTree child : ((PredicateOr) tree).children) {
                collectKilledPredicates(child);
            }
        }
    }

    /**
     * Merge function: combine trees from different control flow paths
     * Only OR trees that belong to the SAME dataframe
     */
    @Override
    protected void merge(FlowSet<PredicateTree> in1, FlowSet<PredicateTree> in2, FlowSet<PredicateTree> out) {
        mergeCallCount++;
        System.out.println("\n[Merge #" + mergeCallCount + "]");
        System.out.println("  in1 size: " + in1.size());
        System.out.println("  in2 size: " + in2.size());

        // If one branch is empty, use the other
        if (in1.isEmpty() && !in2.isEmpty()) {
            in2.copy(out);
            System.out.println("  Used in2 (in1 empty)");
            return;
        }
        if (in2.isEmpty() && !in1.isEmpty()) {
            in1.copy(out);
            System.out.println("  Used in1 (in2 empty)");
            return;
        }

        // If both empty, output is empty
        if (in1.isEmpty() && in2.isEmpty()) {
            out.clear();
            System.out.println("  Both empty");
            return;
        }

        // Build a map of dataframe -> tree for each input
        Map<String, PredicateTree> df1Trees = new HashMap<>();
        Map<String, PredicateTree> df2Trees = new HashMap<>();

        Iterator<PredicateTree> it1 = in1.iterator();
        while (it1.hasNext()) {
            PredicateTree t = it1.next();
            String df = extractDataframeName(t);
            if (df != null) {
                String resolved = resolveAlias(df);
                df1Trees.put(resolved, t);
            }
        }

        Iterator<PredicateTree> it2 = in2.iterator();
        while (it2.hasNext()) {
            PredicateTree t = it2.next();
            String df = extractDataframeName(t);
            if (df != null) {
                String resolved = resolveAlias(df);
                df2Trees.put(resolved, t);
            }
        }

        System.out.println("  Branch 1 dataframes: " + df1Trees.keySet());
        System.out.println("  Branch 2 dataframes: " + df2Trees.keySet());

        // Merge trees for each dataframe separately
        Set<String> allDfs = new HashSet<>();
        allDfs.addAll(df1Trees.keySet());
        allDfs.addAll(df2Trees.keySet());

        out.clear();

        for (String df : allDfs) {
            PredicateTree t1 = df1Trees.get(df);
            PredicateTree t2 = df2Trees.get(df);

            if (t1 != null && t2 != null) {
                // Both branches have predicates for this df → OR them
                PredicateTree orTree = new PredicateOr(t1, t2);
                out.add(orTree);
                System.out.println("  [Merge] OR-ed trees for df=" + df);
                System.out.println(orTree.toDebugString(2));
            } else if (t1 != null) {
                // Only branch 1 has predicates for this df
                out.add(t1);
                System.out.println("  [Merge] Kept tree from branch 1 for df=" + df);
            } else if (t2 != null) {
                // Only branch 2 has predicates for this df
                out.add(t2);
                System.out.println("  [Merge] Kept tree from branch 2 for df=" + df);
            }
        }
    }

    /**
     * Extract final predicate trees from the entry point of the CFG
     */
    private void extractFinalTrees() {
        System.out.println("\n========== EXTRACTING FINAL TREES ==========");

        List<Unit> heads = unitGraph.getHeads();
        if (heads.isEmpty()) {
            return;
        }

        Unit entryPoint = heads.get(0);
        FlowSet<PredicateTree> entryFlow = getFlowBefore(entryPoint);

        System.out.println("Entry flow has " + entryFlow.size() + " trees");

        // Extract all trees (one per dataframe)
        if (!entryFlow.isEmpty()) {
            Iterator<PredicateTree> it = entryFlow.iterator();
            while (it.hasNext()) {
                PredicateTree tree = it.next();

                // Extract dataframe name from tree
                String df = extractDataframeName(tree);
                if (df != null) {
                    String resolved = resolveAlias(df);
                    predicateTrees.put(resolved, tree);
                    System.out.println("Extracted tree for df=" + resolved);
                    System.out.println(tree.toDebugString(1));
                }
            }
        }

        System.out.println("========== END EXTRACTION ==========\n");
    }

    private void handleAliasOperation(Unit unit) {
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
            String orgDf = resolveAlias(sourceDf);

            dfAliasMap.put(aliasDf, orgDf);
        }
    }

    private String getConditionKey(Condition cond) {
        return "(" + cond.getOn() + ", " + cond.getOp() + ", " + cond.getWith() + ")";
    }

    @Override
    protected FlowSet<PredicateTree> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    private static int mergeCallCount = 0;

    @Override
    protected void copy(FlowSet<PredicateTree> src, FlowSet<PredicateTree> dest) {
        src.copy(dest);
    }

    public void insertFilters() {
        JPUnitGraph unitGraph = new CFG(jpMethod).getUnitGraph();
        Unit unit = unitGraph.getHeads().get(0);

        do {
            if (isDataSource(unit)) {
                IStmt stmt = (IStmt) unit;
                String df_def = stmt.getDataFramesDefined().get(0).getName();

                rewriteDataSource(unit, df_def);

                System.out.println("After " + df_def);
            }

            unit = unitGraph.getSuccsOf(unit).isEmpty() ? null : unitGraph.getSuccsOf(unit).get(0);

        } while(unit != null);
    }

    private void rewriteDataSource(Unit u, String df) {
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

        String predicates = "None";

        // Check if we have a tree for this dataframe
        String resolvedDf = resolveAlias(df);
        PredicateTree tree = predicateTrees.get(resolvedDf);

        if (tree == null) {
            tree = predicateTrees.get(df);
        }

        System.out.println("DEBUG rewriteDataSource: Looking for tree for df=" + df);

        if (tree != null) {
            System.out.println("DEBUG rewriteDataSource: Flattening tree for df=" + df);
            System.out.println(tree.toDebugString(1));

            // Flatten the tree to groups
            List<Set<Predicate>> groups = tree.flatten();

            // Filter out killed predicates
            List<Set<Predicate>> validGroups = new ArrayList<>();
            for (Set<Predicate> group : groups) {
                Set<Predicate> validPredicates = new HashSet<>();
                for (Predicate p : group) {
                    if (!killedPredicates.contains(p)) {
                        validPredicates.add(p);
                    }
                }
                if (!validPredicates.isEmpty()) {
                    validGroups.add(validPredicates);
                }
            }

            if (validGroups.isEmpty()) {
                System.out.println("  All predicates were killed - no pushdown");
                predicates = "None";
            } else {
                System.out.println("  Flattened to " + validGroups.size() + " valid groups:");
                for (int i = 0; i < validGroups.size(); i++) {
                    System.out.println("    Group " + i + ": " + validGroups.get(i).stream()
                            .map(p -> getConditionKey(p.getCond()))
                            .collect(Collectors.toList()));
                }

                // Convert to string for predicate pushdown
                predicates = formatGroups(validGroups);

                // Count total number of filter operations on this source dataframe
                int totalFilterCount = countTotalFiltersForDataframe(df);

                System.out.println("  Total filters on dataframe '" + df + "': " + totalFilterCount);

                // Only remove filter statements if there is exactly ONE filter operation
                if (totalFilterCount == 1) {
                    System.out.println("  Single filter detected - replacing with aliases");

                    // Replace all filters in the predicate tree with aliases
                    for (Set<Predicate> group : validGroups) {
                        for (Predicate p : group) {
                            Unit filterUnit = p.getUnit();
                            if (filterUnit instanceof AssignmentStmtSoot) {
                                AssignmentStmtSoot filterAs = (AssignmentStmtSoot) filterUnit;
                                AssignStmt filterStmt = filterAs.getAssignStmt();

                                // Get the target variable (e.g., df1)
                                IExpr target = filterStmt.getTargets().get(0);

                                // Create a simple alias: df1 = df (instead of df1 = df[filter])
                                Name sourceRef = new Name(filterStmt.lineno, filterStmt.col_offset, df, null);

                                // Replace RHS with simple reference to source dataframe
                                filterStmt.setRHS(sourceRef);

                                System.out.println("  Replaced filter with alias: " + target + " = " + df);
                            }
                        }
                    }
                } else {
                    System.out.println("  Multiple filters detected (" + totalFilterCount + ") - keeping all filter statements");
                }
            }
        } else {
            System.out.println("  No predicate tree found for dataframe: " + df);
        }

        // Always perform predicate pushdown (add filters keyword to read_csv/read_parquet)
        Constant filterGrp = new Constant(lno, col_offset, predicates);
        kws.add(new Keyword("filters", filterGrp));
        readCsvCall.setKeywords(kws);

        System.out.println("  Added filters keyword: " + predicates);
    }

    /**
     * Count the total number of filter operations for a specific dataframe
     * (counts all filters that operate on this dataframe directly)
     */
    private int countTotalFiltersForDataframe(String dfName) {
        int count = 0;
        String resolvedDfName = resolveAlias(dfName);

        for (Unit unit : unitChain) {
            if (unit instanceof AssignmentStmtSoot) {
                AssignmentStmtSoot as = (AssignmentStmtSoot) unit;
                AssignStmt stmt = as.getAssignStmt();
                IExpr rhs = stmt.getRHS();

                // Check if this is a filter operation
                if (rhs instanceof Subscript) {
                    Subscript sub = (Subscript) rhs;
                    Slice sl = sub.getSlice();
                    IExpr filterExpr = sl.getIndex();

                    // Check if the filter expression is an actual filter condition
                    if (filterExpr instanceof Compare || filterExpr instanceof BinOp ||
                            filterExpr instanceof UnaryOp || filterExpr instanceof Call ||
                            (filterExpr instanceof Subscript)) {

                        List<Name> usedDfs = as.getDataFramesUsed();
                        if (!usedDfs.isEmpty()) {
                            String usedDf = resolveAlias(usedDfs.get(0).getName());

                            // Count if this filter operates on the target dataframe
                            if (usedDf.equals(resolvedDfName)) {
                                count++;
                                System.out.println("    Found filter #" + count + " on '" + dfName + "': " + unit);
                            }
                        }
                    }
                }
            }
        }

        return count;
    }

    private String formatGroups(List<Set<Predicate>> groups) {
        if (groups.isEmpty()) {
            return "None";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("[");
            int j = 0;
            for (Predicate p : groups.get(i)) {
                if (j > 0) sb.append(", ");
                sb.append(getConditionKey(p.getCond()));
                j++;
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}