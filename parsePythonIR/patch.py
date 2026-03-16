import sys

with open("src/analysis/AvailableExpression/AvailableExpressionAnalysis.java", "r") as f:
    content = f.read()

target = """                Name tempLocal = tempMap.get(key);
                ir.Stmt.AssignStmt irAssign = new ir.Stmt.AssignStmt();
                irAssign.setTargets(new ArrayList<>(java.util.Arrays.asList(tempLocal)));
                irAssign.setRHS((IExpr) e.clone());

                ir.Stmt.AssignmentStmtSoot tassign = new ir.Stmt.AssignmentStmtSoot(tempLocal, (IExpr) e.clone(),
                        irAssign);"""

replacement = """                Name tempLocal = tempMap.get(key);
                
                IExpr cloned = (IExpr) e.clone();
                if (cloned instanceof BinOp) {
                    BinOp b = (BinOp) cloned;
                    b.setLeft(rewriteExpr(b.getLeft(), available, tempMap, u, tempCounter, units));
                    b.setRight(rewriteExpr(b.getRight(), available, tempMap, u, tempCounter, units));
                } else if (cloned instanceof Call) {
                    Call c = (Call) cloned;
                    c.setFunc(rewriteExpr(c.getFunc(), available, tempMap, u, tempCounter, units));
                    List<IExpr> newArgs = new ArrayList<>();
                    for (IExpr a : c.getArgs())
                        newArgs.add(rewriteExpr(a, available, tempMap, u, tempCounter, units));
                    c.setArgs(newArgs);
                } else if (cloned instanceof Attribute) {
                    Attribute a = (Attribute) cloned;
                    a.setValue(rewriteExpr(a.getValue(), available, tempMap, u, tempCounter, units));
                } else if (cloned instanceof Subscript) {
                    Subscript s = (Subscript) cloned;
                    s.setValue(rewriteExpr(s.getValue(), available, tempMap, u, tempCounter, units));
                }

                ir.Stmt.AssignStmt irAssign = new ir.Stmt.AssignStmt();
                irAssign.setTargets(new ArrayList<>(java.util.Arrays.asList(tempLocal)));
                irAssign.setRHS(cloned);

                ir.Stmt.AssignmentStmtSoot tassign = new ir.Stmt.AssignmentStmtSoot(tempLocal, cloned,
                        irAssign);"""

if target in content:
    content = content.replace(target, replacement)
    with open("src/analysis/AvailableExpression/AvailableExpressionAnalysis.java", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Could not find target block")
    
