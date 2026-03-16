import SootClassHelp.SootClassMaker;
import eqsql.dbridge.analysis.eqsql.analysis.DIRRegionAnalyzer;
import eqsql.dbridge.analysis.eqsql.expr.DIR;
import ir.IStmt;
import ir.JPBody;
import ir.JPMethod;
import ir.util.SourceBuffer;
import json.ReadPythonIR;
import org.json.simple.JSONArray;
import parse.*;
import parse.IRMaker.SceneMaker;
import passes.DropColumnPass;

import regions.PythonWriter.RegionToPython;
import regions.RegionDriver;
import rewrite.IRtoPython;
import soot.*;
import test.TestRegionCompatibility;
import util.PythonCall;


import java.util.*;


public class Main {

    public static final void main(String [] args) {
            System.getProperty("java.version");
            //TODO make it dynamic
            PythonCall.pythonCall("pd1.py");

            String srcFileName="test.py";
            //String sourcePath="/home/abc/IDEA1/scirpy/pythonIR";
           // String sourcePath="/home/bhushan/intellijprojects/scirpy/pythonIR";
           // String path="/home/bhushan/intellijprojects/scirpy/pythonIR";
         //   String sourcePath="/home/bhushan/projects/scirpy/pythonIR/";
          //  String path="/home/bhushan/projects/scirpy/pythonIR";


            //String path="/home/abc/IDEA1/scirpy/pythonIR";
            //String sourcePath="/home/mudra/IDEA/scirpy/pythonIR";
            String sourcePath="/home/bhushan/intellijprojects/scirpy/pythonIR";
            String path="/home/bhushan/intellijprojects/scirpy/pythonIR";
            //String path="/home/mudra/IDEA/scirpy/pythonIR";
            // path="/home/bhushan/intellij Projects/scirpy/parsePythonIR";
            //path="/home/bhushan/IdeaProjects/PJ/forMerge/scirpy/pythonIR";
            //path="/home/abc/IDEA1/scirpy/pythonIR";
            //String dataPath="/home/mudra/IDEA/scirpy/pythonIR";
            String dataPath="/home/bhushan/intellijprojects/scirpy/pythonIR";
            SourceBuffer sourceBuffer=new SourceBuffer();
            HashMap<Integer,String> sourceMap=sourceBuffer.makeBuffer(sourcePath+"/"+srcFileName);
            //NEW CODE HERE ONWARDS>>>>
            String filename="temp.json";
            //JSONArray stmtList = ReadPythonIR.ReadFile(args[0]);
            JSONArray stmtList = ReadPythonIR.ReadFile(filename);
            //SCIRPY V02 START
//
//            SceneMaker sceneMaker=new SceneMaker(stmtList);
//            sceneMaker.UpdateScene();

            //SCIRPY V02 END

            //SCIRPY V01 START
            IRParser irParser=new IRParser();
            irParser.setSourceMap(sourceMap);
            JPBody jpBody=irParser.getIR(stmtList,"main");
            //TEST START
            TestRegionCompatibility tRC=new TestRegionCompatibility((JPMethod)jpBody.getMethod());
            tRC.test();
            //TEST END

            //TEST START SOOTCLASS
            SootClassMaker scm=new SootClassMaker();
            SootClass sc=scm.getSootClass("test.py", Modifier.PUBLIC, jpBody.getMethod());
           // TestSoot.testSootPython(sc);

//            List<String> argsList = new ArrayList<String>(Arrays.asList(args));
//            argsList.addAll(Arrays.asList(new String[]{
//                    "-w",
//                    "-main-class",
//                    "testers.CallGraphs",//main-class
//                    "testers.CallGraphs",//argument classes
//                    "testers.A"			//
//            }));
//
//            String javapath = System.getProperty("java.class.path");
//            String jredir = System.getProperty("java.home")+"/lib/rt.jar";
//            String pathjre = javapath+ File.pathSeparator+jredir;
//            Scene.v().setSootClassPath(pathjre);
//            PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {
//
//                    @Override
//                    protected void internalTransform(String phaseName, Map options) {
//                            CHATransformer.v().transform();
//                            SootClass a = Scene.v().getSootClass("test.py");
//
//                            SootMethod src = Scene.v().getMainClass().getMethodByName("doStuff");
//                            CallGraph cg = Scene.v().getCallGraph();
//
////                            Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(src));
////                            while (targets.hasNext()) {
////                                    SootMethod tgt = (SootMethod)targets.next();
////                                    System.out.println(src + " may call " + tgt);
////                            }
//                    }
//
//            }));
//
//            args = argsList.toArray(new String[0]);
//
//            soot.Main.main(args);
            //TEST END SOOTCLASS
            //Building regions here

            //Printing from ir to python

            //SCIRPY V01 END

            System.out.println("Original Source Printing");
            for(Unit unit:jpBody.getUnits()){
            IStmt unitI=(IStmt)unit;
            System.out.println(sourceMap.get(unitI.getLineno()));
            }
            IRtoPython iRtoPython=new IRtoPython();
            //iRtoPython.toStdOut(jpBody);
            //iRtoPython.toFile(path,path,"pd1",jpBody);

            //Uncomment this for normal live variable
            //TestLiveVariable.analyzeLiveVariable((JPMethod)jpBody.getMethod());
            //buildIR.TestChains();

            /*Pd1Pass pd1Pass=new Pd1Pass((JPMethod)jpBody.getMethod());
            pd1Pass.insertLists();*/


            //Pd2Pass pd2Pass=new Pd2Pass((JPMethod)jpBody.getMethod());
            //PRINTING HERE:::

            //Calling drop column pass here

            //changed on 26/09/20 merged pd1pass, pd2pass and dropcolumn into one
            //commented on 02-06-2021 to test DIR
//            DropColumnPass dcp=new DropColumnPass((JPMethod)jpBody.getMethod(), dataPath);
//            dcp.insertLists();
//            dcp.insertClmnTypeStmt();
            //End call drop caolumn pass

            //CAlling 2nd transformation here

            //Pd2Pass pd2Pass=new Pd2Pass((JPMethod)jpBody.getMethod(),path);


            /*
            Pd2Pass pd2Pass=new Pd2Pass((JPMethod)jpBody.getMethod(),dataPath);
            pd2Pass.setMainPath(path);
            pd2Pass.insertClmnTypeStmt();*/



            //end of 2nd transformation

            /*List<InputFileDataTypeMapper> pdLists = pd2Pass.getIfdtmList();
            MultiStageDataFetchRewrite msdfr = new MultiStageDataFetchRewrite((JPMethod)jpBody.getMethod(),pdLists);
            msdfr.PerformPass();
            System.out.println("MSDF Debug output");
            msdfr.DebugOutput();*/

            RegionDriver regionDriver=new RegionDriver();
            regionDriver.buildRegion(jpBody);
            RegionToPython regionToPython=new RegionToPython();
            regionToPython.toFile(path,path,srcFileName,regionDriver.getTopRegion());

            //DIR Started
            DIRRegionAnalyzer dirRegionAnalyzer=DIRRegionAnalyzer.INSTANCE;
            DIR dir=dirRegionAnalyzer.constructDIR(regionDriver.getTopRegion());
            System.out.println(dir);
            //DIR end
            //MultiStageDataFetch msdf = new MultiStageDataFetch((JPMethod)jpBody.getMethod());
            /*MultiStageDataFetch msdf = new MultiStageDataFetch((JPMethod)jpBody.getMethod());
            boolean check = msdf.checkApplicability();
            if(check == true)
                    msdf.Analysis();*/

            //MultiStageDataFetchRewrite msdfr = new MultiStageDataFetchRewrite((JPMethod)jpBody.getMethod());



            //iRtoPython.toStdOut(jpBody);
            //iRtoPython.toFile(path,path,srcFileName,jpBody);

//            Pd1Rewrite pd1Rewrite=new Pd1Rewrite();
//
//            List<CodeLine> listT=new ArrayList<>();
//            List<Pd1Elt> pd1Elts=pd1Pass.getPdLists();
            //pd1Rewrite.rewriteSource(path,path,"pd1",listT,listT,listT );
            //pd1Rewrite.rewritePd1(path,path,"pd1Full",pd1Elts);

        //}//else removed

    }
    //Supposed to parse different bodies, but parsing lines write now
    //private static void parseCodeBody(JSONObject body)

    }