package test;

import analysis.interprocedural.IPMain;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestExamples {

    public String inputPythonFilesPath="";
    public String inputJsonFilesPath="";
    /*
    static String sourcePath = "/home/bhushan/intellijprojects/scirpy/pythonIR";
    static String path = "/home/bhushan/intellijprojects/scirpy/pythonIR";
    static String dataPath = "/home/bhushan/intellijprojects/scirpy/pythonIR";
    static String filename = "temp.json";
    static String srcFileName="test.py";
    static String destinationFileName="";
     */


    public static void main(String args[]) throws FileNotFoundException, IOException {
        String sourcePath = "/home/bhushan/intellijprojects/scirpy/pythonIR/json";
        String path = "/home/bhushan/intellijprojects/scirpy/pythonIR/json";
        String destinationPath="/home/bhushan/intellijprojects/scirpy/pythonIR/optimizedCode";
        String dataPath = "/home/bhushan/intellijprojects/scirpy/pythonIR/data";
        IPAnalysisAll ipAnalysisAll=new IPAnalysisAll();
        String[] srcFileNames={"ex1.py","ex2.py","ex3.py","ex4.py","ex5.py","ex6.py"};
        String[] filenames={"ex1.json","ex2.json","ex3.json","ex4.json","ex5.json","ex6.json"};

        for(int i=0;i<filenames.length;i++) {
            ipAnalysisAll.setSourcePath(sourcePath);
            ipAnalysisAll.setPath(path);
            ipAnalysisAll.setDataPath(dataPath);
            ipAnalysisAll.setFilename(path+"/"+filenames[i]);
            ipAnalysisAll.setSrcFileName(srcFileNames[i]);
            ipAnalysisAll.setDestinationFilePath(destinationPath);
            //ipAnalysisAll.getDestinationFileName();
            ipAnalysisAll.analyze();
        }

        ValidateResults.Validate();
    }

}
