package test;

       import analysis.LiveVariable.AttributeLiveVariableAnalysis;
        import cfg.CFG;
        import ir.JPBody;
        import ir.JPMethod;
        import json.ReadPythonIR;
        import org.json.simple.JSONArray;
        import parse.IRMaker.MainParser;
        import passes.DropColumnPass;
        import regions.PythonWriter.RegionToPython;
        import regions.RegionDriver;
        import rewrite.IRtoPython;
        import soot.SootClass;
        import soot.SootMethod;
        import soot.Unit;
        import soot.toolkits.scalar.FlowSet;

        import java.util.ArrayList;
        import java.util.Iterator;
        import java.util.List;

public class IPAnalysisAll {
     String sourcePath ="";// "/home/bhushan/intellijprojects/scirpy/pythonIR";
     String path ="";// "/home/bhushan/intellijprojects/scirpy/pythonIR";
     String dataPath ="";// "/home/bhushan/intellijprojects/scirpy/pythonIR";
     String filename = "";//""temp.json";
     String srcFileName="";//""test.py";
     String destinationFilePath="";


    public void analyze(){
        //INIT PORTION
        long time0 = System.currentTimeMillis();



        //SOURCE TO IR
        JSONArray stmtList = ReadPythonIR.ReadFile(filename);
        MainParser mainParser = new MainParser(stmtList);
        List<SootClass> sootClasses = mainParser.getClasses();
        SootClass mainClass=null;

        //TRANSFORMATIONS
//        mainClass=getMainClass(sootClasses);
//        SootMethod mainMethod=mainClass.getMethodByName("main");
//        JPBody mainJpBody=(JPBody) (mainMethod.getActiveBody());
//        DropColumnPass dcp=new DropColumnPass((JPMethod)mainJpBody.getMethod(), dataPath,sootClasses);
//        dcp.insertLists();
//        dcp.insertClmnTypeStmt();
//        //IR TO PYTHON
//        RegionToPython regionToPython=new RegionToPython();
//        regionToPython.toFileInterprocedural(path,destinationFilePath,srcFileName,sootClasses);
//        long time1 = System.currentTimeMillis();
//        System.out.println("Total time: "+time(time1-time0));

//Trying step by step
        mainClass=getMainClass(sootClasses);
        SootMethod mainMethod=mainClass.getMethodByName("main");
        JPBody mainJpBody=(JPBody) (mainMethod.getActiveBody());
        DropColumnPass dcp=new DropColumnPass((JPMethod)mainJpBody.getMethod(), dataPath,sootClasses);
        dcp.insertLists();
        //dcp.insertClmnTypeStmt();
        //IR TO PYTHON
        RegionToPython regionToPython=new RegionToPython();
        regionToPython.toFileInterprocedural(path,destinationFilePath+"/op1",srcFileName,sootClasses);
        long time1 = System.currentTimeMillis();
        System.out.println("Total time 1: "+time(time1-time0));

        dcp.insertClmnTypeStmt();
        //IR TO PYTHON
        regionToPython=new RegionToPython();
        regionToPython.toFileInterprocedural(path,destinationFilePath+"/op2",srcFileName,sootClasses);
        long time2 = System.currentTimeMillis();
        System.out.println("Total time 2 after 1: "+time(time2-time1));




    }

    private static SootClass getMainClass(List<SootClass> sootClasses) {
        for (SootClass sootClass : sootClasses) {
            if (sootClass.getName().equalsIgnoreCase("MainClass")) {
                return sootClass;
            }
        }
        return null;
    }
    private static String time(long t) {
        return t/1000 + "." + String.valueOf(1000+(t%1000)).substring(1);
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSrcFileName() {
        return srcFileName;
    }

    public void setSrcFileName(String srcFileName) {
        this.srcFileName = srcFileName;
    }

    public  String getDestinationFilePath() {
        return destinationFilePath;
    }

    public void setDestinationFilePath(String destinationFilePath) {
        this.destinationFilePath = destinationFilePath;
    }
}
