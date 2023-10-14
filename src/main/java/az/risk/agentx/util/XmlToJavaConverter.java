package az.risk.agentx.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

@Log4j2
public class XmlToJavaConverter {

    public static JsonNode parseXmlToJsonNode(String xmlData) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            return xmlMapper.readTree(xmlData);
        } catch (Exception e) {
            log.error("ERROR PARSING : {}", e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            return null;
        }
    }
}
