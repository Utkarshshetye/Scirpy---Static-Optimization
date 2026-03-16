#!/bin/bash
sed -i '308,311c\        if (e instanceof Call) {\n            Call c = (Call) e;\n            List<IExpr> newArgs = new ArrayList<>();' /home/utkarsh/scirpy/parsePythonIR/src/analysis/AvailableExpression/AvailableExpressionAnalysis.java
