package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PythonCall {
        public static void pythonCall(String fileName){
            try{


//                pythonIR/SourceToIR.py
//                        /home/bhushan/IdeaProjects/PJ_v002/parsePythonIR/src/Main.java
//                        /home/bhushan/IdeaProjects/PJ_v002/pythonIR/SourceToIR.py
                //ProcessBuilder pb = new ProcessBuilder("python","/home/bhushan/IdeaProjects/PJ/PJ_v002/pythonIR/SourceToIR.py",""+fileName);
                String file=System.getProperty("user.dir")+"/pythonIR/AllSourcetoIR.pyy";
                System.out.println(file);
                ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3",file);
                //ProcessBuilder pb = new ProcessBuilder("python","/home/bhushan/IdeaProjects/PJ_v002/pythonIR/SourceToIR.py",""+fileName);
                Process p = pb.start();
                //BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
               // int ret = new Integer(in.readLine()).intValue();
               // System.out.println("value is : "+ret);
            }catch(Exception e){
                String fMessage=e.getMessage();
                System.out.println(e);
            }
        }
    }

