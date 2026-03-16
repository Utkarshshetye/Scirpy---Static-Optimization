package dummy;

import java.io.File;

public class dummy {
    public static void main(String args[]){
        String[] files={"s1.csv","s2.csv","s3.csv","s4.csv","s5.csv","s6.csv","s7.csv","s8.csv"};
        for(String filename:files){
            File file = new File("/media/bhushan/nvme/data/vldb/b2/"+filename);
            System.out.println(file.getAbsoluteFile()+": "+file.lastModified());
        }


    }
}
