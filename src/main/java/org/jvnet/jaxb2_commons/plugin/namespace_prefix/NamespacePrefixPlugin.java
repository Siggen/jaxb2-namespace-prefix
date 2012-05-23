package org.jvnet.jaxb2_commons.plugin.namespace_prefix;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JPackage;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import org.xml.sax.ErrorHandler;

/**
 * This plugin adds {@link javax.xml.bind.annotation.XmlNs} annotations to <i>package-info.java</i> file according to an external mapping file. Those annotations associate namespace prefixes with XML
 * namespace URIs.
 * <p/>
 * External mapping file format is the following:
 * <pre>
 *     [fully.qualified.package.name]
 *     XML-namespace-URI=prefix
 * </pre>
 * <p/>
 * Example:
 * <pre>
 *     [ch.ech.ech0006]
 *     http://www.ech.ch/xmlns/eCH-0006/1=eCH-0006-1
 *     http://www.ech.ch/xmlns/eCH-0006/2=eCH-0006-2
 *
 *     [ch.ech.ech0007]
 *     http://www.ech.ch/xmlns/eCH-0007/3=eCH-0007-3
 *
 *     ...
 * </pre>
 *
 * @author Manuel Siggen (c) 2012 Etat-de-Vaud (www.vd.ch)
 */
@SuppressWarnings({"UnusedDeclaration"})
public class NamespacePrefixPlugin extends Plugin {

	private Map<String, List<Pair>> prefixMapping = new HashMap<String, List<Pair>>();

	@Override
	public String getOptionName() {
		return "Xnamespace-prefix";
	}

	@Override
	public String getUsage() {
		return "  -Xnamespace-prefix    :  activate namespaces prefix customizations\n" +
				"  -Xnamespace-prefix-file:<prefixmappingfile>    :  specify prefix mapping file";
	}

	@Override
	public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
		final String argument = args[i];
		if (argument.startsWith("-Xnamespace-prefix-file")) {
			final String filename = argument.split(":")[1];
			prefixMapping = parseMapping(filename);
			return 1;
		}
		return 0;
	}

	/**
	 * Parse the mapping file.
	 * <p/>
	 * Format is :
	 * <pre>
	 * [ch.vd.fully.qualified.package.name]
	 *     http://www.vd.ch/namespace/1=prefix-1
	 *     http://www.vd.ch/namespace/2=prefix-2
	 *
	 * [ch.vd.other.fully.qualified.package.name]
	 *     http://www.vd.ch/other/namespace/1=other-1
	 *     http://www.vd.ch/other/namespace/2=other-2
	 * </pre>
	 *
	 * @param filename file name of prefix mapping file
	 * @return the parsed mapping
	 * @throws FileNotFoundException
	 */
	private static Map<String, List<Pair>> parseMapping(String filename) throws FileNotFoundException {

		final Map<String, List<Pair>> map = new HashMap<String, List<Pair>>();

		List<Pair> list = null;

		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(filename));
			while (scanner.hasNext()) {
				final String token = scanner.next();
				if (token.charAt(0) == '[') { // got a package name
					final String packageName = token.replaceAll("[\\[\\]]", "");
					list = new ArrayList<Pair>();
					map.put(packageName, list);
				}
				else if (list != null) { // got a couple namespace/prefix
					final String[] line = token.split("=");
					final String namespace = line[0];
					final String prefix = line[1];
					list.add(new Pair(namespace, prefix));
				}
			}
		}
		finally {
			if (scanner != null) {
				scanner.close();
			}
		}

		return map;
	}

	@Override
	public boolean run(final Outline outline, final Options options, final ErrorHandler errorHandler) {

		final JClass xmlNsClass = outline.getCodeModel().ref(XmlNs.class);
		final JClass xmlSchemaClass = outline.getCodeModel().ref(XmlSchema.class);

		for (PackageOutline packageOutline : outline.getAllPackageContexts()) {
			final JPackage p = packageOutline._package();

			// is there is defined mapping for the package ?
			final List<Pair> list = prefixMapping.get(p.name());
			if (list == null || list.isEmpty()) {
				continue;
			}

			// if so, add wanted annotations
			final JAnnotationUse xmlSchemaAnnotation = getXmlSchemaAnnotation(p, xmlSchemaClass);
			if (xmlSchemaAnnotation != null) {
				final JAnnotationArrayMember members = xmlSchemaAnnotation.paramArray("xmlns");

				for (Pair pair : list) {
					addNamespacePrefix(xmlNsClass, members, pair.getNamespace(), pair.getPrefix());
				}
			}
		}

		return true;
	}

	private static void addNamespacePrefix(JClass xmlNsClass, JAnnotationArrayMember members, String namespace, String prefix) {
		JAnnotationUse ns = members.annotate(xmlNsClass);
		ns.param("namespaceURI", namespace);
		ns.param("prefix", prefix);
	}

	@SuppressWarnings("unchecked")
	private static JAnnotationUse getXmlSchemaAnnotation(JPackage p, JClass xmlSchemaClass) {
		try {
			final Field annotationsField = p.getClass().getDeclaredField("annotations");
			annotationsField.setAccessible(true);
			final List<JAnnotationUse> annotations = (List<JAnnotationUse>) annotationsField.get(p);
			if (annotations != null) {
				for (JAnnotationUse annotation : annotations) {
					final JClass clazz = getAnnotatedJClass(annotation);
					if (clazz == xmlSchemaClass) {
						return annotation;
					}
				}
			}

		}
		catch (Exception e) {
			throw new RuntimeException("Class [" + p.getClass().getName() + "] : " + e.getMessage(), e);
		}
		return null;
	}

	private static JClass getAnnotatedJClass(JAnnotationUse annotation) throws NoSuchFieldException, IllegalAccessException {
		final Field clazzField = annotation.getClass().getDeclaredField("clazz");
		clazzField.setAccessible(true);
		return (JClass) clazzField.get(annotation);
	}

	private static class Pair {
		private String namespace;
		private String prefix;

		private Pair(String namespace, String prefix) {
			this.namespace = namespace;
			this.prefix = prefix;
		}

		public String getNamespace() {
			return namespace;
		}

		public String getPrefix() {
			return prefix;
		}
	}
}
