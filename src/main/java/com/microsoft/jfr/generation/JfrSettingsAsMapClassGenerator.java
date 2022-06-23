package com.microsoft.jfr.generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class JfrSettingsAsMapClassGenerator {

    private static final String MAP_PUT_TEMPLATE = "MAP_PUT";
    private static final String CLASS_NAME = "CLASS_NAME";
    private static final String MAP_SUPPLIER_TEMPLATE =
            "import java.util.HashMap;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.function.Supplier;\n" +
                    "\n" +
                    "public class "+ CLASS_NAME + " {\n" +
                    "\n" +
                    "    public static Supplier<Map<String, String>>  MAP_SUPPLIER = () -> {\n" +
                    "\n" +
                    "        Map<String, String> settingsAsMap = new HashMap<>();\n" +
                    "\n" +
                    MAP_PUT_TEMPLATE +
                    "        return settingsAsMap;\n" +
                    "    };\n" +
                    "\n" +
                    "}";

    static void generate(Map<String, List<Setting>> settingsByEventName, String className, String targetDirPath) {


        StringBuilder mapPutCodeBuilder = new StringBuilder();
        for (Map.Entry<String, List<Setting>> settingEntry : settingsByEventName.entrySet()) {
            String eventName = settingEntry.getKey();
            List<Setting> settings = settingEntry.getValue();
            String header = "\t\t";
            for (Setting setting : settings) {
                mapPutCodeBuilder.append(header +"settingsAsMap.put(\"" + eventName + "#" + setting.name + "\",\"" + setting.value + "\");");
                mapPutCodeBuilder.append(System.lineSeparator());
            }
            mapPutCodeBuilder.append(System.lineSeparator());
        }

        byte[] content = MAP_SUPPLIER_TEMPLATE.replace(CLASS_NAME, className)
                                              .replace(MAP_PUT_TEMPLATE, mapPutCodeBuilder.toString()).getBytes();

        try {
            Path javaClassPath = FileSystems.getDefault().getPath(targetDirPath, className + ".java");
            File javaFile = javaClassPath.toFile();
            javaFile.createNewFile();
            Files.write(javaClassPath, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
