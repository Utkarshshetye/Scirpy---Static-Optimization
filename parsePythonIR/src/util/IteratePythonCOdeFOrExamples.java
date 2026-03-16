package util;
import soot.JastAddJ.PackageOrTypeAccess;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class IteratePythonCOdeFOrExamples {

    public static void main(String args[]){
        try {
            File fileIter = new File("iterrows.txt");
            File fileChun = new File("chun.txt");
            File fileDtype = new File("dtype.txt");
            File fileUseCols = new File("usecols.txt");
            File fileFilter = new File("filter.txt");

//            fileIter.createNewFile();
//            fileChun.createNewFile();
//            fileDtype.createNewFile();
//            fileUseCols.createNewFile();
            fileFilter.createNewFile();
//            FileWriter fwIter = new FileWriter(fileIter);
//            FileWriter fwChun = new FileWriter(fileChun);
//            FileWriter fwDtype = new FileWriter(fileDtype);
//            FileWriter fwUsecols = new FileWriter(fileUseCols);
            FileWriter fwFileFilter = new FileWriter(fileFilter);

            // creates a FileWriter Object
            //FileWriter writer = new FileWriter(file);
            //File dir = new File("/home/bhushan/Desktop/Phd Assist/githubNotebooks/bb2733859v_1_1/sample_data/data/notebooks");
            File dir = new File("/media/bhushan/My Passport/PhD/nbBig/bb2733859v_2_1");
            //File dir = new File("/home/bhushan/Desktop/Phd Assist/nbBig/selected");



            File[] directoryListing = dir.listFiles();
            int i=1;
            for (File child : directoryListing) {
                //System.out.println(child.getName());
                //chunksize || drop

                System.out.println("File:"+i); i++;
                int word = hasWord("", child);
                if (word==1) {
                    //System.out.println("chunkFile=" + child.getName());
//                    fwIter.write(child.getName() + "\n");
                    fwFileFilter.write(child.getName() + "\n");
                }
//                else if(word==2){
//                    fwChun.write(child.getName() + "\n");
//                }
//                else if(word==3){
//                    fwDtype.write(child.getName() + "\n");
//                }
//                else if(word==4){
//                    fwUsecols.write(child.getName() + "\n");
//                }
            }
//            fwIter.flush();
//            fwIter.close();
//            fwChun.flush();
//            fwChun.close();
//            fwDtype.flush();
//            fwDtype.close();
//            fwUsecols.flush();
//            fwChun.close();
            fwFileFilter.flush();
            fwFileFilter.close();
        }
        catch (Exception e){

        }
    }
    public static int hasWord(String word, File file) {
        //
        //

        boolean hasWord = false;
        try {
            FileInputStream fin = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));
            String n;
            while ((n = br.readLine()) != null) {
                Scanner scanner = new Scanner(n);
                while (scanner.hasNext()){
                    String nextToken = scanner.next();
                    //   if (nextToken.equalsIgnoreCase("drop") ){
                    if (nextToken.equalsIgnoreCase("filter(") ){
                        //hasWord = true;
                        return 1;
                    }
//                    if (nextToken.equalsIgnoreCase("chunksize") ){
//                       // hasWord = true;
//                        return 2;
//                    }
//
//                    if (nextToken.equalsIgnoreCase("dtype") ){
//                      //  hasWord = true;
//                        return 3;
//                    }
//                    if (nextToken.equalsIgnoreCase("usecols") ){
//                        // hasWord = true;
//                        return 4;
//                    }
                }
            }

        }
        catch (Exception e){

        }
        return 0;
    }
    public static boolean hasWord2(String word, File file) {
        //
        //
    //String destination="/home/bhushan/Desktop/Phd Assist/githubnotebookselectedfiles/";
   // String source="/home/bhushan/Desktop/Phd Assist/githubNotebooks/bb2733859v_1_1/sample_data/data/converted/"+file.getName();

        //File.

        boolean hasWord = false;
        try {
            FileInputStream fin = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));
            String n;
            while ((n = br.readLine()) != null) {
            Scanner scanner = new Scanner(n);
                while (scanner.hasNext()){
                    String nextToken = scanner.next();
                 //   if (nextToken.equalsIgnoreCase("drop") ){
                    if (nextToken.equalsIgnoreCase("sql") ){
                        hasWord = true;

//                        Path dest = Paths.get(destination+"drop/"+file.getName());
//                        Path sourcePath=Paths.get(source);
                        //Files.copy(sourcePath,dest  );
                      break;
                }

                }
            }

        }
            catch (Exception e){

        }
        return hasWord;
    }
}
