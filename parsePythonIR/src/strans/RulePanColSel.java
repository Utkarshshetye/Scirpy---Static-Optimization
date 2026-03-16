package strans;

import eqsql.dbridge.analysis.eqsql.expr.operator.OpType;
import eqsql.dbridge.analysis.eqsql.trans.InputTree;
import eqsql.dbridge.analysis.eqsql.trans.OutputTree;
import eqsql.dbridge.analysis.eqsql.trans.Rule;

public class RulePanColSel extends Rule {
    public RulePanColSel(InputTree inPattern, OutputTree outPattern) {
        super(makeInputPattern(), makeOutputPattern());
    }

    //TODO modify this
    private static InputTree makeInputPattern() {
        InputTree cp1 = new InputTree(OpType.CartesianProd, 1);
        InputTree fr2 = new InputTree(OpType.FieldRef, 2);
        InputTree any3 = new InputTree(OpType.Any, 3);

        InputTree invMethod0 = new InputTree(OpType.InvokeMethod, 0, cp1, fr2, any3);
        return invMethod0;
    }

    private static OutputTree makeOutputPattern() {
        return new OutputTree(2);
    }
}
