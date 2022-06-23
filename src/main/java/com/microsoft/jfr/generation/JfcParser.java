package com.microsoft.jfr.generation;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class JfcParser {

    private final Map<String, List<Setting>> settingsByEventName = new LinkedHashMap<>();
    private final String sourceFilePath;
    private final SAXParserFactory factory = SAXParserFactory.newInstance();

    JfcParser(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    Map<String, List<Setting>> getSettingsByEventName() {
        return settingsByEventName;
    }
    void parse() {
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(sourceFilePath, new JfcToMap(this));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class JfcToMap extends DefaultHandler {

        private String eventName;

        private String settingName;

        private StringBuilder settingValue = new StringBuilder();

        private List<Setting> settings = new ArrayList<>();

        private boolean settingProcessed;
        private final JfcParser jfcParser;

        JfcToMap(JfcParser jfcParser) {
            this.jfcParser = jfcParser;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {

            if("event".equals(qName)) {
               this.eventName = attributes.getValue("name");
            }

            if ("setting".equals(qName)) {
                this.settingProcessed = true;
                this.settingName = attributes.getValue("name");
                this.settingValue = new StringBuilder();
            }

        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (settingProcessed){
                settingValue.setLength(0);
                settingValue.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {

            if ("setting".equals(qName)) {
                Setting setting = new Setting(settingName, settingValue.toString());
                this.settings.add(setting);
                this.settingName = null;
                this.settingValue = null;
                this.settingProcessed = false;
            }

            if ("event".equals(qName)) {
                this.jfcParser.settingsByEventName.put(eventName, settings);
                this.settings = new ArrayList<>();
            }
        }

    }

}
