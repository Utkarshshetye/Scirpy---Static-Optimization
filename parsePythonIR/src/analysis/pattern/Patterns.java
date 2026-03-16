package analysis.pattern;

import cfg.CFG;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import ir.IExpr;
import ir.JPBody;
import ir.JPMethod;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.Stmt.FunctionDefStmt;
import ir.expr.Attribute;
import ir.expr.Call;
import ir.expr.Constant;
import ir.expr.ListComp;
import ir.internalast.Keyword;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import parse.IRMaker.GetSootMethod;
import parse.IRParser;
import soot.*;

import java.util.regex.*;

import java.util.Iterator;
import java.util.List;

public class Patterns {
    CFG cfg;
    JPMethod jpMethod;
    Rewriter rewriter;

    public Patterns(JPMethod jpMethod) {
        this.jpMethod = jpMethod;
        this.cfg = new CFG(jpMethod);
        rewriter = new Rewriter(cfg, jpMethod);
    }

    // return true means ascending and false means descending
    public String checkSortOrder(Unit unit) {
        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
        IExpr rhs = assignStmt.getRHS();
        Call headCall = (Call) rhs;

        Attribute func = (Attribute) headCall.getFunc();
        Call sortValueCall = (Call) func.getValue();

        for(Keyword keyword : sortValueCall.getKeywords()) {
            if(keyword.getArg().equals("ascending")) {
                return ((Constant) keyword.getValue()).getValue();
            }
        }
        return "True";
    }

    // Rewrite can only happen if there are only one column otherwise nsmallest is not possible on more than one column.
    public String checkSortBy(Unit unit) {
        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
        IExpr rhs = assignStmt.getRHS();
        Call headCall = (Call) rhs;
        Attribute func = (Attribute) headCall.getFunc();
        Call sortValueCall = (Call) func.getValue();

        for(Keyword keyword : sortValueCall.getKeywords()) {
            if(keyword.getArg().equals("by")) {
                if(((ListComp) keyword.getValue()).getElts().size() > 1) {
                    return "impossible";
                }
                else {
                    return ((Constant) keyword.getValue()).getValue();
                }
            }
        }

        return "";
    }

    // return the value of n if specified
    public String checkHead(Unit unit) {
        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
        IExpr rhs = assignStmt.getRHS();
        Call headCall = (Call) rhs;

        // Check if n=x present in the argument or not
        if(!headCall.getKeywords().isEmpty()) {
            Keyword keyword = (Keyword) headCall.getKeywords().get(0);
            return ((Constant) keyword.getValue()).getValue();
        }
        else {
            return "5";
        }
    }

    private String columnWithCountOne(PatchingChain<Unit> units) {
        int count = 0;
        String prev = "", match = "";
        Pattern patterns = Pattern.compile("\\[(.*?)\\]");

        for(Unit unit : units) {
            Matcher matcher = patterns.matcher(unit.toString());
            if(matcher.find()) {
                match = matcher.group(1);
                if(!match.equals(prev)) {
                    count++;
                    prev = match;
                }
            }
        }

        if(count == 1) {
            return prev;
        }

        return "";
    }

    public JPBody getFunctionBody(Unit unit, JSONArray stmtList, String method_name) {

        IRParser irParser = new IRParser();
        JSONArray funStmtList = new JSONArray();

        for(Iterator it = stmtList.iterator(); it.hasNext(); ) {
            JSONObject stmt = (JSONObject) it.next();
            String typeOfStmt=stmt.get("ast_type").toString();

            if(typeOfStmt.equals("FunctionDef")){
                // Get IR for required function
                String fun_name=stmt.get("name").toString();
                if(fun_name.equals(method_name)){
                    funStmtList.add(stmt);
                    funStmtList.addAll((JSONArray)stmt.get("body"));
                    break;
                }
            }
        }

        return irParser.getIR(funStmtList,method_name);
    }

    // All the necessary preconditions for the sort_values().head() will be checked here
    // 1. check ascending or descending in the sort_values() function
    // 2. check "by" attribute in sort_values() and rewrite can only support one column
    // 3. check the head() call and fetch the value of n if not present then default value is 5
    public void sortHead(Unit unit) {
        String nValue = checkHead(unit);
        String isAscending = checkSortOrder(unit);
        String sortByColumn = checkSortBy(unit);

        if(sortByColumn.equals("impossible")) {
            System.out.println("Checking: Not possible to rewrite nsmallest rule as there are more than one column for sorting");
            return;
        }
        else {
            rewriter.sortHeadRewriter(unit, nValue, isAscending, sortByColumn);
        }
    }

    public Unit getFuncBody(PatchingChain<Unit> units, String funcName) {
        for(Unit unit : units) {
            if(unit instanceof FunctionDefStmt) {
                FunctionDefStmt funcDefStmt = (FunctionDefStmt) unit;
                if(funcDefStmt.getName().id.equals(funcName)) {
                    return unit;
                }
            }
        }
        return null;
    }


    public void applyChecker(Unit unit, JSONArray arr, String methodName) {

        JPBody funJpBody = getFunctionBody(unit,arr,methodName);

        String colmn = columnWithCountOne(funJpBody.getUnits());

        if(colmn.isEmpty()) {
            System.out.println("Remove-Axis Rule is not applicable!!");
            return;
        }

        Unit funUnit = getFuncBody(funJpBody.getUnits(),methodName);

        rewriter.removeAxisRewriter(unit, colmn, (FunctionDefStmt) funUnit);
    }
}
