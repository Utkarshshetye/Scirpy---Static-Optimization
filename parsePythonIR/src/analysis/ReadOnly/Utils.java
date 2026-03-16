package analysis.ReadOnly;

import analysis.PythonScene;
import ir.expr.Str;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ReadUtility {

    public static void printMap(Map<String, Map<Integer, Integer>> ilocDFIdxMap){
        StringBuilder sb = new StringBuilder();
        String dfKey = "";
        Map<Integer, Integer> value = new HashMap<>();
        List<String> orgCol = new ArrayList<>(PythonScene.initialCols);
        final String BLUE = "\u001B[34m";

        // {df3={0=0, 1=2, 2=1}, df2={0=1, 1=2}, df={0=0, 1=1, 2=2}, df5={0=2}, df4={0=2, 1=1, 2=0}, df6={0=2, 1=1}, df9={0=0, 1=1, 2=2}, df8={0=0, 1=1, 2=2}, df11={0=0, 1=1, 2=2}, df10={0=0, 1=1, 2=2}}

        for (Map.Entry<String, Map<Integer, Integer>> entry : ilocDFIdxMap.entrySet()){
            dfKey = entry.getKey();
            value = entry.getValue();

            sb.append(dfKey);
            sb.append(":\n \n");

            for (Map.Entry<Integer, Integer> currVal : value.entrySet()){

                 sb.append("\t" + "col_" + currVal.getKey() + " -> " + "col_" + currVal.getValue() + " {" + orgCol.get(currVal.getValue()) + "} "+ "\n" );
            }
            sb.append("\n");
        }

        System.out.println(sb);

        // System.out.println(ilocDFIdxMap);
    }

}
