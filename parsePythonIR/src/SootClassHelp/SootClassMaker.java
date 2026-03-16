package SootClassHelp;



        import soot.Modifier;
        import soot.Scene;
        import soot.SootClass;
        import soot.SootMethod;
        import soot.jimple.toolkits.callgraph.CallGraph;

//For help see the link: https://www.sable.mcgill.ca/soot/tutorial/createclass/
public class SootClassMaker {
    SootClass sootClass;

    //TEST
    public static void main(String args[]){
        SootClass sClass = new SootClass("Hello", Modifier.PUBLIC);
        System.out.println(sClass);
    }

    public SootClass getBareSootClass(String filename) {
        SootClass sClass = new SootClass(filename, Modifier.PUBLIC);
        return sClass;
    }

    public SootClass getBareSootClass(String filename, int modifier) {
        SootClass sClass = new SootClass(filename, modifier);
        sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        //Add this class to Scene at the caller using:     Scene.v().addClass(sClass);
        return sClass;
    }
    public SootClass getSootClass(String filename, SootMethod sootMethod) {
        return getSootClass(filename, Modifier.PUBLIC,sootMethod);
    }
    public SootClass getSootClass(String filename, int modifier, SootMethod sootMethod) {

       // Scene.v().loadClassAndSupport("java.lang.Object");
        //Scene.v().loadClassAndSupport("java.lang.System");

        SootClass sClass = new SootClass(filename, modifier);
        //sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));

        Scene.v().addClass(sClass);

        sClass.addMethod(sootMethod);

       // CallGraph cg = Scene.v().getCallGraph();


        return sClass;
    }
}
