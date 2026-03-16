package test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Comparor {
    public static boolean areSimilarFiles(String newFilePath, String referenceFilePath) throws FileNotFoundException, IOException {
        BufferedReader newFileReader = new BufferedReader(new FileReader(newFilePath));
        BufferedReader referenceFileReader = new BufferedReader(new FileReader(referenceFilePath));

        boolean areSimilar=true;
        int iLine=0;

        //string line1 and string line2
        String sl1 = newFileReader.readLine();
        String sl2 = referenceFileReader.readLine();


        while(sl1!=null || sl2!=null){
            if(sl1 ==null || sl2==null) {
                areSimilar = false;
                break;
            }
            else if(sl1.equals(sl2)){
                sl1 = newFileReader.readLine();
                sl2 = referenceFileReader.readLine();
                iLine++;
                continue;
            }
            else{
                areSimilar = false;
                break;
            }
        }
        return areSimilar;

    }
}
