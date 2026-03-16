package BackupCode;

import ir.expr.Compare;

public class SimpliefiedIfBackUp {
    /*
    package parse;

import ir.IExpr;
import ir.Stmt.IfStmt;
import ir.Stmt.IfStmtPy;
import ir.expr.*;
import parse.ops.BoolOpParser;
import soot.PatchingChain;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.util.HashChain;

import java.util.ArrayList;
import java.util.List;

public class SimplifiedIf {

    public PatchingChain<Unit> getSimplifiedIf(IfStmtPy ifStmtPy){
        PatchingChain<Unit> extendedIfList=new PatchingChain<Unit>(new HashChain<Unit>());
        IExpr test= ifStmtPy.getTest();
        //TODO createSimpleConditions and Add statements for them in extendedIfList
        IExpr condition=createSimpleConditions(test, extendedIfList);
        NopStmt nop= Jimple.v().newNopStmt();
        IfStmt ifStmt=new IfStmt(condition,nop);
        ifStmt.copyIf(ifStmtPy);
        extendedIfList.add(ifStmt);
        IfStmtHelper ifStmtHelper=new IfStmtHelper();
        ifStmtHelper.addDetailedStmts(extendedIfList,ifStmt);
        return extendedIfList;

    }

    private IExpr createSimpleConditions(IExpr test, PatchingChain<Unit> extendedIfList) {
        //TODO createSimpleConditions and Add statements for them in extendedIfList
        //TODO reverse finalsimple created condition
        IExpr iExpr=null;
        if(test instanceof BoolOp) {
            BoolOp test1 = (BoolOp) test;
            //TODO simplification
            System.out.println("Condition boolop not implemented for simplfying");
            System.exit(1);
        }
        else if (test instanceof Compare) {

    Compare originalTest=(Compare)test;
    Compare reverseTest=new Compare();
            reverseTest.copyLineInfo(originalTest);
    //check if it is a simple compare
            if(originalTest.getComparators().size()==1){
        reverseTest.setLeft(originalTest.getComparators().get(0));
        reverseTest.getComparators().add(originalTest.getLeft());
        reverseTest.setOps(reverseOps(originalTest));
    }

             return reverseTest;

}
        else {
                System.out.println("Condition encountered not implemented for simplfying");
                System.exit(1);
                }
                //should never reach here
                return iExpr;
                }

//Should not be used anymore..changed implementation

private List<OpsType> reverseOps(Compare originalTest) {
        List<OpsType> opsTypes=new ArrayList<>();
        for(OpsType opsType:originalTest.getOps()){
        opsTypes.add(ReverseOp.getReverseOp(opsType));
        }
        return opsTypes;
        }


        }

        */
}
