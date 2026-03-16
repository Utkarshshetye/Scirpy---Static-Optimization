package ir.Stmt;

import ast.StmtAST;
import ir.IStmt;
import ir.JPAbstractStmt;
import ir.expr.Name;

import java.util.ArrayList;
import java.util.List;

public class ContinueStmt extends JPAbstractStmt implements IStmt {

    private StmtAST ast_type= StmtAST.Continue;
    private String sourceCode = "";

    public ContinueStmt() {
    }

    public ContinueStmt(int lineno, int col_offset) {
        this.lineno = lineno;
        this.col_offset = col_offset;
    }

    public StmtAST getAst_type() {
        return ast_type;
    }

    public void setAst_type(StmtAST ast_type) {
        this.ast_type = ast_type;
    }

    @Override
    public String toString() {
        return "continue";
    }

    public void setSourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
    }

    @Override
    public int getLineno() {
        return lineno;
    }

    public int getCol_offset() {
        return col_offset;
    }

    @Override
    public boolean isModified() {
        return false;  // or track actual modifications
    }

    @Override
    public String getOriginalSource() {
        return sourceCode;
    }

    @Override
    public Object clone() {
        return new ContinueStmt(lineno,col_offset);
    }

    @Override
    public List<Name> getDataFramesDefined() {
        return new ArrayList<>();
    }

    @Override
    public List<Name> getDataFramesUsed() {
        return new ArrayList<>();
    }
}