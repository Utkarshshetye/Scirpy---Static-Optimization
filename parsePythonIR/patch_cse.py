import re

file_path = "src/analysis/AvailableExpression/AvailableExpressionAnalysis.java"

with open(file_path, "r") as f:
    text = f.read()

orig_target = """                Name tempLocal = tempMap.get(key);
                ir.Stmt.AssignStmt irAssign = new ir.Stmt.AssignStmt();
                irAssign.setTargets(new ArrayList<>(java.util.Arrays.asList(tempLocal)));
                irAssign.setRHS((IExpr) e.clone());

                ir.Stmt.AssignmentStmtSoot tassign = new ir.Stmt.AssignmentStmtSoot(tempLocal, (IExpr) e.clone(),
                        irAssign);"""

new_target = """                Name tempLocal = tempMap.get(key);

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

if orig_target in text:
    text = text.replace(orig_target, new_target)
    with open(file_path, "w") as f:
        f.write(text)
    print("Patch applied successfully.")
else:
    print("Error: Target not found.")
    idx = text.find("ir.Stmt.AssignStmt irAssign")
    print(f"Context around target: \n{text[idx-100:idx+200]}")
