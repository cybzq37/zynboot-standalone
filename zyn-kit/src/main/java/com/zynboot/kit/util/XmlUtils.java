package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XML 工具类。
 * <p>
 * 基于 JDK 内置 javax.xml，零外部依赖。
 * 支持解析、序列化、Map 互转、XSD 校验、转义等。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class XmlUtils {

    // ==================== 解析 ====================

    /**
     * XML 字符串解析为 Document。
     */
    public static Document parse(String xml) {
        if (StringUtils.isBlank(xml)) throw new IllegalArgumentException("xml must not be blank");
        try {
            DocumentBuilderFactory factory = newSecureFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    /**
     * InputStream 解析为 Document。
     */
    public static Document parse(InputStream in) {
        if (in == null) throw new IllegalArgumentException("inputStream must not be null");
        try {
            DocumentBuilderFactory factory = newSecureFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    /**
     * 文件解析为 Document。
     */
    public static Document parseFile(Path path) throws Exception {
        DocumentBuilderFactory factory = newSecureFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(path.toFile());
    }

    // ==================== 序列化 ====================

    /**
     * Document 序列化为 XML 字符串。
     */
    public static String toString(Document doc) {
        return toString(doc, false);
    }

    /**
     * Document 序列化为 XML 字符串（可选格式化）。
     */
    public static String toString(Document doc, boolean prettyPrint) {
        if (doc == null) return "";
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            if (prettyPrint) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize XML: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化 XML 字符串（美化输出）。
     */
    public static String prettyPrint(String xml) {
        return toString(parse(xml), true);
    }

    // ==================== XML ↔ Map ====================

    /**
     * XML 字符串转 Map（简单 key-value 结构）。
     * <p>
     * 输入：{@code <root><name>test</name><age>18</age></root>}
     * 输出：{@code {name=test, age=18}}
     */
    public static Map<String, String> toMap(String xml) {
        Document doc = parse(xml);
        Element root = doc.getDocumentElement();
        Map<String, String> map = new LinkedHashMap<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                map.put(node.getNodeName(), node.getTextContent());
            }
        }
        return map;
    }

    /**
     * Map 转 XML 字符串。
     * <p>
     * 输入：{@code {name=test, age=18}}
     * 输出：{@code <root><name>test</name><age>18</age></root>}
     */
    public static String fromMap(Map<String, String> map, String rootName) {
        if (map == null || map.isEmpty()) return "";
        String root = StringUtils.isBlank(rootName) ? "root" : rootName;
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(root).append(">");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("<").append(entry.getKey()).append(">");
            sb.append(escape(entry.getValue()));
            sb.append("</").append(entry.getKey()).append(">");
        }
        sb.append("</").append(root).append(">");
        return sb.toString();
    }

    // ==================== XML 提取 ====================

    /**
     * 从 XML 中提取指定标签的文本内容（第一个匹配）。
     */
    public static String extractText(String xml, String tagName) {
        if (StringUtils.isBlank(xml) || StringUtils.isBlank(tagName)) return null;
        Document doc = parse(xml);
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    /**
     * 从 XML 中提取指定标签的所有文本内容。
     */
    public static List<String> extractAllText(String xml, String tagName) {
        if (StringUtils.isBlank(xml) || StringUtils.isBlank(tagName)) return List.of();
        Document doc = parse(xml);
        NodeList nodes = doc.getElementsByTagName(tagName);
        List<String> results = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            results.add(nodes.item(i).getTextContent());
        }
        return results;
    }

    /**
     * 获取根元素名称。
     */
    public static String getRootElementName(String xml) {
        return parse(xml).getDocumentElement().getTagName();
    }

    // ==================== XSD 校验 ====================

    /**
     * 校验 XML 是否符合 XSD Schema。
     */
    public static boolean validate(String xml, String xsd) {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new StreamSource(new StringReader(xsd)));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验 XML 是否符合 XSD 文件。
     */
    public static boolean validate(String xml, Path xsdPath) throws Exception {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(xsdPath.toFile());
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
        return true;
    }

    // ==================== 转义 ====================

    /**
     * XML 特殊字符转义。
     */
    public static String escape(String text) {
        if (StringUtils.isBlank(text)) return text;
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * XML 特殊字符反转义。
     */
    public static String unescape(String text) {
        if (StringUtils.isBlank(text)) return text;
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    // ==================== 判断 ====================

    /**
     * 判断字符串是否为合法 XML。
     */
    public static boolean isXml(String text) {
        if (StringUtils.isBlank(text)) return false;
        try {
            parse(text.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 内部 ====================

    private static DocumentBuilderFactory newSecureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE 防护
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
