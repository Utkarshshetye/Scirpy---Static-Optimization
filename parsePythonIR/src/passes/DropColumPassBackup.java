
package passes;

        import PythonGateWay.ReadProps;
        import analysis.LiveVariable.Alva2;
        import analysis.LiveVariable.AttributeLiveVariableAnalysis;
        import analysis.LiveVariable.LiveVariableAnalysis;
        import cfg.CFG;
        import ir.IExpr;
        import ir.JPBody;
        import ir.JPMethod;
        import ir.Stmt.AssignStmt;
        import ir.Stmt.AssignmentStmtSoot;
        import ir.Stmt.CallExprStmt;
        import ir.Stmt.ImportStmt;
        import ir.expr.*;
        import ir.internalast.Keyword;
        import ir.internalast.Targets;
        import rewrite.pd1.Pd1Elt;
        import soot.Local;
        import soot.PatchingChain;
        import soot.Unit;
        import soot.toolkits.scalar.ArraySparseSet;
        import soot.toolkits.scalar.FlowSet;

        import java.util.*;

public class DropColumPassBackup {

    JPBody jpBody ;
    boolean hasImport;
    boolean hasPandas;
    String pandasAlias="";
    PatchingChain<Unit> unitChain;
    List<Pd1Elt> pdLists =new ArrayList<>();


    /*
    Algorithm
    1. Get all dataframe created at different program points
    2. Get all statements of drop for each data frame
    3. For each dataframe, avoid loading those columns in the memory at the time of creation of those statements
    4. delete those statements
     */

    public DropColumPassBackup(JPBody jpBody) {
        this.jpBody = jpBody;
    }

    CFG cfg ;
    JPMethod jpMethod;
    LiveVariableAnalysis lva;
    Iterator unitIterator;
    AttributeLiveVariableAnalysis alva;
    Alva2 alva2;
    List<Unit> pandasUnits;
    public DropColumPassBackup(JPMethod jpMethod) {
        this.jpMethod = jpMethod;
        this.cfg = new CFG(jpMethod);
        System.out.println(this.cfg.getUnitGraph().toString());
        this.lva = new LiveVariableAnalysis(cfg.getUnitGraph());
        this.alva=new AttributeLiveVariableAnalysis(cfg.getUnitGraph());
        this.unitIterator = cfg.getUnitGraph().iterator();
        this.alva2=new Alva2(cfg.getUnitGraph());
        pandasUnits=new ArrayList<Unit>();
        performPass();
        updateUsedColumns();
    }

    public List<Pd1Elt> getPdLists() {
        return pdLists;
    }

    public void performPass(){
        FlowSet beforeSet,afterSet;



        //generate flowset for each unit
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            beforeSet = (FlowSet) alva.getFlowBefore(unit);
            afterSet = (FlowSet) alva.getFlowAfter(unit);
            if(unit instanceof ImportStmt) {
                hasImport = true;
                ImportStmt importStmt = (ImportStmt) unit;
                if (importStmt.getNames().getName().equals("pandas") || importStmt.getNames().getName().equals(ReadProps.read("ScapaName"))) {
                    hasPandas = true;
                    pandasAlias = importStmt.getNames().getAsname();
                    //System.out.println("Pandas alias:"+ pandasAlias);
                }

            }
            if(hasPandas){
                if(unit instanceof AssignmentStmtSoot){
                    //System.out.println("Assignment statement:"+ unit);
                    AssignmentStmtSoot assignmentStmtSoot=(AssignmentStmtSoot)unit;
                    AssignStmt assignStmt=assignmentStmtSoot.getAssignStmt();
                    IExpr rhs=assignStmt.getRHS();
                    List<IExpr> target=assignStmt.getTargets();
                    if(rhs instanceof Name){
                        Name name=(Name)rhs;
                    }
                    else if(rhs instanceof Call){
                        Call call=(Call)rhs;
                        if(call.getFunc() instanceof Attribute){
                            Attribute func=(Attribute)call.getFunc();
                            //System.out.println(func.getAttr());
                            while(func.getValue() instanceof Attribute){
                                //System.out.println(func.getAttr());
                                func=(Attribute)func.getValue();

                            }
                            if(func.getValue() instanceof Name){

                                Name name=(Name)func.getValue();

                                if(name.id.equals(pandasAlias)){
                                    String dfName="";
                                    int lineno=-1;
                                    if(target.get(0) instanceof Targets) {
                                        dfName = ((Targets) target.get(0)).getName();
                                        lineno = ((Targets) target.get(0)).getLineno();
                                    }
                                    if(target.get(0) instanceof Name) {
                                        dfName = ((Name) target.get(0)).getName();
                                        lineno = ((Name) target.get(0)).getLineno();
                                    }

                                    //String attrName=att
                                    List atList=new ArrayList();
                                    System.out.println(name.id+" is used to create dataframe with name: "+dfName+ " at lineno: "+lineno+" and variables live are:"+ afterSet );
                                    FlowSet killed=new ArraySparseSet();
                                    afterSet.difference(beforeSet,killed);
                                    Pd1Elt elt=new Pd1Elt();

                                    for (Iterator<Local> itr = killed.toList().iterator(); itr.hasNext(); ){
                                        //for(Local l:outIt){
                                        Local l=itr.next();
                                        String localName=l.getName();
//
                                        //TODO verify this code::::Verified for working...
                                        //Newly added
                                        if(l instanceof  Name){
                                            Name nam=(Name)l;
                                            //HERE checking if it is a pandas dataframe attribute for this dataframe
                                            if(nam.getParent()!=null && nam.getParent().equals(dfName)){
                                                atList.add(nam.getName());
                                                //System.exit(1);
                                            }
                                        }
                                    }
                                    //System.out.println("Columns used from file are:"+atList );
                                    elt.setLineno(lineno);
                                    elt.setUnit(unit);
                                    elt.setCols(atList);
                                    assert(dfName!="");
                                    elt.setDfName(dfName);
                                    //TODO added this to verify that if no columns are used, dont add anythin.
                                    //TODO tis could also mean to remove that dataframe all together:)
                                    //TODO Update:this also used to removes drop columns, so used even if no column used..
                                    pdLists.add(elt);
                                    //code before update
//                                    if(atList.size()!=0) {
//                                        pdLists.add(elt);
//                                    }
                                }
                            }
                        }
                    }
                }
                if(unit instanceof CallExprStmt){
                    IExpr calIExpr=((CallExprStmt) unit).getCallExpr();
                    if(calIExpr instanceof Call){
                        //dfNmae=name of dataframe if dataframe. attr=action, we are looking for "drop" here
                        String dfName="", attr="";
                        Call call=(Call)calIExpr;
                        if(call.getFunc()!=null && call.getFunc() instanceof Attribute){
                            Attribute func=(Attribute)call.getFunc();
                            attr=func.getAttr();
                            if(func.getValue()!=null && func.getValue() instanceof Name){
                                dfName=((Name)func.getValue()).getName();
                            }
                        }
                        //If something is dropped::check if that is used from here to the point of definition in dataframe....
                        if(attr.equals("drop")){
                            //if a column is dropped, checking dataframe from which it was dropped, and adding the drop info to its list

                            for(Pd1Elt pd1Elt:pdLists){
                                if(pd1Elt.getDfName().equals(dfName)){
                                    List<IExpr> args= call.getArgs();
                                    for(IExpr arg:args){
                                        if(arg instanceof ListComp){
                                            ListComp argLC=(ListComp)arg;
                                            for(IExpr elt: argLC.getElts()){
                                                if(elt instanceof Str){
                                                    pd1Elt.getDropCols().add((((Str) elt)).getS());
                                                    pd1Elt.getDropsColsUnit().add(unit);
                                                    pd1Elt.getDropColUnitMap().put( ((Str) elt).getS(),unit);
                                                }
                                            }
                                        }
                                    }
                                    //pd1Elt.getDropCols().add("test");
                                }
                            }

                        }


                    }


                    //to delete line if it is a drop column
                    // ((CallExprStmt) unit).setLineno(-10);

                    //System.out.println("CallExprStmt statement:"+ unit);
                    //CallExprStmt callExprStmt=(CallExprStmt)unit;
                }
                FlowSet set = (FlowSet) alva.getFlowBefore(unit);
                //retainUsedDropColumns(set, pdLists);



            }



        }// end unitIterator
    }//end Perform pass

    public void insertLists(){
        HashMap<Unit,Unit> insertMap= new HashMap<>();
        this.unitIterator = cfg.getUnitGraph().iterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            if(unit instanceof AssignmentStmtSoot) {
                for (Pd1Elt pdList : pdLists) {
                    Unit listUnit = pdList.getUnit();
                    //see if list has to be inserted here
                    if (unit.equals(listUnit)) {
                        AssignStmt assignStmt=getListStmt(pdList);
                        //TODO::create this Map at the time of insertion in pdLists to avoid this method call altogether
                        insertMap.put(assignStmt,unit);
                        //jpMethod.getBody().getUnits().insertAfter(assignStmt,unit);
                        System.out.println("Created column list for :"+unit.toString()+"\n"+assignStmt.toString());
                    }
                }

            }//if assignment statement

        }
        for (Unit assignStmt : insertMap.keySet()) {
            Unit unit=insertMap.get(assignStmt);
            //insert in jpBody
            jpMethod.getBody().getUnits().insertBefore(assignStmt,unit);
            //update dataframe creation statement to include usecols
            updatePDCreationStmt(assignStmt,unit);
        }

    }
    public AssignStmt getListStmt(Pd1Elt pdList){
        //TODO change to col_offset
        int lineno=pdList.getLineno();
        int col_offset=lineno;
        AssignStmt assignStmt=new AssignStmt(lineno,col_offset);
        assignStmt.getTargets().add(new Name("columns", Expr_Context.Load));
        ListComp listComp=new ListComp();
        listComp.setCol_offset(col_offset);
        listComp.setLineno(lineno);
        for(String colname:pdList.getCols()) {
            listComp.getElts().add(new Str(lineno,col_offset,colname));
        }
        assignStmt.setRHS(listComp);
        assignStmt.setModified(true);
        return assignStmt;
    }

    private void updatePDCreationStmt(Unit assignStmt,Unit unit){
        AssignStmt assignStmt1=(AssignStmt)assignStmt;

        Keyword keyword=new Keyword();
        keyword.setArg("usecols");
        keyword.setValue(assignStmt1.getTargets().get(0));
        AssignmentStmtSoot unitAssignSoot=(AssignmentStmtSoot)unit;
        Call call=(Call)unitAssignSoot.getAssignStmt().getRHS();
        call.getKeywords().add(keyword);
        unitAssignSoot.setModified(true);

    }

    public void updateUsedColumns(){
        //Call check condition
        //if condition fails, remove that column from drop column list
        this.unitIterator = cfg.getUnitGraph().iterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            FlowSet set = (FlowSet) alva.getFlowBefore(unit);
            System.out.println("Unit: " + unit+"\n");

            System.out.println("\t att live before Before: " + set);

            set = (FlowSet) alva.getFlowAfter(unit);
            System.out.println("\t att live after: " + set);
            retainUsedDropColumns(set, pdLists);
//            for(Local local:(FlowSet)set.){
//
//            }
        }
        System.out.println("Dropped Columns are:"+pdLists.get(0).getDropCols());


    }
    public void  retainUsedDropColumns(FlowSet liveBefore, List<Pd1Elt> pdLists){
        for(Pd1Elt pd1Elt: pdLists){
            for(String dropColumn: pd1Elt.getDropCols()){
                Iterator liveBeforeIterator=liveBefore.iterator();
                while (liveBeforeIterator.hasNext()){
                    IExpr var=(IExpr) liveBeforeIterator.next();
                    String varStr=var.toString();
                    if(varStr.equals(dropColumn)){
                        pd1Elt.getDropCols().remove(dropColumn);
                    }
                }

            }
        }

    }


}
