package analysis.pattern;

import ast.ExprAST;
import cfg.CFG;
import ir.IExpr;
import ir.JPBody;
import ir.JPMethod;
import ir.Stmt.AssignStmt;
import ir.Stmt.AssignmentStmtSoot;
import ir.Stmt.FunctionDefStmt;
import ir.Stmt.ReturnStmt;
import ir.expr.*;
import ir.internalast.Keyword;
import ir.internalast.Slice;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rewriter {
    CFG cfg;
    JPMethod jpMethod;

    public Rewriter(CFG cfg, JPMethod jpMethod) {
        this.cfg = cfg;
        this.jpMethod = jpMethod;
    }

    public void sortHeadRewriter(Unit unit, String nValue, String isAscending, String sortByColumn) {
        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
        IExpr rhs = assignStmt.getRHS();
        Call headCall = (Call) rhs;

        String baseName = headCall.getBaseName();
        Constant nVal = new Constant(0, 0, nValue);
        Constant sortCol = new Constant(0, 0, "'" + sortByColumn + "'");
        List<IExpr> arguments = new ArrayList<>();
        arguments.add(nVal);
        if(!sortByColumn.isEmpty()) {
            arguments.add(sortCol);
        }

        // df['col'].sort_values() format
        if(sortByColumn.isEmpty()) {
            Attribute func = (Attribute) headCall.getFunc();
            Call sortCall = (Call) func.getValue();
            Attribute funcAttr = (Attribute) sortCall.getFunc();
            Subscript sortValue = (Subscript) funcAttr.getValue();
            Constant colName = (Constant) sortValue.getSlice().getIndex();
            String subscriptCol = colName.toString();
            baseName += "[" + subscriptCol + "]";

            System.out.println("pranab");
        }

        if(isAscending.equals("false")) {
            Call nlargest = new Call();
            nlargest.setFunc(new Attribute("nlargest", new Name(baseName, Expr_Context.Load)));
            nlargest.setArgs(arguments);
            nlargest.setBaseName(baseName);
            assignStmt.setRHS(nlargest);
        }
        else {
            Call nsmallest = new Call();
            nsmallest.setFunc(new Attribute("nsmallest", new Name(baseName, Expr_Context.Load)));
            nsmallest.setArgs(arguments);
            nsmallest.setBaseName(baseName);
            assignStmt.setRHS(nsmallest);
        }
    }

    public void changeMainClassMethod(String methodName, ReturnStmt returnStmt) {
        SootClass mainClass = jpMethod.getDeclaringClass();
        SootMethod userMethod = mainClass.getMethodByName(methodName);
        JPBody jpMethod = (JPBody) userMethod.getActiveBody();
        PatchingChain<Unit> units = jpMethod.getUnits();
        Unit runit = null;
        for(Unit unit : units) {
            if(unit instanceof ReturnStmt) {
                runit = unit;
                break;
            }
        }

        if(runit != null) {
            units.swapWith(runit, returnStmt);
        }
    }

    public void changeFuncBodyRewriter(FunctionDefStmt funcDefStmt) {
        PatchingChain<Unit> patchingChain = funcDefStmt.getBody().getUnits();
        ReturnStmt returnStmt = (ReturnStmt) patchingChain.getFirst();
        Compare compareStmt =  (Compare) returnStmt.getValue();
        String funcParameter = ((Str)funcDefStmt.getArgs().get(0)).getName();
        Name rowName = new Name(0, 0, funcParameter, Expr_Context.Load);
        compareStmt.setLeft(rowName);
        String methodName = funcDefStmt.getName().getName();
        changeMainClassMethod(methodName, returnStmt);
    }





    public void removeAxisRewriter(Unit unit, String col, FunctionDefStmt funcBody) {

        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
        Call rhs = (Call) assignStmt.getRHS();
        Attribute funcAttr = (Attribute) rhs.getFunc();

        Constant subscriptCol = new Constant(0, 0,  col);
        Slice slice = new Slice();
        slice.setIndex(subscriptCol);

        Call call = (Call) rhs;
        String rename_df = call.getBaseName();

        List<IExpr> rnmExpr = new ArrayList<>();

        IExpr fun_name = call.getArgs().get(0);

        rnmExpr.add(fun_name);
        call.setArgs(rnmExpr);

        Subscript subcript = new Subscript(ExprAST.Subscript, 0, 0, Expr_Context.Load, new Name(rename_df, Expr_Context.Load), slice);
        List<Keyword> keywords = call.getKeywords();
        keywords.removeIf(k -> k.getArg().equals("axis"));
        funcAttr.setValue(subcript);
        changeFuncBodyRewriter(funcBody);

//        AssignmentStmtSoot assignmentStmtSoot = (AssignmentStmtSoot) unit;
//        AssignStmt assignStmt = assignmentStmtSoot.getAssignStmt();
//        IExpr rhs = assignStmt.getRHS();
//        List<IExpr> arguments = new ArrayList<>();
//
//        Call call = (Call) rhs;
//        String rename_df = call.getBaseName()+"["+col+"]";
//
//        IExpr fun_name = call.getArgs().get(0);
//        arguments.add(fun_name);
//
//        // Creating new Call to be Set
//        Call apply = new Call();
//        apply.setFunc(new Attribute("apply", new Name(rename_df, Expr_Context.Load)));
//        apply.setArgs(arguments);
//        apply.setBaseName(rename_df);
//
//        // Changing RHS to new
//        assignStmt.setRHS(apply);
//
//        // Changing function code to adapt new changes
//        // Removing the col from the function code itself
//        // PatchingChain<Unit> units = funJpBody.getUnits();
//        // Pattern patterns = Pattern.compile("\\[.*?\\]");
//        // String match = "";
//
//        System.out.println(assignStmt);
    }
}