package DataFileAnalysis;
import java.io.File;

public class FileTester {
    public static void main(String[] args) {
        System.out.println(new File("/media/bhushan/nvme/data/vldb/b12/s1.csv").lastModified());
        System.out.println(new File("/media/bhushan/nvme/data/vldb/b12/s2.csv").lastModified());
        System.out.println(new File("/media/bhushan/nvme/data/vldb/b12/s3.csv").lastModified());
        System.out.println(new File("/media/bhushan/nvme/data/vldb/b12/s4.csv").lastModified());
        System.out.println(new File("/media/bhushan/nvme/data/vldb/b12/s5.csv").lastModified());
    }
}
