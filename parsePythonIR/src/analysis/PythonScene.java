package analysis;

import soot.Unit;

import java.util.*;

public class PythonScene {
    public static boolean isGroupBy=false;
    public static String pandasAliasName="";
    public static boolean hasPandas=false;
    public static boolean hasFilter=true;
    public static ArrayList<String> dfsFromFile=new ArrayList<>();
    public static Map<String,Unit> dfNametoUnitMap=new HashMap<>();
    public static Map<Unit, String> unitToDfNameMap=new HashMap<>();
    public static Map<Unit, String> unitGetDummiesMap=new HashMap<>();

    public static Set<String> updatedColumns = new HashSet<>();
    public static List<String> ilocUpdatedColPos = new ArrayList<>();
    public static Set<String> ilocDFs = new HashSet<>();
    public static Map<String, String> assignDFtoDFMap = new HashMap<>();
    public static Map<String, List<String>> assignDFtoDFsMap = new HashMap<>();
    public static Map<String, Map<Integer, Integer>> ilocDFIdxMap = new HashMap<>();

    // RND
    public static Set<String> initialCols = new LinkedHashSet<>();


    public static ArrayList<Unit> groupbyStmts=new ArrayList<>();
    public static ArrayList<Unit> compareStmtsDFFiltering=new ArrayList<>();
    public static Map<String,ArrayList<String>> attributesUsedinCompareMap=new HashMap<>();

    public static ArrayList<Unit> DFFilterOps=new ArrayList<>();

    public static boolean isMerge=false;
    public static ArrayList<Unit> mergeStmts=new ArrayList<>();

    public static Map<String, ArrayList<Unit>> varUnitMap=new HashMap<>();

    public static Map<String,ArrayList<String>> nameValueMap=new HashMap<>();

    // Chiranmoy 2-2-24
    public static Set<String> allDfNames = new LinkedHashSet<>();
    public static Set<String> imported = new HashSet<>();

    // Predicate Pushdown
    public static Set<String> nonRowPreservingOps = new HashSet<>(Arrays.asList(
            "groupby", "agg", "pivot", "pivot_table", "merge", "join",
            "concat", "melt", "explode", "drop_duplicates", "dropna", "sample",
            "set_index", "reset_index"
    ));

    public static Set<String> printDfs = new HashSet<>();
    public static Set<String> usedDFs = new HashSet<>();
    public static Set<String> killDfs = new HashSet<>();

    public static void updateVarUnitMap(String lvalue, Unit unit){
        if(varUnitMap.get(lvalue)==null){
            ArrayList<Unit> list=new ArrayList<>();
            list.add(unit);
            varUnitMap.put(lvalue,list);
        }
        else{
            varUnitMap.get(lvalue).add(unit);
        }

    }

}