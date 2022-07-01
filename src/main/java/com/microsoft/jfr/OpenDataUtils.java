package com.microsoft.jfr;

import javax.management.openmbean.*;
import java.util.Map;

class OpenDataUtils {

    private OpenDataUtils() {
    }

    /**
     * Convert the Map to TabularData
     * @param options A map of key-value pairs.
     * @return TabularData
     * @throws OpenDataException Can only be raised if there is a bug in this code.
     */
    static TabularData makeOpenData(final Map<String, String> options) throws OpenDataException {
        // Copied from newrelic-jfr-core
        final String typeName = "java.util.Map<java.lang.String, java.lang.String>";
        final String[] itemNames = new String[]{"key", "value"};
        final OpenType<?>[] openTypes = new OpenType[]{SimpleType.STRING, SimpleType.STRING};
        final CompositeType rowType = new CompositeType(typeName, typeName, itemNames, itemNames, openTypes);
        final TabularType tabularType = new TabularType(typeName, typeName, rowType, new String[]{"key"});
        final TabularDataSupport table = new TabularDataSupport(tabularType);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            Object[] itemValues = {entry.getKey(), entry.getValue()};
            CompositeData element = new CompositeDataSupport(rowType, itemNames, itemValues);
            table.put(element);
        }
        return table;
    }
}
