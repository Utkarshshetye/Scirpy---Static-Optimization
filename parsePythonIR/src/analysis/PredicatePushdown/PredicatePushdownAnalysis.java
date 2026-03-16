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
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.*;

import java.util.*;
import java.util.stream.Collectors;

public class PredicatePushdownAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<PredicateTree>> {

    JPMethod               jpMethod;
    PatchingChain<Unit>    unitChain;
    JPUnitGraph            unitGraph;
    Map<Unit, PredicateModel>  unitToPredMapping;
    // Keyed by the read_csv/read_parquet Unit so that two read_csv calls
    // that assign the same variable name are tracked independently.
    Map<Unit, PredicateTree> predicateTrees;
    // Current unit being processed in flowThrough — used by merge()
    Unit currentFlowUnit = null;
    // Loop headers whose bodies contain NO df assignment.
    // For these, an empty back-edge means "transparent" not "unconstrained",
    // so merge should not let {} dominate — the loop-exit value passes through.
    Set<Unit> transparentLoopHeaders;
    Map<String, String>        dfAliasMap;

    public PredicatePushdownAnalysis(JPMethod jpMethod) {
        super(new CFG(jpMethod).getUnitGraph());
        this.jpMethod          = jpMethod;
        this.unitGraph         = new CFG(jpMethod).getUnitGraph();
        this.unitToPredMapping = new LinkedHashMap<>();
        this.unitChain         = jpMethod.getActiveBody().getUnits();
        this.predicateTrees    = new LinkedHashMap<>(); // Unit → PredicateTree
        this.dfAliasMap        = new HashMap<>();
        this.transparentLoopHeaders = computeTransparentLoopHeaders();

        System.out.println("\n========== INITIAL IR DUMP ==========");
        for (Unit u : unitChain) {
            System.out.println(u + "  [" + u.getClass().getSimpleName() + "]");
            if (u instanceof AssignmentStmtSoot) {
                AssignmentStmtSoot as = (AssignmentStmtSoot) u;
                IExpr tgt = as.getAssignStmt().getTargets().get(0);
                IExpr rhs = as.getAssignStmt().getRHS();
                System.out.println("  TGT: " + tgt + "  [" + tgt.getClass().getSimpleName() + "]");
                System.out.println("  RHS: " + rhs + "  [" + rhs.getClass().getSimpleName() + "]");
            }
        }
        System.out.println("========== END IR DUMP ==========\n");

        prePass();
        doAnalysis();
        extractFinalTrees();
        printFinalPredicateTrees();
    }

    // ==================================================================//
    //  Soot framework methods                                             //
    // ==================================================================//

    /**
     * Returns a PredicateFlowSet — extends ArraySparseSet with a
     * signature-based equals() so Soot's fixed-point check fires when
     * two flow sets contain logically equivalent trees, terminating loops.
     */
    @Override
    protected FlowSet<PredicateTree> newInitialFlow() {
        return new PredicateFlowSet();
    }

    @Override
    protected void copy(FlowSet<PredicateTree> src, FlowSet<PredicateTree> dest) {
        src.copy(dest);
    }

    // ==================================================================//
    //  removeByEquals                                                     //
    //                                                                     //
    //  ArraySparseSet.remove() uses == internally. When existingTree      //
    //  was found via iterator (which uses equals()), it may be a          //
    //  different object reference than what the set stores, so            //
    //  out.remove(existingTree) silently fails, leaving duplicates.       //
    //  This helper finds the exact stored reference first, then removes.  //
    // ==================================================================//

    private void removeByEquals(FlowSet<PredicateTree> set, PredicateTree target) {
        List<PredicateTree> toRemove = new ArrayList<>();
        Iterator<PredicateTree> it = set.iterator();
        while (it.hasNext()) {
            PredicateTree t = it.next();
            if (t.equals(target)) toRemove.add(t);
        }
        for (PredicateTree t : toRemove) set.remove(t);
    }

    // ==================================================================//
    //  Pre-pass: collect filter units and alias assignments               //
    // ==================================================================//

    private void prePass() {
        for (Unit u : unitChain) {
            collectPrintDfs(u);
            collectFilterOperations(u);
        }
    }

    private void collectPrintDfs(Unit u) {
        if (!(u instanceof CallExprStmt)) return;
        IExpr expr = ((CallExprStmt) u).getCallExpr();
        if (!(expr instanceof Call)) return;
        Call call = (Call) expr;
        if (call.getBaseName().equals("print")) {
            List<IExpr> args = call.getArgs();
            if (!args.isEmpty() && args.get(0) instanceof Name)
                PythonScene.printDfs.add(((Name) args.get(0)).getName());
        }
    }

    private void collectFilterOperations(Unit unit) {
        if (!(unit instanceof AssignmentStmtSoot)) return;
        AssignmentStmtSoot as  = (AssignmentStmtSoot) unit;
        AssignStmt         ast = as.getAssignStmt();
        IExpr              rhs = ast.getRHS();

        if (rhs instanceof Subscript) {
            IExpr filterExpr = ((Subscript) rhs).getSlice().getIndex();
            if (isFilterExpression(filterExpr)) {
                List<String> cols = extractColumnsFromFilter(filterExpr);
                if (!cols.isEmpty()) {
                    int lineno = (filterExpr instanceof Compare)
                            ? ((Compare) filterExpr).getLineno() : ast.lineno;
                    unitToPredMapping.putIfAbsent(unit, new PredicateModel(lineno, -1, cols));
                }
            }
        } else if (rhs instanceof Name) {
            List<Name> defined = as.getDataFramesDefined();
            List<Name> used    = as.getDataFramesUsed();
            if (!defined.isEmpty() && !used.isEmpty()) {
                String alias = defined.get(0).getName();
                String src   = used.get(0).getName();
                while (dfAliasMap.containsKey(src) && !dfAliasMap.get(src).equals(src))
                    src = dfAliasMap.get(src);
                dfAliasMap.put(alias, src);
            }
        }
    }

    private boolean isFilterExpression(IExpr expr) {
        return expr instanceof Compare || expr instanceof BinOp
                || expr instanceof UnaryOp || expr instanceof Call
                || expr instanceof Subscript;
    }

    // ==================================================================//
    //  Alias resolution                                                   //
    // ==================================================================//

    private String resolveAlias(String dfName) {
        String r = dfName;
        while (dfAliasMap.containsKey(r) && !dfAliasMap.get(r).equals(r))
            r = dfAliasMap.get(r);
        return r;
    }

    // ==================================================================//
    //  Tree building from filter expression                               //
    // ==================================================================//

    // ==================================================================//
    //  Constant resolution for predicate comparators                      //
    //                                                                     //
    //  resolveToConstant() attempts to reduce an expression to a single   //
    //  compile-time constant so it can be safely embedded in a parquet    //
    //  filter. Parquet/pyarrow evaluate filters at READ TIME and cannot   //
    //  resolve Python variable names, so only literal values can be used. //
    //                                                                     //
    //  RESOLVE if:                                                        //
    //    - expr is already a Constant literal                             //
    //    - expr is a Name with exactly one reaching definition that is    //
    //      itself resolvable, AND that variable is never assigned inside  //
    //      any loop enclosing the filter OR between its def and filter    //
    //    - expr is an arithmetic BinOp/UnaryOp where all operands resolve //
    //                                                                     //
    //  SKIP (return null) if:                                             //
    //    - variable has multiple reaching definitions (from if/else)      //
    //    - variable is assigned by a Call or complex expression           //
    //    - variable is a loop iteration variable                          //
    //    - variable is assigned anywhere inside a loop that could have    //
    //      mutated it between its definition and the filter statement     //
    // ==================================================================//

    /**
     * Try to resolve expr to a constant string value (the literal token).
     * Returns null if the expression cannot be safely resolved.
     * @param expr       the comparator expression (RHS of the comparison)
     * @param filterUnit the unit containing the filter statement
     */
    private String resolveToConstant(IExpr expr, Unit filterUnit) {
        if (expr instanceof Constant) {
            String raw = ((Constant) expr).getValue();
            // getValue() strips quotes from string literals (e.g. 'NY' → NY).
            // Re-add quotes if the constant is a string (toString has quotes).
            String str = expr.toString();
            if ((str.startsWith("'") && str.endsWith("'"))
                    || (str.startsWith("\"") && str.endsWith("\""))) {
                return str; // return with quotes preserved: 'NY'
            }
            return raw;   // numeric/boolean — no quotes needed
        }

        if (expr instanceof Name) {
            String varName = ((Name) expr).getName();
            return resolveNameToConstant(varName, filterUnit);
        }

        if (expr instanceof BinOp) {
            BinOp  b   = (BinOp) expr;
            String op  = b.getOp().getSymbol();
            // Only arithmetic operators are safe to evaluate at analysis time
            if (!op.equals("+") && !op.equals("-") && !op.equals("*")
                    && !op.equals("/") && !op.equals("//")
                    && !op.equals("%") && !op.equals("**"))
                return null;
            String L = resolveToConstant(b.getLeft(),  filterUnit);
            String R = resolveToConstant(b.getRight(), filterUnit);
            if (L == null || R == null) return null;
            return evalArith(L, op, R);
        }

        if (expr instanceof UnaryOp) {
            UnaryOp u  = (UnaryOp) expr;
            String  op = u.getOp().getSymbol();
            if (!op.equals("-") && !op.equals("+")) return null;
            String V = resolveToConstant(u.getLeft(), filterUnit);
            if (V == null) return null;
            try {
                double v = Double.parseDouble(V);
                double r = op.equals("-") ? -v : v;
                return r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r);
            } catch (NumberFormatException e) { return null; }
        }

        return null; // Call, Attribute, Subscript, etc. → unknown
    }

    /**
     * Resolve a named variable to a constant by walking the unit chain
     * backward from filterUnit to find its single reaching definition.
     * Returns null if the variable cannot be safely resolved.
     */
    /**
     * Resolve a variable name to its RHS IExpr (not just scalar constants).
     * Used for list variables: dept_list = ['HR','Sales'] → returns the ListComp.
     * Returns null if:
     *   - variable never assigned
     *   - variable assigned more than once (may vary in loop/branch)
     */
    private IExpr resolveNameToRhs(String varName, Unit filterUnit) {
        // Collect units before filterUnit
        List<Unit> before = new ArrayList<>();
        for (Unit u : unitChain) {
            if (u == filterUnit) break;
            before.add(u);
        }
        // Walk backward — find most recent assignment
        IExpr defRhs = null;
        for (int i = before.size() - 1; i >= 0; i--) {
            Unit u = before.get(i);
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as = (AssignmentStmtSoot) u;
            IExpr tgt = as.getAssignStmt().getTargets().get(0);
            if (tgt instanceof Name && ((Name) tgt).getName().equals(varName)) {
                defRhs = as.getAssignStmt().getRHS();
                break;
            }
        }
        if (defRhs == null) return null;
        // Safety: reject if assigned more than once
        int count = 0;
        for (Unit u : unitChain) {
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as = (AssignmentStmtSoot) u;
            IExpr tgt = as.getAssignStmt().getTargets().get(0);
            if (tgt instanceof Name && ((Name) tgt).getName().equals(varName)) count++;
        }
        if (count > 1) return null;
        return defRhs;
    }

    private String resolveNameToConstant(String varName, Unit filterUnit) {
        System.out.println("[resolveNameToConstant] looking for: " + varName);

        // Collect all units in forward order up to (not including) filterUnit
        List<Unit> before = new ArrayList<>();
        for (Unit u : unitChain) {
            if (u == filterUnit) break;
            before.add(u);
        }
        System.out.println("[resolveNameToConstant] units before filter: " + before.size());

        // Find all assignments to varName that are visible from filterUnit.
        // Walk backward to find the most recent one.
        Unit defUnit = null;
        IExpr defRhs = null;
        for (int i = before.size() - 1; i >= 0; i--) {
            Unit u = before.get(i);
            System.out.println("[resolveNameToConstant]   scanning unit class="
                    + u.getClass().getSimpleName() + " toString=" + u);
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as = (AssignmentStmtSoot) u;
            IExpr tgt = as.getAssignStmt().getTargets().get(0);
            System.out.println("[resolveNameToConstant]   unit=" + u
                    + " tgt=" + tgt + " tgtClass=" + tgt.getClass().getSimpleName()
                    + " rhs=" + as.getAssignStmt().getRHS()
                    + " rhsClass=" + as.getAssignStmt().getRHS().getClass().getSimpleName());
            if (tgt instanceof Name && ((Name) tgt).getName().equals(varName)) {
                defUnit = u;
                defRhs  = as.getAssignStmt().getRHS();
                System.out.println("[resolveNameToConstant]   FOUND def: rhs=" + defRhs
                        + " class=" + defRhs.getClass().getSimpleName());
                break;
            }
        }

        if (defUnit == null || defRhs == null) {
            System.out.println("[resolveNameToConstant] no def found for: " + varName);
            return null; // never assigned
        }

        // SAFETY CHECK: reject if varName is assigned inside any loop that
        // encloses filterUnit, or inside any loop between defUnit and filterUnit.
        // We detect loop assignments by checking if varName appears as an
        // assignment target inside a for-loop header's body range.
        // Simple conservative check: scan all units between defUnit and filterUnit.
        // If any of them assigns varName AND is inside a loop (detected by
        // checking if any loop-header precedes it with a back-edge past filterUnit),
        // skip. For simplicity we use the unitChain order and check if varName
        // has MORE THAN ONE assignment in the before-list — that implies it was
        // reassigned (e.g. inside a loop or branch) and is not a true constant.
        // Conservative safety check: if the variable is assigned more than once
        // anywhere in the method it may vary (reassigned in a loop or branch) → skip.
        int totalAssignCount = 0;
        for (Unit u : unitChain) {
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as = (AssignmentStmtSoot) u;
            IExpr tgt = as.getAssignStmt().getTargets().get(0);
            if (tgt instanceof Name && ((Name) tgt).getName().equals(varName))
                totalAssignCount++;
        }
        if (totalAssignCount > 1) {
            System.out.println("[resolveToConstant] " + varName
                    + " assigned " + totalAssignCount + " times → skip (may vary in loop)");
            return null;
        }

        // Recursively resolve the RHS of the single definition
        return resolveToConstant(defRhs, defUnit);
    }

    /** Evaluate a two-operand arithmetic expression on constant strings. */
    private String evalArith(String L, String op, String R) {
        try {
            double l = Double.parseDouble(L);
            double r = Double.parseDouble(R);
            double result;
            switch (op) {
                case "+":  result = l + r;              break;
                case "-":  result = l - r;              break;
                case "*":  result = l * r;              break;
                case "/":  result = l / r;              break;
                case "//": result = Math.floor(l / r);  break;
                case "%":  result = l % r;              break;
                case "**": result = Math.pow(l, r);     break;
                default:   return null;
            }
            // Return integer string if result is whole, else decimal
            return result == Math.floor(result) && !Double.isInfinite(result)
                    ? String.valueOf((long) result)
                    : String.valueOf(result);
        } catch (NumberFormatException e) {
            return null; // non-numeric constants (e.g. strings) → skip
        }
    }

    private PredicateTree buildTreeFromExpr(IExpr expr, String df_var, Unit unit) {

        if (expr instanceof UnaryOp) {
            UnaryOp u  = (UnaryOp) expr;
            String  op = u.getOp().getSymbol();
            if (op.equals("~") || op.equals("not")) {
                IExpr operand = u.getLeft();
                if (operand instanceof Subscript) {
                    IExpr idx = ((Subscript) operand).getSlice().getIndex();
                    if (idx instanceof Constant)
                        return new PredicateLeaf(new Predicate(
                                new Condition(((Constant) idx).getValue(), "==", "False"),
                                null, unit, df_var));
                }
                PredicateTree inner = buildTreeFromExpr(operand, df_var, unit);
                if (inner != null) return new PredicateNot(inner);
            }
        }

        if (expr instanceof BinOp) {
            BinOp         b   = (BinOp) expr;
            String        op  = b.getOp().getSymbol();
            PredicateTree lft = buildTreeFromExpr(b.getLeft(),  df_var, unit);
            PredicateTree rgt = buildTreeFromExpr(b.getRight(), df_var, unit);
            if (lft != null && rgt != null) {
                if (op.equals("|")) return new PredicateOr(lft, rgt);
                if (op.equals("&")) return new PredicateAnd(lft, rgt);
            }
        }

        else if (expr instanceof Compare) {
            Compare     comp = (Compare) expr;
            IExpr       lft  = comp.getLeft();
            String      op   = comp.getOps().get(0).getSymbol();
            List<IExpr> cmp  = comp.getComparators();

            if (lft instanceof Subscript) {
                IExpr idx = ((Subscript) lft).getSlice().getIndex();
                if (idx instanceof Constant && !cmp.isEmpty()) {
                    String col = ((Constant) idx).getValue();
                    // Try to resolve comparator to a constant (handles variables, x*5, etc.)
                    String val = resolveToConstant(cmp.get(0), unit);
                    if (val != null)
                        return new PredicateLeaf(new Predicate(
                                new Condition(col, op, val), null, unit, df_var));
                    // comparator is not a resolvable constant → skip this predicate
                    System.out.println("[buildTreeFromExpr] comparator not resolvable for col="
                            + col + " → skip predicate");
                }
            } else if (lft instanceof Attribute) {
                if (!cmp.isEmpty()) {
                    String col = ((Attribute) lft).getAttr();
                    String val = resolveToConstant(cmp.get(0), unit);
                    if (val != null)
                        return new PredicateLeaf(new Predicate(
                                new Condition(col, op, val), null, unit, df_var));
                    System.out.println("[buildTreeFromExpr] comparator not resolvable for col="
                            + col + " → skip predicate");
                }
            }
        }

        else if (expr instanceof Call) {
            Call call = (Call) expr;
            if (call.getFunc() instanceof Attribute) {
                Attribute fa = (Attribute) call.getFunc();
                if (fa.getAttr().equals("isin") && fa.getValue() instanceof Subscript) {
                    IExpr idx = ((Subscript) fa.getValue()).getSlice().getIndex();
                    if (idx instanceof Constant) {
                        String col = ((Constant) idx).getValue();
                        // isin args must all be Constant literals — if any arg is a
                        // variable or non-constant expression, parquet cannot evaluate
                        // the list at read time, so skip this predicate entirely.
                        // Resolve isin argument to a list of Constant elements.
                        // Three cases:
                        //   (a) isin(['HR','Sales'])  → single ListComp arg → unwrap via getElts()
                        //   (b) isin(dept_list)       → single Name arg     → resolve variable to ListComp
                        //   (c) anything else         → skip (not pushable)
                        List<IExpr> isinElts = null;
                        List<IExpr> rawArgs  = call.getArgs();
                        if (rawArgs.size() == 1) {
                            IExpr arg = rawArgs.get(0);
                            // Case (b): variable — resolve to its RHS definition
                            if (arg instanceof Name) {
                                String listVar = ((Name) arg).getName();
                                IExpr defRhs = resolveNameToRhs(listVar, unit);
                                if (defRhs instanceof ListComp) arg = defRhs;
                                // else stays as Name → allConstant will fail → skip
                            }
                            // Case (a) or resolved (b): ListComp → unwrap
                            if (arg instanceof ListComp) {
                                @SuppressWarnings("unchecked")
                                List<IExpr> elts = (List<IExpr>) ((ListComp) arg).getElts();
                                isinElts = elts;
                            }
                        }
                        if (isinElts == null) isinElts = rawArgs; // fallback — allConstant will fail
                        boolean allConstant = true;
                        for (IExpr a : isinElts) {
                            if (!(a instanceof Constant)) { allConstant = false; break; }
                        }
                        if (allConstant) {
                            // Store as "in" (pyarrow syntax) with values as
                            // comma-separated string — no outer brackets so
                            // condKey formats it as (col, in, v1, v2, ...)
                            StringBuilder vals = new StringBuilder("[");
                            for (IExpr a : isinElts) {
                                if (vals.length() > 1) vals.append(", ");
                                vals.append("'").append(((Constant) a).getValue()).append("'");
                            }
                            vals.append("]");
                            return new PredicateLeaf(new Predicate(
                                    new Condition(col, "in", vals.toString()),
                                    null, unit, df_var));
                        }
                        System.out.println("[buildTreeFromExpr] isin args not all Constant for col="
                                + col + " → skip predicate");
                    }
                }
            }
        }

        else if (expr instanceof Subscript) {
            IExpr idx = ((Subscript) expr).getSlice().getIndex();
            if (idx instanceof Constant)
                return new PredicateLeaf(new Predicate(
                        new Condition(((Constant) idx).getValue(), "==", "True"),
                        null, unit, df_var));
        }

        return null;
    }

    // ==================================================================//
    //  Kill helpers                                                       //
    // ==================================================================//

    private void killPredicatesForUsedDataframes(Unit u, FlowSet<PredicateTree> out) {
        if (!(u instanceof CallExprStmt)) return;
        IExpr expr = ((CallExprStmt) u).getCallExpr();
        if (!(expr instanceof Call)) return;

        Call        call         = (Call) expr;
        Set<String> consumingOps = new HashSet<>(Arrays.asList(
                "print", "to_csv", "to_parquet", "to_excel", "to_json",
                "to_sql", "to_hdf", "to_pickle", "to_html", "to_latex"));

        if (call.getFunc() instanceof Attribute) {
            Attribute fa = (Attribute) call.getFunc();
            if (consumingOps.contains(fa.getAttr()) && fa.getValue() instanceof Name)
                killPredicatesForDataframe(((Name) fa.getValue()).getName(), out, fa.getAttr());
        } else {
            String fn = call.getBaseName();
            if (consumingOps.contains(fn))
                for (IExpr arg : call.getArgs())
                    if (arg instanceof Name)
                        killPredicatesForDataframe(((Name) arg).getName(), out, fn);
        }
    }

    private void killPredicatesForDataframe(String dfName,
                                            FlowSet<PredicateTree> out,
                                            String operation) {
        String resolved = resolveAlias(dfName);
        System.out.println("[Kill] '" + resolved + "' consumed by " + operation);
        List<PredicateTree> toKill = new ArrayList<>();
        Iterator<PredicateTree> it = out.iterator();
        while (it.hasNext()) {
            PredicateTree t = it.next();
            String df = extractDataframeName(t);
            if (df != null && resolveAlias(df).equals(resolved)) toKill.add(t);
        }
        for (PredicateTree t : toKill) {
            removeByEquals(out, t);
            System.out.println("  Killed tree for df=" + resolved);
        }
    }

    // ==================================================================//
    //  getKillSet                                                         //
    //                                                                     //
    //  columnFilteredReplacements is passed in as a LOCAL variable from   //
    //  flowThrough() — NOT an instance field. If it were an instance      //
    //  field, concurrent flowThrough() calls during loop iteration would  //
    //  corrupt each other's state.                                        //
    // ==================================================================//

    private FlowSet<PredicateTree> getKillSet(Unit unit,
                                              FlowSet<PredicateTree> outSet,
                                              FlowSet<PredicateTree> columnFilteredReplacements) {
        FlowSet<PredicateTree> killSet = new ArraySparseSet<>();
        if (!(unit instanceof AssignmentStmtSoot)) return killSet;

        AssignmentStmtSoot as  = (AssignmentStmtSoot) unit;
        AssignStmt         ast = as.getAssignStmt();

        List<Name> definedDfs = as.getDataFramesDefined();
        if (definedDfs.isEmpty()) return killSet;

        String assignedDf       = definedDfs.get(0).getName();
        IExpr  target           = ast.getTargets().get(0);
        List<Name> usedDfs      = as.getDataFramesUsed();
        String sourceDf         = usedDfs.isEmpty() ? assignedDf : usedDfs.get(0).getName();
        String resolvedSourceDf = resolveAlias(sourceDf);

        String modifiedColumn = null;
        if (target instanceof Subscript) {
            IExpr idx = ((Subscript) target).getSlice().getIndex();
            if (idx instanceof Constant) modifiedColumn = ((Constant) idx).getValue();
        }

        boolean isFilter        = unitToPredMapping.containsKey(unit);
        boolean isRowPreserving = isRowPreserving(as, assignedDf);

        System.out.println("[getKillSet] " + unit);
        System.out.println("  assigned=" + assignedDf + " source=" + resolvedSourceDf
                + " isFilter=" + isFilter + " rowPreserving=" + isRowPreserving
                + " modifiedCol=" + modifiedColumn);

        if (!isFilter && isDataSource(unit)) {
            // df = read_csv(...) / read_parquet(...) — hard lifetime boundary.
            // Any predicates for 'df' in the flow belong to a LATER (in forward
            // order) lifetime of that variable. Kill them so they don't bleed
            // backward into an earlier chain that happens to reuse the same name.
            // rewriteDataSource uses getFlowBefore(unit), which is computed
            // BEFORE this kill fires, so the second chain's predicates are
            // safely extracted before being cleared here.
            System.out.println("  KILL: data source re-assignment clears prior df chain");
            Iterator<PredicateTree> it = outSet.iterator();
            while (it.hasNext()) {
                PredicateTree t  = it.next();
                String        df = extractDataframeName(t);
                if (df != null && resolveAlias(df).equals(resolveAlias(assignedDf)))
                    killSet.add(t);
            }
        } else if (!isFilter && !isRowPreserving) {
            // Non-row-preserving op (e.g. groupby) — kill all preds for this df
            System.out.println("  KILL: non-row-preserving");
            Iterator<PredicateTree> it = outSet.iterator();
            while (it.hasNext()) {
                PredicateTree t  = it.next();
                String        df = extractDataframeName(t);
                if (df != null && (df.equals(assignedDf)
                        || resolveAlias(df).equals(resolvedSourceDf))) {
                    killSet.add(t);
                    System.out.println("    kill df=" + df);
                }
            }
        } else if (!isFilter && isRowPreserving && modifiedColumn != null) {
            // Column assignment — kill only preds that reference the modified column
            System.out.println("  KILL: column modification on " + modifiedColumn);
            Iterator<PredicateTree> it = outSet.iterator();
            while (it.hasNext()) {
                PredicateTree t  = it.next();
                String        df = extractDataframeName(t);
                if (df != null && df.equals(assignedDf)
                        && treeContainsColumn(t, modifiedColumn)) {
                    killSet.add(t);
                    PredicateTree filtered = filterColumnFromTree(t, modifiedColumn);
                    if (filtered != null) {
                        columnFilteredReplacements.add(filtered);
                        System.out.println("    filtered col '" + modifiedColumn + "'");
                    } else {
                        System.out.println("    entire tree removed (all on '" + modifiedColumn + "')");
                    }
                }
            }
        }
        return killSet;
    }

    // ==================================================================//
    //  flowThrough — backward transfer function                           //
    //                                                                     //
    //  For each filter statement: df2 = df1[expr]                        //
    //    - Build a PredicateTree from expr                                //
    //    - AND it with any existing tree for df1 (sequential filters)     //
    //    - OR it with any existing tree for df1 (if/else merge handled    //
    //      in merge())                                                    //
    //                                                                     //
    //  LOOP CONVERGENCE:                                                  //
    //    treeSignature() compares trees by their flattened DNF signature  //
    //    rather than structural nesting. When AND/OR would produce a tree  //
    //    with the same signature as what's already in the set, we skip    //
    //    creating the new node — leaving the flow set completely          //
    //    untouched. Soot's fixed-point check (in PredicateFlowSet.equals) //
    //    then detects no change and stops iterating.                      //
    // ==================================================================//

    @Override
    protected void flowThrough(FlowSet<PredicateTree> in,
                               Unit u,
                               FlowSet<PredicateTree> out) {
        currentFlowUnit = u;
        in.copy(out);
        killPredicatesForUsedDataframes(u, out);

        if (!(u instanceof AssignmentStmtSoot)) {
            handleAliasOperation(u);
            return;
        }

        AssignmentStmtSoot as         = (AssignmentStmtSoot) u;
        IExpr              rhs        = as.getAssignStmt().getRHS();
        List<Name>         definedDfs = as.getDataFramesDefined();
        List<Name>         usedDfs    = as.getDataFramesUsed();

        // Capture predicates for data-source nodes BEFORE any kill fires.
        // Keyed by Unit (not by df name) so two read_csv calls that assign
        // the same variable are stored independently.
        // We ALWAYS write an entry — null when no predicate is found —
        // so a later read_csv for the same df name cannot inherit a stale
        // entry left by an earlier read_csv further down in the program.
        if (isDataSource(u) && !definedDfs.isEmpty()) {
            String srcDf = resolveAlias(definedDfs.get(0).getName());
            PredicateTree captured = null;
            Iterator<PredicateTree> pit = out.iterator();
            while (pit.hasNext()) {
                PredicateTree t  = pit.next();
                String        dn = extractDataframeName(t);
                if (dn != null && resolveAlias(dn).equals(srcDf)) {
                    captured = t;
                    break;
                }
            }
            // Always put — even null — so this Unit's entry is definitive
            predicateTrees.put(u, captured);
            System.out.println("[flowThrough] captured predicates for " + srcDf
                    + " at data source: " + (captured != null ? captured.toDebugString(0) : "null (filters=None)"));
        }

        // Kill set — columnFilteredReplacements is LOCAL to this call
        FlowSet<PredicateTree> columnFilteredReplacements = new ArraySparseSet<>();
        FlowSet<PredicateTree> killSet = getKillSet(u, out, columnFilteredReplacements);
        for (PredicateTree killed : killSet)         removeByEquals(out, killed);
        Iterator<PredicateTree> cfIt = columnFilteredReplacements.iterator();
        while (cfIt.hasNext()) {
            out.add(cfIt.next());
            System.out.println("  Added back filtered tree");
        }

        // Filter statement: df2 = df1[filterExpr]
        if (!(rhs instanceof Subscript)) {
            handleAliasOperation(u);
            return;
        }

        IExpr filterExpr = ((Subscript) rhs).getSlice().getIndex();
        if (!isFilterExpression(filterExpr) || usedDfs.isEmpty() || definedDfs.isEmpty()) {
            handleAliasOperation(u);
            return;
        }

        String  sourceDf         = usedDfs.get(0).getName();
        String  targetDf         = definedDfs.get(0).getName();
        String  resolvedSourceDf = resolveAlias(sourceDf);
        String  resolvedTargetDf = resolveAlias(targetDf);
        boolean isSameDataframe  = resolvedTargetDf.equals(resolvedSourceDf);

        System.out.println("[flowThrough] " + targetDf + " = " + sourceDf + "[...]");

        // If different df: kill old preds for targetDf first
        if (!isSameDataframe) {
            List<PredicateTree> toKill = new ArrayList<>();
            Iterator<PredicateTree> ki = out.iterator();
            while (ki.hasNext()) {
                PredicateTree t = ki.next();
                if (targetDf.equals(extractDataframeName(t))) toKill.add(t);
            }
            toKill.forEach(t -> removeByEquals(out, t));
        }

        PredicateTree newTree = buildTreeFromExpr(filterExpr, resolvedSourceDf, u);
        if (newTree == null) {
            handleAliasOperation(u);
            return;
        }

        System.out.println("  newTree: " + newTree.toDebugString(0));

        // Find existing tree for source df
        PredicateTree existing = null;
        Iterator<PredicateTree> it = out.iterator();
        while (it.hasNext()) {
            PredicateTree t = it.next();
            if (resolvedSourceDf.equals(resolveAlias(extractDataframeName(t)))) {
                existing = t;
                break;
            }
        }

        if (existing == null) {
            // First filter seen for this df
            out.add(newTree);
            System.out.println("  First tree for df=" + resolvedSourceDf);
        } else if (isSameDataframe) {
            // df = df[filter] — AND the new filter with existing
            //
            // CONVERGENCE GUARD 1 — exact sig match:
            //   AND(newTree, existing) has same DNF as existing → newTree
            //   adds nothing structurally new.
            //
            // CONVERGENCE GUARD 2 — dnfSubsumes(existing, newTree):
            //   Every group of newTree is already a subset of some group in
            //   existing. This fires in loop back-edge situations where the
            //   existing tree (built from a prior merge) already contains
            //   newTree's condition as one of its OR-alternatives. AND-ing
            //   it again would incorrectly strengthen the predicate.
            //
            //   Example: existing=OR(age>30, city==NY), newTree=LEAF(age>30)
            //   {age>30} ⊆ {age>30} → subsumes → leave untouched.
            //   Without this guard: AND(age>30, OR(age>30,city==NY)) which
            //   flattens to [{age>30},{age>30,city==NY}] — wrong and grows.
            //
            // In both cases we leave 'out' completely untouched so that
            // Soot's fixed-point check (PredicateFlowSet.equals) sees no
            // change and stops iterating.
            PredicateTree candidate = new PredicateAnd(newTree, existing);
            if (sig(candidate).equals(sig(existing))) {
                System.out.println("  AND unchanged (exact sig) — leaving out untouched");
            } else if (dnfSubsumes(existing, newTree)) {
                System.out.println("  AND skipped (existing subsumes newTree) — leaving out untouched");
            } else {
                removeByEquals(out, existing);
                out.add(candidate);
                System.out.println("  AND-ed: " + candidate.toDebugString(0));
            }
        } else {
            // df2 = df1[filter] — OR the new filter with existing (different-df path)
            PredicateTree candidate = new PredicateOr(newTree, existing);
            if (sig(candidate).equals(sig(existing))) {
                System.out.println("  OR unchanged — leaving out untouched");
            } else {
                removeByEquals(out, existing);
                out.add(candidate);
                System.out.println("  OR-ed: " + candidate.toDebugString(0));
            }
        }

        handleAliasOperation(u);
    }

    // ==================================================================//
    //  merge — join point for if/else branches and loop back-edges        //
    //                                                                     //
    //  This is an OPTIMISTIC analysis: we collect all filters seen on     //
    //  any path and push as much as possible to read_csv/read_parquet.   //
    //  The Python filters always run after parquet, so over-broad         //
    //  pushdown is safe; only over-narrow pushdown causes wrong results.  //
    //                                                                     //
    //  RULE 1 — empty side → keep non-empty (optimistic):               //
    //    An empty flow means "no filter seen on this path". We treat it   //
    //    as transparent and keep what the other path found.               //
    //    This allows a post-loop if(A) filter to flow up to the loop      //
    //    header so Rule 2 can fire.                                       //
    //                                                                     //
    //  RULE 2 — subsumption → keep the WEAKER (less restrictive) tree:   //
    //    If t1 subsumes t2 (every DNF group of t2 is a subset of some    //
    //    group of t1), t1 is MORE restrictive. The loop-exit path (which  //
    //    may skip the loop entirely) carries the weaker tree; we keep it  //
    //    so we never push a filter that wasn’t applied on every path.    //
    //                                                                     //
    //    Example (for: df=df[A]; if(B): df=df[B]):                       //
    //      loop body  → AND(A, B)  groups [{A,B}]                        //
    //      loop exit  → LEAF(B)    groups [{B}]                          //
    //      {B} ⊆ {A,B} → AND subsumes LEAF → keep LEAF(B)             //
    //      Final pushdown: [[(B)]]                                        //
    //                                                                     //
    //  RULE 3 — otherwise → OR (genuinely different branches):           //
    //    if(A) else(B) → merge produces OR(A,B).                         //
    // ==================================================================//

    // ==================================================================//
    //  Transparent loop detection                                          //
    //                                                                      //
    //  A loop is "transparent" for df-predicate propagation if its body   //
    //  contains NO assignment to any tracked dataframe variable.           //
    //  For such loops the empty back-edge means "df flows through          //
    //  unchanged", not "any row is valid on this path" — so the           //
    //  loop-exit predicate should propagate through the loop header        //
    //  rather than being killed by the empty back-edge.                    //
    // ==================================================================//

    /**
     * Pre-compute the set of loop headers whose loop bodies contain no
     * assignment to any dataframe variable. Uses Soot's back-edge detection:
     * a back-edge (t→h) means h is a loop header and all nodes dominated
     * by h (up to t) form the loop body.
     */
    private Set<Unit> computeTransparentLoopHeaders() {
        Set<Unit> result = new HashSet<>();

        // Collect df variable names: any variable assigned via df = df[...]
        Set<String> dfNames = new HashSet<>();
        for (Unit u : unitChain) {
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as = (AssignmentStmtSoot) u;
            IExpr tgt = as.getAssignStmt().getTargets().get(0);
            if (!(tgt instanceof Name)) continue;
            String varName = ((Name) tgt).getName();
            IExpr rhs = as.getAssignStmt().getRHS();
            if (rhs instanceof Subscript) {
                IExpr base = ((Subscript) rhs).getValue();
                if (base instanceof Name && ((Name) base).getName().equals(varName))
                    dfNames.add(varName);
            }
        }
        System.out.println("[transparentLoop] tracked df names: " + dfNames);

        // Build ordered unit list for index-based range scans
        List<Unit> chainList = new ArrayList<>();
        for (Unit u : unitChain) chainList.add(u);
        Map<Unit, Integer> order = new HashMap<>();
        for (int i = 0; i < chainList.size(); i++) order.put(chainList.get(i), i);

        // Identify loop headers directly from the IR:
        // Strategy:
        // 1. Find IfStmt whose toString starts with "for " — the loop header.
        // 2. Find the back-edge: any unit at ti > hi that has a successor
        //    at index <= hi (jumps backward). The back-edge may target a
        //    NopStmt just before the IfStmt, not the IfStmt itself — so
        //    use index comparison rather than object identity.
        // 3. Scan body (hi+1 .. backEdgeIdx) for df filter assignments.
        // 4. Add the IfStmt object (from chainList — same instance Soot uses)
        //    to result so Set.contains() works correctly in mergeInto.
        for (int hi = 0; hi < chainList.size(); hi++) {
            Unit u = chainList.get(hi);
            String uStr = u.toString();
            if (uStr == null || (!uStr.startsWith("for ") && !uStr.startsWith("while "))) continue;
            System.out.println("[transparentLoop] found for-loop header at hi=" + hi + ": " + uStr);

            // Find back-edge by index: scan forward for unit whose
            // successor has a lower index (jumps backward to/before hi).
            int backEdgeIdx = -1;
            for (int ti = hi + 1; ti < chainList.size(); ti++) {
                Unit candidate = chainList.get(ti);
                for (Unit succ : unitGraph.getSuccsOf(candidate)) {
                    Integer succIdx = order.get(succ);
                    if (succIdx != null && succIdx <= hi) {
                        backEdgeIdx = ti;
                        System.out.println("[transparentLoop]   back-edge at ti=" + ti
                                + " → idx=" + succIdx);
                        break;
                    }
                }
                if (backEdgeIdx != -1) break;
                // Do NOT break on nested loop headers — the index-based
                // back-edge check (succIdx <= hi) already self-corrects:
                // inner back-edges jump to inner headers (succIdx > hi) and
                // are ignored; only the outer back-edge has succIdx <= hi.
            }

            if (backEdgeIdx == -1) {
                System.out.println("[transparentLoop]   no back-edge found, skip");
                continue;
            }

            // Scan loop body for df filter assignments
            boolean dfTouchedInBody = false;
            for (int bi = hi + 1; bi <= backEdgeIdx; bi++) {
                Unit bodyUnit = chainList.get(bi);
                if (!(bodyUnit instanceof AssignmentStmtSoot)) continue;
                AssignmentStmtSoot as = (AssignmentStmtSoot) bodyUnit;
                IExpr tgt = as.getAssignStmt().getTargets().get(0);
                if (tgt instanceof Name && dfNames.contains(((Name) tgt).getName())) {
                    dfTouchedInBody = true;
                    System.out.println("[transparentLoop]   df touched: " + bodyUnit);
                    break;
                }
            }

            if (!dfTouchedInBody) {
                result.add(u); // same object Soot passes to mergeInto
                System.out.println("[transparentLoop] → TRANSPARENT: " + u);
            } else {
                System.out.println("[transparentLoop] → NOT transparent");
            }
        }
        return result;
    }

    @Override
    protected void mergeInto(Unit succNode,
                             FlowSet<PredicateTree> inout,
                             FlowSet<PredicateTree> in) {
        // Set currentFlowUnit so merge() knows which node is being joined at.
        // Soot calls mergeInto(node, accumulated, new) at each join point —
        // node is the unit whose in-flow is being computed.
        currentFlowUnit = succNode;
        System.out.println("[mergeInto] succNode=" + succNode
                + " class=" + (succNode != null ? succNode.getClass().getSimpleName() : "null")
                + " isTransparent=" + transparentLoopHeaders.contains(succNode));
        super.mergeInto(succNode, inout, in);
    }

    @Override
    protected void merge(FlowSet<PredicateTree> in1,
                         FlowSet<PredicateTree> in2,
                         FlowSet<PredicateTree> out) {

        // RULE 1a: both sides empty → output empty.
        if (in1.isEmpty() && in2.isEmpty()) { out.clear(); return; }

        // RULE 1b: one side empty.
        // Normal case: {} means "no filter on this path — every row valid"
        //   → {} dominates, output {}.
        // Exception: if the current node is a TRANSPARENT loop header
        //   (loop body contains no df assignment), the empty side is the
        //   back-edge carrying "df flows through unchanged" not "unconstrained".
        //   In that case the loop-exit predicate should pass through.
        if (in1.isEmpty() || in2.isEmpty()) {
            System.out.println("  [merge] one side empty. currentFlowUnit="
                    + currentFlowUnit + " transparent=" + transparentLoopHeaders.contains(currentFlowUnit)
                    + " in1.size=" + in1.size() + " in2.size=" + in2.size());
            if (currentFlowUnit != null && transparentLoopHeaders.contains(currentFlowUnit)) {
                // Transparent loop: use whichever side is non-empty
                FlowSet<PredicateTree> nonEmpty = in1.isEmpty() ? in2 : in1;
                nonEmpty.copy(out);
                System.out.println("  [merge] transparent loop header — passing through non-empty side");
                return;
            }
            // Normal case: {} dominates
            out.clear();
            return;
        }

        // Build df -> tree maps for each branch
        Map<String, PredicateTree> map1 = new HashMap<>();
        Map<String, PredicateTree> map2 = new HashMap<>();
        Iterator<PredicateTree> it1 = in1.iterator();
        while (it1.hasNext()) { PredicateTree t = it1.next(); String df = extractDataframeName(t); if (df != null) map1.put(resolveAlias(df), t); }
        Iterator<PredicateTree> it2 = in2.iterator();
        while (it2.hasNext()) { PredicateTree t = it2.next(); String df = extractDataframeName(t); if (df != null) map2.put(resolveAlias(df), t); }

        Set<String> allDfs = new HashSet<>();
        allDfs.addAll(map1.keySet());
        allDfs.addAll(map2.keySet());
        out.clear();

        for (String df : allDfs) {
            PredicateTree t1 = map1.get(df);
            PredicateTree t2 = map2.get(df);

            // One branch has no tree for this df → that path is unconstrained.
            // Same logic as Rule 1: {} dominates, so output nothing for this df.
            if (t1 == null || t2 == null) { continue; }

            if (sig(t1).equals(sig(t2))) {
                // Identical on both paths
                out.add(t1);
                System.out.println("  [merge] identical for df=" + df);
            } else if (dnfSubsumes(t1, t2)) {
                // t1 is more restrictive than t2 → keep t2 (weaker/broader)
                // Correct join: t2's guarantee is all we can claim on every path
                out.add(t2);
                System.out.println("  [merge] t1 subsumes t2 for df=" + df + " (kept t2, weaker)");
            } else if (dnfSubsumes(t2, t1)) {
                // t2 is more restrictive than t1 → keep t1 (weaker/broader)
                out.add(t1);
                System.out.println("  [merge] t2 subsumes t1 for df=" + df + " (kept t1, weaker)");
            } else {
                // Genuinely different — OR them (e.g. if/else with different filters)
                out.add(new PredicateOr(t1, t2));
                System.out.println("  [merge] OR-ed for df=" + df);
            }
        }
    }

    /**
     * Returns true if t1 subsumes t2 in DNF:
     * every group of t2 is a SUBSET of at least one group of t1.
     *
     * In predicate terms: t1's conditions are a superset of t2's,
     * meaning t1 is more restrictive and already implies t2.
     *
     * Example:
     *   t1 groups: [{age>30, city==NY}]
     *   t2 groups: [{city==NY}]
     *   {city==NY} ⊆ {age>30, city==NY} → true → t1 subsumes t2
     */
    private boolean dnfSubsumes(PredicateTree t1, PredicateTree t2) {
        List<Set<Predicate>> groups1 = t1.flatten();
        List<Set<Predicate>> groups2 = t2.flatten();

        // Pre-compute condition-key sets for each group of t1
        List<Set<String>> keyGroups1 = new ArrayList<>();
        for (Set<Predicate> g : groups1) {
            Set<String> keys = new HashSet<>();
            for (Predicate p : g)
                keys.add(p.getDf_var() + ":(" + p.getCond().getOn()
                        + "," + p.getCond().getOp()
                        + "," + p.getCond().getWith() + ")");
            keyGroups1.add(keys);
        }

        // Every group of t2 must be a subset of some group of t1
        for (Set<Predicate> g2 : groups2) {
            Set<String> keys2 = new HashSet<>();
            for (Predicate p : g2)
                keys2.add(p.getDf_var() + ":(" + p.getCond().getOn()
                        + "," + p.getCond().getOp()
                        + "," + p.getCond().getWith() + ")");

            boolean foundSupergroup = false;
            for (Set<String> keys1 : keyGroups1) {
                if (keys1.containsAll(keys2)) {
                    foundSupergroup = true;
                    break;
                }
            }
            if (!foundSupergroup) return false;
        }
        return true;
    }

    // Shorthand to keep flowThrough/merge readable
    private String sig(PredicateTree tree) {
        return PredicateFlowSet.signature(tree);
    }

    // ==================================================================//
    //  Alias handler                                                      //
    // ==================================================================//

    private void handleAliasOperation(Unit unit) {
        if (!(unit instanceof AssignmentStmtSoot)) return;
        AssignmentStmtSoot as  = (AssignmentStmtSoot) unit;
        if (!(as.getAssignStmt().getRHS() instanceof Name)) return;
        List<Name> defined = as.getDataFramesDefined();
        List<Name> used    = as.getDataFramesUsed();
        if (!defined.isEmpty() && !used.isEmpty())
            dfAliasMap.put(defined.get(0).getName(), resolveAlias(used.get(0).getName()));
    }

    // ==================================================================//
    //  Utility                                                            //
    // ==================================================================//

    private boolean isDataSource(Unit unit) {
        if (!(unit instanceof AssignmentStmtSoot)) return false;
        IExpr rhs = ((AssignmentStmtSoot) unit).getAssignStmt().getRHS();
        if (!(rhs instanceof Call)) return false;
        IExpr func = ((Call) rhs).getFunc();
        if (!(func instanceof Attribute)) return false;
        String op = ((Attribute) func).getAttr();
        return op.equals("read_csv") || op.equals("read_parquet");
    }

    private boolean isRowPreserving(AssignmentStmtSoot as, String dfName) {
        IExpr rhs = as.getAssignStmt().getRHS();
        if (!(rhs instanceof Call)) return true;
        IExpr func = ((Call) rhs).getFunc();
        if (!(func instanceof Attribute)) return true;
        return !PythonScene.nonRowPreservingOps.contains(((Attribute) func).getAttr());
    }

    private String extractDataframeName(PredicateTree tree) {
        return tree == null ? null : tree.getDataframeName();
    }

    // ==================================================================//
    //  Column tree helpers                                                //
    // ==================================================================//

    private boolean treeContainsColumn(PredicateTree tree, String col) {
        if (tree instanceof PredicateLeaf)
            return ((PredicateLeaf) tree).predicate.getCond().getOn().equals(col);
        if (tree instanceof PredicateNot)
            return treeContainsColumn(((PredicateNot) tree).child, col);
        if (tree instanceof PredicateAnd)
            return ((PredicateAnd) tree).children.stream().anyMatch(c -> treeContainsColumn(c, col));
        if (tree instanceof PredicateOr)
            return ((PredicateOr) tree).children.stream().anyMatch(c -> treeContainsColumn(c, col));
        return false;
    }

    private PredicateTree filterColumnFromTree(PredicateTree tree, String col) {
        if (tree instanceof PredicateLeaf)
            return ((PredicateLeaf) tree).predicate.getCond().getOn().equals(col) ? null : tree;
        if (tree instanceof PredicateNot) {
            PredicateTree f = filterColumnFromTree(((PredicateNot) tree).child, col);
            return f == null ? null : new PredicateNot(f);
        }
        if (tree instanceof PredicateAnd) {
            List<PredicateTree> fc = new ArrayList<>();
            for (PredicateTree c : ((PredicateAnd) tree).children) {
                PredicateTree f = filterColumnFromTree(c, col);
                if (f != null) fc.add(f);
            }
            if (fc.isEmpty()) return null;
            return fc.size() == 1 ? fc.get(0) : new PredicateAnd(fc);
        }
        if (tree instanceof PredicateOr) {
            List<PredicateTree> fc = new ArrayList<>();
            for (PredicateTree c : ((PredicateOr) tree).children) {
                PredicateTree f = filterColumnFromTree(c, col);
                if (f != null) fc.add(f);
            }
            if (fc.isEmpty()) return null;
            return fc.size() == 1 ? fc.get(0) : new PredicateOr(fc);
        }
        return tree;
    }

    // ==================================================================//
    //  Column extraction from filter expression                           //
    // ==================================================================//

    private List<String> extractColumnsFromFilter(IExpr expr) {
        List<String> cols = new ArrayList<>();
        if (expr instanceof Compare) {
            Compare c = (Compare) expr;
            if (c.getLeft() instanceof Subscript) {
                IExpr idx = ((Subscript) c.getLeft()).getSlice().getIndex();
                if (idx instanceof Constant) cols.add(((Constant) idx).getValue());
            } else if (c.getLeft() instanceof Attribute) {
                cols.add(((Attribute) c.getLeft()).getAttr());
            }
        } else if (expr instanceof BinOp) {
            cols.addAll(extractColumnsFromFilter(((BinOp) expr).getLeft()));
            cols.addAll(extractColumnsFromFilter(((BinOp) expr).getRight()));
        } else if (expr instanceof UnaryOp) {
            cols.addAll(extractColumnsFromFilter(((UnaryOp) expr).getLeft()));
        } else if (expr instanceof Call) {
            Call call = (Call) expr;
            if (call.getFunc() instanceof Attribute) {
                IExpr val = ((Attribute) call.getFunc()).getValue();
                if (val instanceof Subscript) {
                    IExpr idx = ((Subscript) val).getSlice().getIndex();
                    if (idx instanceof Constant) cols.add(((Constant) idx).getValue());
                }
            }
        } else if (expr instanceof Subscript) {
            IExpr idx = ((Subscript) expr).getSlice().getIndex();
            if (idx instanceof Constant) cols.add(((Constant) idx).getValue());
        }
        return cols;
    }

    // ==================================================================//
    //  Final tree extraction                                              //
    // ==================================================================//

    private void extractFinalTrees() {
        System.out.println("\n========== EXTRACTING FINAL TREES ==========");
        List<Unit> heads = unitGraph.getHeads();
        if (heads.isEmpty()) return;

        FlowSet<PredicateTree> entryFlow = getFlowBefore(heads.get(0));
        System.out.println("Entry flow size=" + entryFlow.size());

        // All data sources are now captured directly in flowThrough (keyed by Unit).
        // The entry flow here is only used as a fallback for programs where
        // the analysis somehow didn't encounter the read_csv node (rare edge case).
        // In normal operation this loop adds nothing new.
        Iterator<PredicateTree> it = entryFlow.iterator();
        while (it.hasNext()) {
            PredicateTree tree = it.next();
            String        df   = extractDataframeName(tree);
            if (df != null) {
                System.out.println("Entry flow has df=" + resolveAlias(df) + " (handled via Unit key)");
            }
        }
        System.out.println("========== END EXTRACTION ==========\n");
    }

    private void printFinalPredicateTrees() {
        System.out.println("\n=== FINAL PREDICATE TREES ===");
        for (Map.Entry<Unit, PredicateTree> e : predicateTrees.entrySet()) {
            String dfName = e.getValue() != null ? extractDataframeName(e.getValue()) : "?";
            System.out.println("DataSource: " + e.getKey() + "  df=" + dfName);
            if (e.getValue() != null) System.out.println(e.getValue().toDebugString(1));
            else System.out.println("  (None)");
        }
    }

    // ==================================================================//
    //  insertFilters / rewriteDataSource                                  //
    // ==================================================================//

    public void insertFilters() {
        JPUnitGraph ug   = new CFG(jpMethod).getUnitGraph();
        Unit        unit = ug.getHeads().get(0);
        do {
            if (isDataSource(unit)) {
                String df = ((IStmt) unit).getDataFramesDefined().get(0).getName();
                rewriteDataSource(unit, df);
            }
            unit = ug.getSuccsOf(unit).isEmpty() ? null : ug.getSuccsOf(unit).get(0);
        } while (unit != null);
    }

    private void rewriteDataSource(Unit u, String df) {
        if (!(u instanceof AssignmentStmtSoot)) return;
        AssignmentStmtSoot as  = (AssignmentStmtSoot) u;
        IExpr              rhs = as.getAssignStmt().getRHS();
        if (!(rhs instanceof Call)) return;

        Call          call      = (Call) rhs;
        List<Keyword> kws       = call.getKeywords();
        int    lno        = call.getLineno();
        int    col_offset = call.getCol_offset();
        String predicates = "None";

        String        resolvedDf = resolveAlias(df);
        // Look up by Unit — each read_csv has its own independent entry
        PredicateTree tree       = predicateTrees.get(u);

        System.out.println("[rewriteDataSource] df=" + df);

        if (tree != null) {
            System.out.println("  Tree:\n" + tree.toDebugString(2));
            List<Set<Predicate>> groups = simplifyGroups(tree.flatten());

            if (!groups.isEmpty()) {
                for (int i = 0; i < groups.size(); i++)
                    System.out.println("  Group " + i + ": "
                            + groups.get(i).stream().map(p -> condKey(p.getCond()))
                            .collect(Collectors.toList()));

                predicates = formatGroups(groups);
                System.out.println("  Pushing down: " + predicates);

                int totalFilters = countTotalFiltersForDataframe(df);
                if (totalFilters == 1) {
                    Map<Unit, Set<String>> filterUnits    = getFilterUnitsAndColumns(df);
                    Map<Unit, Set<String>> pushedDown     = new HashMap<>();
                    for (Set<Predicate> g : groups)
                        for (Predicate p : g)
                            pushedDown.computeIfAbsent(p.getUnit(), k -> new HashSet<>())
                                    .add(p.getCond().getOn());

                    boolean allPushed = filterUnits.entrySet().stream()
                            .allMatch(e -> pushedDown
                                    .getOrDefault(e.getKey(), Collections.emptySet())
                                    .containsAll(e.getValue()));

                    if (allPushed) {
                        outer:
                        for (Set<Predicate> g : groups)
                            for (Predicate p : g)
                                if (p.getUnit() instanceof AssignmentStmtSoot) {
                                    AssignmentStmtSoot fa  = (AssignmentStmtSoot) p.getUnit();
                                    AssignStmt         ast = fa.getAssignStmt();
                                    ast.setRHS(new Name(ast.lineno, ast.col_offset, df, null));
                                    System.out.println("  Removed filter at " + p.getUnit());
                                    break outer;
                                }
                    }
                }
            }
        } else {
            System.out.println("  No tree for df=" + df);
        }

        kws.add(new Keyword("filters", new Constant(lno, col_offset, predicates)));
        call.setKeywords(kws);
        System.out.println("  filters=" + predicates);
    }

    private Map<Unit, Set<String>> getFilterUnitsAndColumns(String dfName) {
        Map<Unit, Set<String>> result = new HashMap<>();
        String resolved = resolveAlias(dfName);
        for (Unit u : unitChain) {
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as  = (AssignmentStmtSoot) u;
            IExpr              rhs = as.getAssignStmt().getRHS();
            if (!(rhs instanceof Subscript)) continue;
            IExpr fe = ((Subscript) rhs).getSlice().getIndex();
            if (!isFilterExpression(fe)) continue;
            List<Name> used = as.getDataFramesUsed();
            if (!used.isEmpty() && resolveAlias(used.get(0).getName()).equals(resolved))
                result.put(u, new HashSet<>(extractColumnsFromFilter(fe)));
        }
        return result;
    }

    private int countTotalFiltersForDataframe(String dfName) {
        int count = 0;
        String resolved = resolveAlias(dfName);
        for (Unit u : unitChain) {
            if (!(u instanceof AssignmentStmtSoot)) continue;
            AssignmentStmtSoot as  = (AssignmentStmtSoot) u;
            IExpr              rhs = as.getAssignStmt().getRHS();
            if (!(rhs instanceof Subscript)) continue;
            IExpr fe = ((Subscript) rhs).getSlice().getIndex();
            if (!isFilterExpression(fe)) continue;
            List<Name> used = as.getDataFramesUsed();
            if (!used.isEmpty() && resolveAlias(used.get(0).getName()).equals(resolved))
                count++;
        }
        return count;
    }

    /**
     * Simplify a DNF group list by removing any group that is a SUPERSET
     * of another group in the list.
     *
     * In DNF, OR([A,B], [B]) = [B] because any row satisfying [A,B] also
     * satisfies [B]. The larger group is redundant and can be removed.
     * This is the Boolean absorption law: A∧B ∨ B = B.
     *
     * This is called after flatten() in rewriteDataSource() so that both
     * approaches — "keep weaker in merge()" and "OR then simplify" —
     * produce the same clean output.
     *
     * Example:
     *   [{age>30, city==NY}, {city==NY}]
     *   {city==NY} ⊆ {age>30,city==NY} → remove {age>30,city==NY}
     *   Result: [{city==NY}]
     */
    private List<Set<Predicate>> simplifyGroups(List<Set<Predicate>> groups) {
        // Step 1: deduplicate exact duplicates
        Map<Set<String>, Set<Predicate>> seen = new LinkedHashMap<>();
        for (Set<Predicate> g : groups) {
            Set<String> keys = new LinkedHashSet<>();
            for (Predicate p : g)
                keys.add(p.getDf_var() + ":(" + p.getCond().getOn()
                        + "," + p.getCond().getOp()
                        + "," + p.getCond().getWith() + ")");
            seen.putIfAbsent(keys, g);
        }
        List<Map.Entry<Set<String>, Set<Predicate>>> unique = new ArrayList<>(seen.entrySet());

        // Step 2: remove any group whose key-set is a strict superset of another group's key-set.
        // In DNF, OR([A,B], [B]) = [B] by Boolean absorption: the larger group is redundant.
        List<Set<Predicate>> result = new ArrayList<>();
        for (int i = 0; i < unique.size(); i++) {
            Set<String> ki = unique.get(i).getKey();
            boolean subsumed = false;
            for (int j = 0; j < unique.size(); j++) {
                if (i == j) continue;
                Set<String> kj = unique.get(j).getKey();
                // kj is a STRICT subset of ki → ki is the redundant/larger group
                if (ki.containsAll(kj) && !ki.equals(kj)) { subsumed = true; break; }
            }
            if (!subsumed) result.add(unique.get(i).getValue());
        }
        return result;
    }

    private String formatGroups(List<Set<Predicate>> groups) {
        if (groups.isEmpty()) return "None";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("[");
            int j = 0;
            for (Predicate p : groups.get(i)) {
                if (j++ > 0) sb.append(", ");
                sb.append(condKey(p.getCond()));
            }
            sb.append("]");
        }
        return sb.append("]").toString();
    }

    private String condKey(Condition c) {
        return "(" + c.getOn() + ", " + c.getOp() + ", " + c.getWith() + ")";
    }
}