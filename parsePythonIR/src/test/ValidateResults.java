package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ValidateResults {

    //public static void validate(){
    public static void main(String args[]) throws FileNotFoundException, IOException {
    FileReader reader = new FileReader("/home/bhushan/intellijprojects/scirpy/parsePythonIR/src/test/paths.properies");
    Properties paths=new Properties();
    paths.load(reader);

    String referencePath=paths.get("referenceFilesPath").toString();
    String newPath=paths.get("destinationPath").toString();
    List<String> changedFiles = new ArrayList<String>();
    boolean allSimilar=true;

        File refDir = new File(referencePath);
        File[] refDirList = refDir.listFiles();
        if (refDirList != null) {
            for (File subdir : refDirList) {
                if (subdir.isDirectory()) {
                    String subDirName = subdir.getName();
                    File[] refSubDirList = subdir.listFiles();
                    for (File file : refSubDirList) {
                        String refFileFullPath=file.getAbsolutePath();
                        String newFileFullPath=newPath+"/"+subdir.getName()+"/"+file.getName();
//                        System.out.println(refFileFullPath);
//                        System.out.println(newFileFullPath);
                          boolean areSimilar=Comparor.areSimilarFiles(newFileFullPath,refFileFullPath);
                          if(!areSimilar){
                            changedFiles.add(newFileFullPath);
                          }

                    }
                }
            }
        }   else {

        }


    if(changedFiles.size()==0){
        System.out.println("All files are matching. Validation test is successful!!!");
    }
    else{
        System.out.println("Some of the files have changed!!! \nThe changes are observed in the following files:");
        for(String fileName:changedFiles) {
            System.out.println(fileName);
        }
        System.out.println("\nValidation test is unsuccessful!!!");

    }

    }




    //this is copy of main method..main method is for independent testing
    public static void Validate()throws FileNotFoundException, IOException {
        FileReader reader = new FileReader("/home/bhushan/intellijprojects/scirpy/parsePythonIR/src/test/paths.properies");
        Properties paths=new Properties();
        paths.load(reader);

        String referencePath=paths.get("referenceFilesPath").toString();
        String newPath=paths.get("destinationPath").toString();
        List<String> changedFiles = new ArrayList<String>();
        boolean allSimilar=true;

        File refDir = new File(referencePath);
        File[] refDirList = refDir.listFiles();
        if (refDirList != null) {
            for (File subdir : refDirList) {
                if (subdir.isDirectory()) {
                    String subDirName = subdir.getName();
                    File[] refSubDirList = subdir.listFiles();
                    for (File file : refSubDirList) {
                        String refFileFullPath=file.getAbsolutePath();
                        String newFileFullPath=newPath+"/"+subdir.getName()+"/"+file.getName();
//                        System.out.println(refFileFullPath);
//                        System.out.println(newFileFullPath);
                        boolean areSimilar=Comparor.areSimilarFiles(newFileFullPath,refFileFullPath);
                        if(!areSimilar){
                            changedFiles.add(newFileFullPath);
                        }

                    }
                }
            }
        }   else {

        }


        if(changedFiles.size()==0){
            System.out.println("All files are matching. Validation test is successful!!!");
        }
        else{
            System.out.println("Some of the files have changed!!! \nThe changes are observed in the following files:");
            for(String fileName:changedFiles) {
                System.out.println(fileName);
            }
            System.out.println("\nValidation test is unsuccessful!!!");

        }

    }



}
