package statistic;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public class SysTest {

    private static void printUsage() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.getName().startsWith("getFreePhysicalMemorySize")
                    && Modifier.isPublic(method.getModifiers())) {
                Object value;
                try {
                    value = method.invoke(operatingSystemMXBean);
                } catch (Exception e) {
                    value = e;
                } // try
                long result = Long.divideUnsigned((Long)value, 1000000);
                System.out.println(method.getName() + " = " + value);
                System.out.println(method.getName() + " = " + result);
            } // if
        } //
//        OperatingSystemImpl os=(OperatingSystemImpl)operatingSystemMXBean;
//        System.out.println("Available System memory in MBs is:"+os.getClass());
    }
    public static void main(String args[]){
        printUsage();
    }
}
