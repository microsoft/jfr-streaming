package com.microsoft.jfr.generation;

import java.util.List;
import java.util.Map;

/**
 * This class allows you to convert a .jfc configuration file into a Java class.
 */
public class JfcToJava {

    private JfcToJava() {
    }

    /**
     * Converts a .jfc configuration file into a Java class that provides a <code>java.util.Map</code> containing a JFR event configuration.
     * @param jfcPath Path to a .jfc file
     * @param javaClassDirPath Path directory of the generated Java class
     * @param className Class name of the generated Java class
     */
    public static void generateJavaMap(String jfcPath, String javaClassDirPath, String className) {
        JfcParser jfcParser = new JfcParser(jfcPath);
        jfcParser.parse();
        Map<String, List<Setting>> settingsByEventName = jfcParser.getSettingsByEventName();

        JfrSettingsAsMapClassGenerator.generate(settingsByEventName, className, javaClassDirPath);
    }

}
