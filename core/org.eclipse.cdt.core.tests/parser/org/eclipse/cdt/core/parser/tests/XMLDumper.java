package org.eclipse.cdt.core.parser.tests;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class implements a utility that will walk through an object
 * and it's children and create an XML file for it.
 */
public class XMLDumper {

	public static class Test {
		private String msg = "hi";
		
		public String getMsg() {
			return msg;
		}
		
		public Test self = this;
	}
	
	public static void main(String [] args) {
		Test test = new Test();
		try {
			XMLDumper dumper = new XMLDumper(test);
			Document document = dumper.getDocument();
			StringWriter writer = new StringWriter();
		
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(document), new StreamResult(writer));

			System.out.println( "STRXML = " + writer.toString() ); //Spit out DOM as a String
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

	}
	
	private int id = 0;
	private HashMap map = new HashMap();
	private Document document;
	
	public Document getDocument() {
		return document;
	}
	
	public XMLDumper(Object obj) throws ParserConfigurationException {
		document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		document.appendChild(createObject(obj));
	}
	
	private Element createObject(Object obj) {
		Class cls = obj.getClass();
		String clsName = cls.getName();
		clsName = clsName.replace('$', '.');
		
		Element element = document.createElement(clsName);
		map.put(obj, new Integer(id));
		element.setAttribute("id",String.valueOf(id++));
		
		Field [] fields = cls.getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			Field field = fields[i];
			int modifiers = field.getModifiers();
			
			// Skip over static fields
			if (Modifier.isStatic(modifiers))
				continue;
			
			// Skip fields that start with an underscore
			if (field.getName().charAt(0) == '_')
				continue;
			
			Object value = null;
			
			String fieldName = field.getName();
			if (Modifier.isPublic(modifiers)) {
				try {
					value = field.get(obj);
				} catch (Exception e) {
					value = e;
				}
			} else {
				String methodName = "get" +
					fieldName.substring(0, 1).toUpperCase() +
					fieldName.substring(1);
				
				Method method = null;
				try {
					method = cls.getMethod(methodName, null);
				} catch (NoSuchMethodException e) {
					continue;
				}
				
				try {
					value = method.invoke(obj, null);
				} catch (Exception e) {
					value = e;
				}
			}
			
			Element fieldElement = document.createElement(fieldName);
			element.appendChild(fieldElement);
			
			if (value == null)
				return element;
				
			Class type = field.getType();
			if (String.class.isAssignableFrom(type))
				fieldElement.appendChild(document.createTextNode((String)value));
			else if (Integer.class.isAssignableFrom(type))
				fieldElement.appendChild(document.createTextNode(((Integer)value).toString()));
			else if (Exception.class.isAssignableFrom(type))
				fieldElement.appendChild(document.createTextNode(value.toString()));
			else {
				Object v = map.get(value);
				if (v != null)
					fieldElement.setAttribute("refid", v.toString());
				else
					fieldElement.appendChild(createObject(value));
			}
		
		}
		
		return element;
	}
}
