package parse;

import ir.Stmt.ContinueStmt;
import org.json.simple.JSONObject;

public class ContinueStmtParser {
    JSONObject block;
    ContinueStmt continueStmt = null;

    public ContinueStmtParser(JSONObject block) {
        this.block = block;
    }

    public ContinueStmtParser() {}

    public ContinueStmt getContinueStmt(JSONObject block) {
        this.block = block;
        return getContinueStmt();
    }

    public ContinueStmt getContinueStmt() {


        int col_offset = Integer.parseInt(block.get("col_offset").toString());
        int lineno = Integer.parseInt(block.get("lineno").toString());
        continueStmt = new ContinueStmt(lineno,col_offset);
        return continueStmt;
    }
}

