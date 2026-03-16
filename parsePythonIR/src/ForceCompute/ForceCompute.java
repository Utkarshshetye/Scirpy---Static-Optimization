// Chiranmoy 10-2-24: Add .compute(live_df=[...]) when dataframe passed to imported methods
package ForceCompute;

import analysis.LiveVariable.LiveDataFrame;
import analysis.Pandas.PandasAPIs;
import analysis.PythonScene;
import cfg.CFG;
import ir.IExpr;
import ir.IStmt;
import ir.JPMethod;
import ir.Stmt.*;
import ir.expr.*;
import ir.internalast.Keyword;
import org.jboss.util.NotImplementedException;
import soot.Unit;
import soot.Value;
import java.util.ArrayList;
import java.util.List;

public class ForceCompute {
    JPMethod jpMethod;
    CFG cfg;
    LiveDataFrame ldf;

    public ForceCompute(JPMethod jpMethod, LiveDataFrame ldf) {
        this.jpMethod = jpMethod;
        this.ldf = ldf;
        this.cfg = new CFG(jpMethod);
    }

    public void force() {
        for (Unit unit : cfg.getUnitGraph()) {
            List<String> liveList = new ArrayList<>();
            ldf.getFlowAfter(unit).forEach(liveList::add);
            forceStmt(unit, liveList);
        }
    }

    private java.util.Set<String> aggregateVars = new java.util.HashSet<>();
    private java.util.Set<String> groupByVars = new java.util.HashSet<>();

    private void forceStmt(Unit unit, List<String> liveList) {
        if (unit instanceof AssignmentStmtSoot) {
            AssignStmt assStmt = ((AssignmentStmtSoot) unit).getAssignStmt();
            IExpr rhs = assStmt.getRHS();
            List<IExpr> targets = assStmt.getTargets();

            for (IExpr lhs : targets) {
                String lhsName = lhs.toString();
                // Tracking logic remains same
                if (rhs instanceof Call) {
                    Call call = (Call) rhs;
                    IExpr func = call.getFunc();
                    if (func instanceof Attribute) {
                        Attribute attr = (Attribute) func;
                        String attrName = attr.getAttr();
                        if ("groupby".equals(attrName)) {
                            groupByVars.add(lhsName);
                        } else if (isAggregate(attrName)) {
                            IExpr base = attr.getValue();
                            String baseName = base.toString();
                            if (base instanceof Name) {
                                baseName = ((Name) base).getName();
                            }
                            if (!groupByVars.contains(baseName)) {
                                aggregateVars.add(lhsName);
                            }
                        }
                    }
                } else if (rhs instanceof Name) {
                    String rhsName = ((Name) rhs).getName();
                    if (aggregateVars.contains(rhsName))
                        aggregateVars.add(lhsName);
                    if (groupByVars.contains(rhsName))
                        groupByVars.add(lhsName);
                }
            }

            // RECURSIVE PROCESSING
            processExpr(rhs, liveList);

        } else if (unit instanceof CallExprStmt) {
            Call call = (Call) ((CallExprStmt) unit).getCallExpr();
            processExpr(call, liveList);
        } else if (unit instanceof IfStmt) {
            IStmt pyStmt = (IStmt) ((IfStmt) unit).getPyStmt();
            if (pyStmt instanceof ForStmtPy)
                forceComputeForStmt((ForStmtPy) pyStmt, liveList);
            else
                forceComputeIfStmt((IfStmtPy) pyStmt, liveList);
        }
    }

    private void processExpr(IExpr expr, List<String> liveList) {
        if (expr == null)
            return;

        if (expr instanceof Call) {
            Call call = (Call) expr;
            if (call.getFunc() instanceof Attribute) {
                processExpr(((Attribute) call.getFunc()).getValue(), liveList);
            }
            for (IExpr arg : call.getArgs())
                processExpr(arg, liveList);
            for (Keyword kw : call.getKeywords())
                processExpr(kw.getValue(), liveList);

            forceComputeCall(call, liveList, true);

        } else if (expr instanceof BinOp) {
            processExpr(((BinOp) expr).getLeft(), liveList);
            processExpr(((BinOp) expr).getRight(), liveList);
        } else if (expr instanceof Compare) {
            forceComputeCompare((Compare) expr, liveList);
        } else if (expr instanceof Attribute) {
            processExpr(((Attribute) expr).getValue(), liveList);
        } else if (expr instanceof Subscript) {
            processExpr(((Subscript) expr).getValue(), liveList);
        } else if (expr instanceof Starred) {
            processExpr(((Starred) expr).getValue(), liveList);
        } else if (expr instanceof ListComp) {
        }
    }

    private boolean isAggregate(String name) {
        return name.equals("sum") || name.equals("mean") || name.equals("min") ||
                name.equals("max") || name.equals("count") || name.equals("std") ||
                name.equals("var") || name.equals("median") || name.equals("prod") ||
                name.equals("sem") || name.equals("skew") || name.equals("kurt") ||
                name.equals("quantile") || name.equals("mad") || name.equals("nunique") ||
                name.equals("first") || name.equals("last") || name.equals("size");
    }

    private void forceComputeCall(Call call, List<String> liveList, boolean forceBasedOnImport) {
        // Handle "split": df.split() -> df.compute().split()
        IExpr func = call.getFunc();
        if (func instanceof Attribute) {
            Attribute attr = (Attribute) func;
            if ("split".equals(attr.getAttr())) {
                IExpr base = attr.getValue();
                if (base.isDataFrame()) {
                    // Check if already computed?
                    if (!isComputeCall(base))
                        attr.setValue(makeComputeCall(base, liveList));
                }
            }
        }

        boolean isPandasOp = false;
        String funcName = "";

        if (func instanceof Attribute) {
            Attribute attr = (Attribute) func;
            funcName = attr.getAttr();
            IExpr base = attr.getValue();

            String baseNameS = base.toString();
            if (base instanceof Name)
                baseNameS = ((Name) base).getName();

            if (base.isDataFrame() || aggregateVars.contains(baseNameS) || groupByVars.contains(baseNameS)) {
                isPandasOp = true;
            } else if (baseNameS.equals(PythonScene.pandasAliasName) || "pandas".equals(baseNameS)
                    || "pd".equals(baseNameS)) {
                isPandasOp = true;
            }
        } else if (func instanceof Name) {
            funcName = ((Name) func).getName();
            // Exclude print and display functions - they can handle lazy DataFrames
            if ("print".equals(funcName) || "display".equals(funcName) || "show".equals(funcName)) {
                isPandasOp = true; // Treat as safe, no compute needed
            } else if ("zip".equals(funcName)) {
                isPandasOp = false;
            } else if (PandasAPIs.getAPIs().contains(funcName)) {
                isPandasOp = true;
            }
        }

        boolean shouldForceArgs = !isPandasOp;

        if (shouldForceArgs) {
            List<Name> used = call.getDataFrames();
            for (int i = used.size() - 1; i > 0; i--) {
                liveList.add(used.get(i).getName());
            }

            for (int i = 0; i < call.getArgs().size(); i++) {
                IExpr arg = call.getArgs().get(i);

                if (!arg.isDataFrame())
                    continue;
                if (isComputeCall(arg))
                    continue;

                if (arg instanceof Starred) {
                    IExpr inner = ((Starred) arg).getValue();
                    if (!isComputeCall(inner))
                        call.getArgs().set(i, new Starred(makeComputeCall(inner, liveList)));
                } else {
                    call.getArgs().set(i, makeComputeCall(arg, liveList));
                }

                if (used.size() > 1 && !liveList.isEmpty())
                    liveList.remove(liveList.size() - 1);
            }

            for (int i = 0; i < call.getKeywords().size(); i++) {
                Keyword keyword = call.getKeywords().get(i);
                if (!keyword.getValue().isDataFrame())
                    continue;
                if (isComputeCall(keyword.getValue()))
                    continue;

                keyword.setValue(makeComputeCall(keyword.getValue(), liveList));
                if (used.size() > 1 && !liveList.isEmpty())
                    liveList.remove(liveList.size() - 1);
            }
        }
    }

    private boolean isComputeCall(IExpr expr) {
        if (expr instanceof Call) {
            IExpr fn = ((Call) expr).getFunc();
            if (fn instanceof Attribute) {
                return "compute".equals(((Attribute) fn).getAttr());
            }
        }
        return false;
    }

    private void forceComputeCompare(Compare compare, List<String> liveList) {
        // Original Logic preserved, but maybe use processExpr?
        // Logic: inject compute if 'aggregateVars'.
        IExpr left = compare.getLeft();
        if (left instanceof Call)
            processExpr(left, liveList); // Recursion
        else if (shouldCompute(left)) {
            compare.setLeft(makeComputeCall(left, liveList));
        }

        for (int i = 0; i < compare.getComparators().size(); i++) {
            IExpr comp = compare.getComparators().get(i);
            if (shouldCompute(comp)) {
                compare.getComparators().set(i, makeComputeCall(comp, liveList));
            } else if (comp instanceof Call)
                processExpr(comp, liveList); // Recursion
        }
    }

    private boolean shouldCompute(IExpr expr) {
        if (!expr.isDataFrame())
            return false;
        if (expr instanceof Name) {
            String name = ((Name) expr).getName();
            return aggregateVars.contains(name);
        }
        return false;
    }

    private void forceComputeForStmt(ForStmtPy forStmt, List<String> liveList) {
        IExpr iter = forStmt.getIterator();
        processExpr(iter, liveList); // Recurse
        if (iter.isDataFrame())
            forStmt.setIterator(makeComputeCall(iter, liveList));
        // iter instanceof Call handled by processExpr
    }

    private void forceComputeIfStmt(IfStmtPy ifStmt, List<String> liveList) {
        IExpr test = ifStmt.getTest();
        processExpr(test, liveList);
        if (test.isDataFrame())
            ifStmt.setTest(makeComputeCall(test, liveList));
    }

    private Call makeComputeCall(IExpr expr, List<String> liveList) {
        List<IExpr> liveListNames = new ArrayList<>();
        liveList.forEach(elem -> liveListNames.add(new Name(elem, Expr_Context.Param)));

        Call computeCall = new Call();
        computeCall.getKeywords().add(new Keyword("live_df", new ListComp(liveListNames)));
        computeCall.setFunc(new Attribute("compute", expr));
        return computeCall;
    }
}
