package analysis.pattern;

import cfg.CFG;
import ir.IExpr;
import ir.JPMethod;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.expr.Attribute;
import ir.expr.Call;
import org.json.simple.JSONArray;
import soot.Body;
import soot.Scene;
import soot.Unit;
import java.util.Iterator;
import java.util.List;

public class PatternMatcher {
    JPMethod jpMethod;
    CFG cfg;
    Iterator unitIterator;
    Patterns patterns;
    JSONArray stmtList;

    public PatternMatcher(JPMethod jpMethod,JSONArray stmtList) {
        this.jpMethod = jpMethod;
        this.cfg = new CFG(jpMethod);
        this.unitIterator = cfg.getUnitGraph().iterator();
        this.stmtList = stmtList;
        patterns = new Patterns(jpMethod);
        checkPatterns();
    }

    // Function to traverse every node in the CFG and check if any patterns exists or not
    public void checkPatterns() {
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            if (unit instanceof AssignmentStmtSoot) {
                AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
                AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
                IExpr rhs = assignStmt.getRHS();
                // List<IExpr> target = assignStmt.getTargets();

                if (rhs instanceof Call) {
                    Call call = (Call) rhs;

                    if (call.getFunc() instanceof Attribute) {
                        Attribute func = (Attribute) call.getFunc();

                        // if(func.getAttr().equals("head")) {
                        //    if(func.getValue() instanceof Call) {
                        //        Call funcCall = (Call) func.getValue();
                        //        if(funcCall.getFunc() instanceof Attribute) {
                        //            Attribute funcAttr = (Attribute) funcCall.getFunc();

//                                    if(funcAttr.getAttr().equals("sort_values")) {
//                                        patterns.sortHead(unit);
//                                    }
//                                }
//                            }
//                        }

                        if(func.getAttr().equals("apply")) {
                            Call funcCall = (Call) rhs;
                            List<IExpr> exp = funcCall.getArgs();
                            String method_name = exp.get(0).toString();
                            patterns.applyChecker(unit,stmtList,method_name);
                        }
                    }
                }
            }
        }
    }
}
