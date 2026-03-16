package DataFileAnalysis;
/* bhushan created on 23/4/20  */


import DataFileAnalysis.model.DataType;
import DataFileAnalysis.model.FileInfo;
import DataFileAnalysis.model.MetaData;

import java.io.*;
import java.util.*;

public class TestFile {

    public static void main(String args[]){
        String filePath="/home/bhushan/IdeaProjects/PJ/scirpy/pythonIR/SacramentocrimeJanuary2006.csv";
        FileInfo fileInfo=new FileInfo(filePath);
        MetaData metaData=new MetaData(fileInfo);
        System.out.println(fileInfo.getName());
        System.out.println(fileInfo.getFile().lastModified());
        System.out.println(fileInfo.getExtensionType());
        if(fileInfo.getExtensionType().equalsIgnoreCase("csv")){

            if(!isExistingFileMD(metaData)){
                csvMetaDataGenerator(metaData);
            }
            else{
                getCSVMetaData(metaData);
            }
        System.out.println("Meta Data is:"+metaData.getIsCategory());
        }
    }

    public static void getCSVMetaData(MetaData metaData) {

    }
    public static void csvMetaDataGenerator(MetaData metaData) {

        final int ROWS=5;
        int i=0;

        String path=metaData.getFileInfo().getAbsPath();
        FileInfo fileInfo=metaData.getFileInfo();

        ArrayList<String> clmnList=new ArrayList();
        Map<String, String> clmnTypeMap=new HashMap<>();
        String fileCache[][]=new String[ROWS][];
        try {
            FileReader fr = new FileReader(path);
            BufferedReader lineReader = new BufferedReader(fr);
            String line;
            Scanner sc;
            line = lineReader.readLine();
            sc = new Scanner(line);
            sc.useDelimiter(",");
            while(sc.hasNext()) {
                String clmn=sc.next();
                clmnList.add(clmn);
                clmnTypeMap.put(clmn,"");
            }
            sc.close();
            System.out.println(clmnList);

            fileCache=new String[ROWS][clmnList.size()];
            for(i=0;i<ROWS;i++){
                int j=0;
                line = lineReader.readLine();
                sc = new Scanner(line);
                sc.useDelimiter(",");
                while(sc.hasNext()) {
                fileCache[i][j]=sc.next();
                System.out.print(fileCache[i][j]+",");
                j++;
                }
                j=0;
                System.out.println();
                sc.close();
            }
            System.out.println();
            updateTableDataType(clmnTypeMap,clmnList,fileCache);
            boolean[] isCategory=getIsCategory(fileCache);
            String metaPath=metaData.getMetaPath();
            storeUpdateMD(fileInfo, clmnList,clmnTypeMap,isCategory, metaPath);
        }

    catch(Exception e){

    }

    }


    public static DataType getType(String value){
        String type="Ambiguos";
        int iv;
        String sv;
        float fv=0f;
        double dv;
        try{
            iv=Integer.parseInt(value);
            return DataType.int64;
        }
        catch (Exception e){}
        try{
            fv= Float.parseFloat(value);
            return DataType.float32;
        }
        catch (Exception e){}
        try{
            dv=Double.parseDouble(value);
            return DataType.float64;
        }
        catch (Exception e){}

        return DataType.str;
    }

    public static void updateTableDataType(Map clmnTypeMap, List clmnList, String[][] fileCache ){
        DataType[][] fileDataTypes=new DataType[fileCache.length][fileCache[0].length];
        for(int i=0;i< fileCache.length;i++){
            for (int j=0;j<clmnList.size();j++){
                updateIndividualDataType(clmnList.get(j).toString(), getType(fileCache[i][j]),clmnTypeMap);
            }

        }
        System.out.println("here"+clmnTypeMap);
    }
    public static void updateIndividualDataType(String clmn, DataType dataType, Map clmnTypeMap){
            switch(dataType){
                case int64:
                    if(clmnTypeMap.get(clmn).equals("")){
                        clmnTypeMap.replace(clmn, DataType.int64);
                    }
                    break;
                case float32:
                    if(clmnTypeMap.get(clmn).equals("") || clmnTypeMap.get(clmn).equals(DataType.int64)){
                        clmnTypeMap.replace(clmn, DataType.float32);
                    }
                    break;
                case float64:
                    if(clmnTypeMap.get(clmn).equals("") || clmnTypeMap.get(clmn).equals(DataType.int64) || clmnTypeMap.get(clmn).equals(DataType.float32)){
                        clmnTypeMap.replace(clmn, DataType.float64);
                    }
                    break;
                case str:
                    clmnTypeMap.replace(clmn, DataType.str);
                    break;
                default:
                        clmnTypeMap.replace(clmn, DataType.ambiguos);
                        break;
            }

    }

    public static boolean[] getIsCategory(String[][] fileCache){
        boolean[] isCategory=new boolean[fileCache[0].length];
        Set<String> set=new HashSet<>();
        for(int i=0;i<fileCache[0].length;i++) {
            for(int j=0;j<fileCache.length;j++){
                set.add(fileCache[j][i]);
            }
            if(set.size()*2<=fileCache.length){
                isCategory[i]=true;
            }
            set=new HashSet<>();
        }
        for(boolean val:isCategory)
            System.out.print(val +" ");

        return isCategory;

    }

    public static void storeUpdateMD(FileInfo fileInfo,  ArrayList clmnlist, Map clmnTypeMap, boolean[] isCategory, String metaPath) throws IOException{
            storeFileMD(fileInfo, clmnlist, clmnTypeMap, isCategory, metaPath);

    }
    public static boolean isExistingFileMD( MetaData metaData){
        FileInfo fileInfo =metaData.getFileInfo();
        boolean exists =false;
        try {
            File file = new File(metaData.getMetaPath());
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            for (line = br.readLine(); line != null; line = br.readLine()) {
                if (line.equalsIgnoreCase("####: " + "" + fileInfo.getName())) {
                    line = br.readLine();
                   // String lastModified=
                    if(line.equalsIgnoreCase(String.valueOf(fileInfo.getFile().lastModified())))
                    {
                        //SET METADATA as it already exists
                        line = br.readLine();//ignore address//could be compared later
                        Scanner sc;
                        line = br.readLine();//clmnList
                        sc = new Scanner(line);
                        sc.useDelimiter(",");
                        while(sc.hasNext()) {
                            String clmn=sc.next();
                            metaData.getClmnList().add(clmn);
                            metaData.getClmnTypeMap().put(clmn,"");
                        }
                        sc.close();
                        //NOW get MAp
                        int j=0;
                        line = br.readLine();//Type of columns
                        sc = new Scanner(line);
                        sc.useDelimiter(",");
                        while(sc.hasNext()) {
                            String type=sc.next();
                            metaData.getClmnTypeMap().replace(metaData.getClmnList().get(j), type);
                            j++;
                        }
                        //Category Information
                        line = br.readLine();//category
                        sc = new Scanner(line);
                        sc.useDelimiter(",");
                        boolean[] isCategory=new boolean[metaData.getClmnList().size()];
                        j=0;
                        while(sc.hasNext()) {
                            isCategory[j]=sc.nextBoolean();
                            j++;
                        }
                        metaData.setIsCategory(isCategory);
                        exists = true;
                        break;
                    }
                }
            }
            br.close();
            fr.close();
        }
        catch (IOException e){
            System.out.println("Meta data IO Error:"+e.getMessage());
        }
        return exists;
    }

    //NOT WORKING
    public static void deleteFileMD(FileInfo fileInfo, String metaPath) throws IOException{
        try {
            File file = new File(metaPath);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int lineno=0;
            for (line = br.readLine(),lineno=0; line != null; line = br.readLine(),lineno++) {
                if (line.equalsIgnoreCase("####: " + "" + fileInfo.getName())) {
                    break;
                }

            }
            br.close();
            fr.close();
        }
        catch (IOException e){
            System.out.println("Meta data IO Error:"+e.getMessage());
        }


    }

    public static void storeFileMD(FileInfo fileInfo,  ArrayList clmnlist, Map clmnTypeMap, boolean[] isCategory, String metaPath) throws IOException{
        try {
            File file = new File(metaPath);
            FileWriter fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);

            br.write("####: " + "" + fileInfo.getName() + "\n");
            br.write(fileInfo.getFile().lastModified() + "\n");
            br.write(fileInfo.getAbsPath() + "\n");
            for (int i = 0; i < clmnlist.size(); i++){
                 br.write(clmnlist.get(i) + ",");
            }
            br.write("\n");
            for ( int i = 0; i < clmnlist.size(); i++){
            br.write(clmnTypeMap.get(clmnlist.get(i)) + ",");
            }
            br.write("\n");
            for (boolean cat : isCategory) {
                br.write(cat + ",");
            }
            br.write("\n");
            br.write("\n");

            br.close();
            fr.close();

        }
        catch (IOException e){
            System.out.println("Error in creating META DATA FILE: "+e.getMessage());
        }


    }

}
