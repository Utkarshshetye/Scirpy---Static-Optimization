package analysis.AvailableExpression;

import cfg.CFG;
import ir.IExpr;
import ir.JPMethod;
import ir.expr.*;
import soot.PatchingChain;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.Expr;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

public class AvailableExpressionAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Expr>> {

    private List<IExpr> assignStatements = new ArrayList<>();
    private JPMethod jpMethod;
    private List<String> skipOp;
    private HashMap<String, Integer> counts = new HashMap<>();
    private IdentityHashMap<IExpr, String> originalKeys = new IdentityHashMap<>();

    private String getOriginalKey(IExpr e) {
        if (e == null)
            return "null";
        if (originalKeys.containsKey(e))
            return originalKeys.get(e);

        String key = exprKey(e);
        originalKeys.put(e, key);
        return key;
    }

    public AvailableExpressionAnalysis(JPMethod jMethod) {
        super(new CFG(jMethod).getUnitGraph());
        this.jpMethod = jMethod;
        skipOp = Arrays.asList("groupby","rolling", "expanding", "resample");
        getAssignmentStats(jMethod);
        doAnalysis();
    }

    private void getAssignmentStats(JPMethod m1) {
        PatchingChain<Unit> assignStats = m1.getActiveBody().getUnits();

        for (Unit expr : assignStats) {
            if (expr instanceof AssignStmt) {
                AssignStmt assignExpr = (AssignStmt) expr;
                Value rhsExpr = assignExpr.getRightOp();

                if (rhsExpr instanceof IExpr) {
                    collectGenExpr((IExpr) rhsExpr);
                }
            }
        }
    }

    private void collectGenExpr(IExpr expr) {
        if (expr == null)
            return;

        assignStatements.add(expr);
        String key = getOriginalKey(expr);
        counts.put(key, counts.getOrDefault(key, 0) + 1);

        // a op b
        if (expr instanceof BinOp) {
            BinOp b = (BinOp) expr;
            collectGenExpr(b.getLeft());
            collectGenExpr(b.getRight());
            return;
        }

        // a.fun()
        if (expr instanceof Call) {

            Call c = (Call) expr;

            // if (isInterMediateOperators(c.getBaseName())) {
            // return;
            // }
            collectGenExpr(c.getFunc());

            for (IExpr a : c.getArgs())
                collectGenExpr(a);

            return;
        }

        // a['idx']
        if (expr instanceof Subscript) {
            Subscript s = (Subscript) expr;
            collectGenExpr(s.getValue());
            if (s.getSlice() != null) {
                if (s.getSlice().getIndex() != null)
                    collectGenExpr(s.getSlice().getIndex());
                if (s.getSlice().getValue() != null)
                    collectGenExpr(s.getSlice().getValue());
                if (s.getSlice().getLower() != null)
                    collectGenExpr(s.getSlice().getLower());
                if (s.getSlice().getUpper() != null)
                    collectGenExpr(s.getSlice().getUpper());
                if (s.getSlice().getStep() != null)
                    collectGenExpr(s.getSlice().getStep());
            }
            return;
        }

        if (expr instanceof Attribute) {
            Attribute a = (Attribute) expr;
            collectGenExpr(a.getValue());
        }
    }

    private void collectGenExprs(IExpr expr, FlowSet<Expr> genSet) {
        if (expr == null)
            return;

        if (expr instanceof BinOp) {
            BinOp bop = (BinOp) expr;
            genSet.add(bop);

            collectGenExprs(bop.getLeft(), genSet);
            collectGenExprs(bop.getRight(), genSet);
            return;
        }

        if (expr instanceof Call) {
            Call c = (Call) expr;

            genSet.add(c);

            collectGenExprs(c.getFunc(), genSet);
            for (IExpr a : c.getArgs())
                collectGenExprs(a, genSet);
            return;
        }

        if (expr instanceof Subscript) {
            Subscript s = (Subscript) expr;
            genSet.add(s);
            collectGenExprs(s.getValue(), genSet);
            if (s.getSlice() != null) {
                if (s.getSlice().getIndex() != null)
                    collectGenExprs(s.getSlice().getIndex(), genSet);
                if (s.getSlice().getValue() != null)
                    collectGenExprs(s.getSlice().getValue(), genSet);
            }
            return;
        }

        if (expr instanceof Attribute) {
            Attribute a = (Attribute) expr;
            genSet.add(a);

            collectGenExprs(a.getValue(), genSet);
        }
    }

    private String exprKey(IExpr e) {
        if (e == null)
            return "null";

        if (e instanceof Name) {
            return "N:" + ((Name) e).getName();
        }

        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            String op = (b.getOp() != null) ? b.getOp().getSymbol() : "unknown";
            return "B(" + op + "," +
                    exprKey(b.getLeft()) + "," +
                    exprKey(b.getRight()) + ")";
        }

        if (e instanceof Call) {
            Call c = (Call) e;
            StringBuilder sb = new StringBuilder();

            sb.append("C(").append(exprKey(c.getFunc()));

            for (IExpr a : c.getArgs())
                sb.append(",").append(exprKey(a));
            sb.append(")");

            return sb.toString();
        }

        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;

            return "A(" + exprKey(a.getValue()) + "." + a.getAttr() + ")";
        }

        if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            StringBuilder sb = new StringBuilder();
            sb.append("S(").append(exprKey(s.getValue()));
            if (s.getSlice() != null) {
                sb.append("[");
                if (s.getSlice().getIndex() != null)
                    sb.append("idx:").append(exprKey(s.getSlice().getIndex()));
                if (s.getSlice().getValue() != null)
                    sb.append("val:").append(exprKey(s.getSlice().getValue()));
                if (s.getSlice().getLower() != null)
                    sb.append("low:").append(exprKey(s.getSlice().getLower()));
                if (s.getSlice().getUpper() != null)
                    sb.append("up:").append(exprKey(s.getSlice().getUpper()));
                if (s.getSlice().getStep() != null)
                    sb.append("st:").append(exprKey(s.getSlice().getStep()));
                sb.append("]");
            }
            sb.append(")");
            return sb.toString();
        }

        return e.toString();
    }

    private boolean isCSECandidate(IExpr e) {
        if (e == null)
            return false;

        if (e instanceof BinOp) {
            return true;
        }

        if (e instanceof Call) {
            Call c = (Call) e;
            String funcName = "";

            if (c.getFunc() instanceof Attribute) {
                funcName = ((Attribute) c.getFunc()).getAttr();
            } else if (c.getFunc() instanceof Name) {
                funcName = ((Name) c.getFunc()).getName();
            }

            if (skipOp.contains(funcName)) {
                return false;
            }

            return true;
        }

        return false;
    }

    private IExpr rewriteExpr(IExpr e, FlowSet<Expr> available, HashMap<String, Name> tempMap, Unit u,
            int[] tempCounter, PatchingChain<Unit> units) {
        if (e == null)
            return null;

        if (e instanceof Name)
            return e;

        String key = getOriginalKey(e);

        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            b.setLeft(rewriteExpr(b.getLeft(), available, tempMap, u, tempCounter, units));
            b.setRight(rewriteExpr(b.getRight(), available, tempMap, u, tempCounter, units));
        } else if (e instanceof Call) {
            Call c = (Call) e;
            c.setFunc(rewriteExpr(c.getFunc(), available, tempMap, u, tempCounter, units));
            List<IExpr> newArgs = new ArrayList<>();

            for (IExpr a : c.getArgs())
                newArgs.add(rewriteExpr(a, available, tempMap, u, tempCounter, units));
            c.setArgs(newArgs);
        } else if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            a.setValue(rewriteExpr(a.getValue(), available, tempMap, u, tempCounter, units));
        } else if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            s.setValue(rewriteExpr(s.getValue(), available, tempMap, u, tempCounter, units));
            if (s.getSlice() != null) {
                if (s.getSlice().getIndex() != null)
                    s.getSlice()
                            .setIndex(rewriteExpr(s.getSlice().getIndex(), available, tempMap, u, tempCounter, units));
                if (s.getSlice().getValue() != null)
                    s.getSlice()
                            .setValue(rewriteExpr(s.getSlice().getValue(), available, tempMap, u, tempCounter, units));
            }
        }

        if (isCSECandidate(e)) {

            boolean isAvailable = false;

            for (Expr ae : available) {
                if (ae instanceof IExpr && getOriginalKey((IExpr) ae).equals(key)) {
                    isAvailable = true;
                    break;
                }
            }

            if (isAvailable && tempMap.containsKey(key)) {
                return (IExpr) tempMap.get(key).clone();
            }

            if (!isAvailable && counts.getOrDefault(key, 0) > 1) {
                Name tempLocal;
                if (!tempMap.containsKey(key)) {
                    String tname = "cse_temp_" + tempCounter[0]++;
                    tempLocal = new Name(tname, Expr_Context.Store);
                    jpMethod.getActiveBody().getLocals().add(tempLocal);
                    tempMap.put(key, tempLocal);
                } else {
                    tempLocal = tempMap.get(key);
                }

                ir.Stmt.AssignStmt irAssign = new ir.Stmt.AssignStmt();
                irAssign.setTargets(new ArrayList<>(Arrays.asList(tempLocal)));
                irAssign.setRHS((IExpr) e.clone());

                ir.Stmt.AssignmentStmtSoot tassign = new ir.Stmt.AssignmentStmtSoot(tempLocal, (IExpr) e.clone(),
                        irAssign);

                units.insertBefore(tassign, u);

                IExpr cloned = (IExpr) e.clone();
                originalKeys.put(cloned, key);
                available.add(cloned);

                return (IExpr) tempLocal.clone();
            }
        }

        return e;
    }

    private boolean usesLocal(IExpr expr, Local v) {
        if (expr == null)
            return false;

        if (expr instanceof Name) {
            return ((Name) expr).getName().equals(v.getName());
        }

        if (expr instanceof BinOp) {
            BinOp b = (BinOp) expr;
            return usesLocal(b.getLeft(), v) || usesLocal(b.getRight(), v);
        }

        if (expr instanceof Call) {
            Call c = (Call) expr;
            if (usesLocal(c.getFunc(), v))
                return true;
            for (IExpr a : c.getArgs())
                if (usesLocal(a, v))
                    return true;
            return false;
        }

        if (expr instanceof Subscript) {
            return usesLocal(((Subscript) expr).getValue(), v);
        }

        if (expr instanceof Attribute) {
            return usesLocal(((Attribute) expr).getValue(), v);
        }

        return false;
    }

    @Override
    protected void flowThrough(FlowSet<Expr> in, Unit unit, FlowSet<Expr> out) {
        in.copy(out);

        FlowSet<Expr> genSet = new ArraySparseSet<>();
        FlowSet<Expr> killSet = new ArraySparseSet<>();

        if (unit instanceof AssignStmt) {
            AssignStmt as = (AssignStmt) unit;
            Value rhs = as.getRightOp();

            if (rhs instanceof IExpr) {
                collectGenExprs((IExpr) rhs, genSet);
            }
        }

        for (ValueBox defBox : unit.getDefBoxes()) {
            Value def = defBox.getValue();

            if (def instanceof Local) {
                Local defined = (Local) def;

                for (IExpr e : assignStatements) {
                    if (usesLocal(e, defined)) {
                        killSet.add(e);
                    }
                }
            }
        }

        out.difference(killSet);
        out.union(genSet);
    }

    @Override
    protected FlowSet<Expr> newInitialFlow() {
        FlowSet<Expr> set = new ArraySparseSet<>();

        for (IExpr expr : assignStatements) {
            set.add(expr);
        }

        return set;
    }

    @Override
    protected FlowSet<Expr> entryInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<Expr> in1, FlowSet<Expr> in2, FlowSet<Expr> out) {
        in1.intersection(in2, out);
    }

    @Override
    protected void copy(FlowSet<Expr> src, FlowSet<Expr> dest) {
        src.copy(dest);
    }

    public void applyCSE() {
        PatchingChain<Unit> units = jpMethod.getActiveBody().getUnits();
        HashMap<String, Name> tempMap = new HashMap<>();
        int[] tempCounter = new int[] { 0 };

        List<Unit> sts = new ArrayList<>(units);

        for (Unit u : sts) {
            if (!(u instanceof AssignStmt))
                continue;

            AssignStmt as = (AssignStmt) u;
            Value rhs = as.getRightOp();

            if (!(rhs instanceof IExpr))
                continue;

            FlowSet<Expr> available = getFlowBefore(u);
            IExpr newExpr = rewriteExpr((IExpr) rhs, available, tempMap, u, tempCounter, units);

            if (newExpr != rhs) {
                as.setRightOp(newExpr);

                if (as instanceof ir.Stmt.AssignmentStmtSoot) {
                    ((ir.Stmt.AssignmentStmtSoot) as).getAssignStmt().setRHS(newExpr);
                }
            }
        }
    }
}