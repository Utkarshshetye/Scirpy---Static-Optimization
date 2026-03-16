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

public class PredicatePushdownAnalysis3 extends BackwardFlowAnalysis<Unit, FlowSet<Predicate>> {

    JPMethod jpMethod;
    PatchingChain<Unit> unitChain;

    // Unit,Predicate mapping for filter statements
    Map<Unit, PredicateModel> unitToPredMapping;

    // Tree-based storage for all predicates (simple and complex)
    Map<String, PredicateTree> predicateTrees;

    // Alias tracking
    Map<String, String> dfAliasMap;

    // Track which predicates have been killed
    Set<Predicate> killedPredicates;

    public PredicatePushdownAnalysis3(JPMethod jpMethod) {
        super(new CFG(jpMethod).getUnitGraph());
        this.jpMethod = jpMethod;
        this.unitToPredMapping = new LinkedHashMap<>();
        this.unitChain = jpMethod.getActiveBody().getUnits();
        this.killedPredicates = new HashSet<>();
        this.predicateTrees = new LinkedHashMap<>();
        this.dfAliasMap = new HashMap<>();

        prePass();
        doAnalysis();
    }

    private void prePass() {
        for (Unit u : unitChain) {
            collectPrintDfs(u);
            collectFilterOperations(u);
        }
    }

    /**
     * Helper method to add a predicate or tree to the existing tree for a dataframe
     * Sequential predicates are AND-ed together
     */
    private void addPredicateToTree(String df, PredicateTree newTree) {
        if (predicateTrees.containsKey(df)) {
            PredicateTree existing = predicateTrees.get(df);

            // Merge with AND (sequential predicates are AND-ed together)
            PredicateTree merged = new PredicateAnd(existing, newTree);
            predicateTrees.put(df, merged);

            System.out.println("DEBUG addPredicateToTree: Added to existing tree for df=" + df);
            System.out.println(merged.toDebugString(1));
        } else {
            predicateTrees.put(df, newTree);
            System.out.println("DEBUG addPredicateToTree: Created new tree for df=" + df);
            System.out.println(newTree.toDebugString(1));
        }
    }

    /**
     * Recursively build a predicate tree from a BinOp expression
     */
    private PredicateTree buildTreeFromBinOp(IExpr expr, String df_var, Unit unit) {
        if (expr instanceof BinOp) {
            BinOp binOp = (BinOp) expr;
            String op = binOp.getOp().getSymbol();

            // Recursively build left and right subtrees
            PredicateTree left = buildTreeFromBinOp(binOp.getLeft(), df_var, unit);
            PredicateTree right = buildTreeFromBinOp(binOp.getRight(), df_var, unit);

            if (op.equals("|")) {
                return new PredicateOr(left, right);
            } else if (op.equals("&")) {
                return new PredicateAnd(left, right);
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

                        return new PredicateLeaf(pred);
                    }
                }
            }
        }
        else if (expr instanceof Call) {
            Call call = (Call) expr;
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

        // Fallback - should not reach here
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

    private FlowSet<Predicate> getGenSet(Unit unit) {
        FlowSet<Predicate> genSet = new ArraySparseSet<>();

        if (!(unit instanceof AssignmentStmtSoot)) {
            return genSet;
        }

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

            if (compExpr instanceof Compare) {
                handleSimpleCompare(as, assignStmt, compExpr, genSet);
            }
            else if (compExpr instanceof Call) {
                handleCallExpr(as, assignStmt, compExpr, genSet);
            }
            else if (compExpr instanceof BinOp) {
                handleBinOpExpr(as, assignStmt, compExpr, genSet);
            }
            else if (compExpr instanceof Subscript || compExpr instanceof UnaryOp) {
                handleBooleanExpr(as, assignStmt, compExpr, genSet);
            }
        }

        return genSet;
    }

    private void handleSimpleCompare(AssignmentStmtSoot as, AssignStmt assignStmt, IExpr compExpr, FlowSet<Predicate> genSet) {
        Compare compExp = (Compare) compExpr;
        IExpr leftExp = compExp.getLeft();

        if (leftExp instanceof Subscript) {
            Subscript leftPred = (Subscript) leftExp;
            String df_var = leftPred.getValue().toString();
            String originalDf = resolveAlias(df_var);

            Slice colSlice = leftPred.getSlice();
            Constant colOp = (Constant) colSlice.getIndex();
            String filterColumn = colOp.getValue();
            String ops = compExp.getOps().get(0).getSymbol();
            List<IExpr> exprs = compExp.getComparators();
            Constant constWith = (Constant) exprs.get(0);
            String with = constWith.toString();

            Condition conds = new Condition(filterColumn, ops, with);
            Predicate genPred = new Predicate(conds, null, (Unit)as, originalDf);

            // Don't add to genSet - goes to tree only
            PredicateTree leaf = new PredicateLeaf(genPred);
            addPredicateToTree(originalDf, leaf);

        } else if (leftExp instanceof Attribute) {
            Attribute leftEx = (Attribute) leftExp;
            String filter_Column = leftEx.getAttr();
            String ops = compExp.getOps().get(0).getSymbol();
            String df_var = leftEx.getBaseName();
            String originalDf = resolveAlias(df_var);

            List<IExpr> exprs = compExp.getComparators();
            Constant constWith = (Constant) exprs.get(0);
            String with = constWith.getValue();

            Condition conds = new Condition(filter_Column, ops, with);
            Predicate genPred = new Predicate(conds, null, (Unit)as, originalDf);

            // Don't add to genSet - goes to tree only
            PredicateTree leaf = new PredicateLeaf(genPred);
            addPredicateToTree(originalDf, leaf);
        }
    }

    private void handleCallExpr(AssignmentStmtSoot as, AssignStmt assignStmt, IExpr compExpr, FlowSet<Predicate> genSet) {
        Call call = (Call) compExpr;

        if (call.getFunc() instanceof Attribute) {
            Attribute funAttr = (Attribute) call.getFunc();
            String fun = funAttr.getName();

            if (fun.equals("isin")) {
                Subscript colSubscript = (Subscript) funAttr.getValue();
                String df_name = colSubscript.getBaseName();
                Slice colSlice = colSubscript.getSlice();

                String originalDf = resolveAlias(df_name);

                if (colSlice.getIndex() instanceof Constant) {
                    String col = ((Constant) colSlice.getIndex()).getValue();
                    List<IExpr> options = call.getArgs();

                    Condition cond = new Condition(col, fun, options.toString());
                    Predicate new_pred = new Predicate(cond, null, (Unit)as, originalDf);

                    // Don't add to genSet - goes to tree only
                    PredicateTree leaf = new PredicateLeaf(new_pred);
                    addPredicateToTree(originalDf, leaf);
                }
            }
        }
    }

    private void handleBinOpExpr(AssignmentStmtSoot as, AssignStmt assignStmt, IExpr compExpr, FlowSet<Predicate> genSet) {
        List<Name> df = as.getDataFramesUsed();
        String df_var = df.get(0).getName();
        String originalDf = resolveAlias(df_var);

        // Build tree from BinOp expression (handles complex predicates)
        PredicateTree tree = buildTreeFromBinOp(compExpr, originalDf, (Unit)as);

        System.out.println("DEBUG: Built tree from BinOp for df=" + originalDf);
        System.out.println(tree.toDebugString(1));

        // Add to tree (this is a complex predicate)
        addPredicateToTree(originalDf, tree);
    }

    private void handleBooleanExpr(AssignmentStmtSoot as, AssignStmt assignStmt, IExpr compExpr, FlowSet<Predicate> genSet) {
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
        String originalDf = resolveAlias(df_var);

        Slice colSlice = leftPred.getSlice();
        Constant colOp = (Constant) colSlice.getIndex();
        String filterColumn = colOp.getValue();

        String ops = opsType.get(0).getSymbol();
        String with = ((Constant) check).getValue();

        Condition conds = new Condition(filterColumn, ops, with);
        Predicate genPred = new Predicate(conds, null, (Unit)as, originalDf);

        // Don't add to genSet - goes to tree only
        PredicateTree leaf = new PredicateLeaf(genPred);
        addPredicateToTree(originalDf, leaf);
    }

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
        String originalSourceDf = resolveAlias(sourceDf);

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

        // Case 1: Non-row-preserving operation kills ALL predicates AND tree
        if (!isFilter && !isRowPreserving) {
            // Kill tree for this dataframe
            predicateTrees.remove(originalSourceDf);

            System.out.println("DEBUG getKillSet: Non-row-preserving op, killed tree for df=" + originalSourceDf);
        }
        // Case 2: Row-preserving but column-modifying operation
        else if (!isFilter && isRowPreserving && modifiedColumn != null) {
            // Need to remove predicates using this column from the tree
            if (predicateTrees.containsKey(originalSourceDf)) {
                PredicateTree tree = predicateTrees.get(originalSourceDf);
                PredicateTree filtered = filterTreeByColumn(tree, modifiedColumn, originalSourceDf);

                if (filtered != null) {
                    predicateTrees.put(originalSourceDf, filtered);
                } else {
                    predicateTrees.remove(originalSourceDf);
                }

                System.out.println("DEBUG getKillSet: Filtered tree for df=" + originalSourceDf + ", column=" + modifiedColumn);
            }
        }

        return killSet;
    }

    /**
     * Filter a tree to remove predicates on a specific column
     * Returns null if tree becomes empty
     */
    private PredicateTree filterTreeByColumn(PredicateTree tree, String column, String df) {
        if (tree instanceof PredicateLeaf) {
            PredicateLeaf leaf = (PredicateLeaf) tree;
            if (leaf.predicate.getCond().getOn().equals(column) &&
                    leaf.predicate.getDf_var().equals(df)) {
                return null; // Remove this leaf
            }
            return tree; // Keep this leaf
        }
        else if (tree instanceof PredicateAnd) {
            PredicateAnd and = (PredicateAnd) tree;
            List<PredicateTree> filteredChildren = new ArrayList<>();

            for (PredicateTree child : and.children) {
                PredicateTree filtered = filterTreeByColumn(child, column, df);
                if (filtered != null) {
                    filteredChildren.add(filtered);
                }
            }

            if (filteredChildren.isEmpty()) {
                return null;
            } else if (filteredChildren.size() == 1) {
                return filteredChildren.get(0);
            } else {
                return new PredicateAnd(filteredChildren);
            }
        }
        else if (tree instanceof PredicateOr) {
            PredicateOr or = (PredicateOr) tree;
            List<PredicateTree> filteredChildren = new ArrayList<>();

            for (PredicateTree child : or.children) {
                PredicateTree filtered = filterTreeByColumn(child, column, df);
                if (filtered != null) {
                    filteredChildren.add(filtered);
                }
            }

            if (filteredChildren.isEmpty()) {
                return null;
            } else if (filteredChildren.size() == 1) {
                return filteredChildren.get(0);
            } else {
                return new PredicateOr(filteredChildren);
            }
        }

        return tree;
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

            // Copy tree from source to alias
            if (predicateTrees.containsKey(sourceDf)) {
                PredicateTree sourceTree = predicateTrees.get(sourceDf);
                PredicateTree aliasTree = cloneTreeWithNewDf(sourceTree, aliasDf);
                predicateTrees.put(aliasDf, aliasTree);

                System.out.println("DEBUG handleAliasOperation: Cloned tree from " + sourceDf + " to " + aliasDf);
            }
        }
    }

    /**
     * Clone a tree and update all df_var references
     */
    private PredicateTree cloneTreeWithNewDf(PredicateTree tree, String newDf) {
        if (tree instanceof PredicateLeaf) {
            PredicateLeaf leaf = (PredicateLeaf) tree;
            Predicate newPred = new Predicate(
                    leaf.predicate.getCond(),
                    leaf.predicate.getParentUnit(),
                    leaf.predicate.getUnit(),
                    newDf
            );
            return new PredicateLeaf(newPred);
        }
        else if (tree instanceof PredicateAnd) {
            PredicateAnd and = (PredicateAnd) tree;
            List<PredicateTree> newChildren = new ArrayList<>();
            for (PredicateTree child : and.children) {
                newChildren.add(cloneTreeWithNewDf(child, newDf));
            }
            return new PredicateAnd(newChildren);
        }
        else if (tree instanceof PredicateOr) {
            PredicateOr or = (PredicateOr) tree;
            List<PredicateTree> newChildren = new ArrayList<>();
            for (PredicateTree child : or.children) {
                newChildren.add(cloneTreeWithNewDf(child, newDf));
            }
            return new PredicateOr(newChildren);
        }

        return tree;
    }

    private String getConditionKey(Condition cond) {
        return "(" + cond.getOn() + ", " + cond.getOp() + ", " + cond.getWith() + ")";
    }

    @Override
    protected void flowThrough(FlowSet<Predicate> inSet, Unit unit, FlowSet<Predicate> outSet) {
        System.out.println("flowThrough: " + unit.getClass().getSimpleName());

        // Step 1: Copy input to output (backward flow)
        inSet.copy(outSet);

        // Step 2: Generate predicates (adds to trees, not outSet)
        FlowSet<Predicate> genSet = getGenSet(unit);

        // Step 3: Kill predicates (modifies trees)
        FlowSet<Predicate> killSet = getKillSet(unit, outSet);

        // Step 4: Handle alias operations
        handleAliasOperation(unit);

        System.out.println("  After flowThrough, predicateTrees state:");
        for (String dfKey : predicateTrees.keySet()) {
            PredicateTree tree = predicateTrees.get(dfKey);
            System.out.println("    df=" + dfKey);
            System.out.println(tree.toDebugString(2));
        }
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

        // Union for backward flow (doesn't matter much with tree approach)
        in1.union(in2, out);

        // The real merging happens in the trees
        // We need to identify which dataframes have different predicates in the two branches

        // Get all dataframes that have trees
        Set<String> allDfs = new HashSet<>(predicateTrees.keySet());

        // For each dataframe, check if we need to create an OR node
        for (String df : allDfs) {
            PredicateTree tree = predicateTrees.get(df);

            // Check if this tree was created during this merge
            // (In practice, need to track which trees are "branch-specific")
            // For now assuming merge is called at if/else join points

            // The tree already contains the OR logic from the branches
            // Nothing additional needed here
        }

        System.out.println("========== END MERGE #" + mergeCallCount + " ==========\n");
    }

    @Override
    protected void copy(FlowSet<Predicate> src, FlowSet<Predicate> dest) {
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
        PredicateTree tree = predicateTrees.get(df);

        if (tree != null) {
            System.out.println("DEBUG rewriteDataSource: Flattening tree for df=" + df);
            System.out.println(tree.toDebugString(1));

            // Flatten the tree to groups
            List<Set<Predicate>> groups = tree.flatten();

            System.out.println("  Flattened to " + groups.size() + " groups:");
            for (int i = 0; i < groups.size(); i++) {
                System.out.println("    Group " + i + ": " + groups.get(i).stream()
                        .map(p -> getConditionKey(p.getCond()))
                        .collect(Collectors.toList()));
            }

            // Convert to string
            predicates = formatGroups(groups);

            // Remove filter statements from the code
            Set<Unit> unitsToRemove = new HashSet<>();
            for (Set<Predicate> group : groups) {
                for (Predicate p : group) {
                    unitsToRemove.add(p.getUnit());
                }
            }
            unitChain.removeAll(unitsToRemove);
        }

        Constant filterGrp = new Constant(lno, col_offset, predicates);
        kws.add(new Keyword("filters", filterGrp));
        readCsvCall.setKeywords(kws);
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