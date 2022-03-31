/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */

package com.distrimind.madkit.kernel;

import com.distrimind.madkit.gui.GUIProvider;
import com.distrimind.madkit.gui.MASModel;
import com.distrimind.madkit.util.XMLUtilities;
import com.distrimind.util.UtilClassLoader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * The MadkitClassLoader is the class loader used by MaDKit. It enables some
 * specific features such as class hot reloading, jar loading, etc.
 * 
 * @author Fabien Michel
 * @author Jacques Ferber
 * @author Jason Mahdjoub
 * @since MaDKit 4.0
 * @since MadkitLanEdition 1.0
 * @version 6.0
 * 
 */
final public class MadkitClassLoader extends UtilClassLoader { // NO_UCD


	private static Set<String> agentClasses;
	private static Set<File> xmlFiles;
	private static Set<String> mains;
	private static Set<MASModel> demos;
	private static Set<URL> scannedURLs;
	private static MadkitClassLoader currentMCL;

	static {
		init();
	}

	public static void init() {
		currentMCL = new MadkitClassLoader(MadkitClassLoader.class.getClassLoader(), null);
	}

	private MadkitClassLoader(final ClassLoader parent, Collection<String> toReload) {
		super(parent, toReload);
	}

	/**
	 * Returns the last class loader, thus having all the loaded jars on the
	 * classpath.
	 * 
	 * @return the last class loader.
	 */
	public static MadkitClassLoader getLoader() {
		return currentMCL;
	}



	/**
	 * Loads all the jars present in a directory
	 * 
	 * @param directoryPath
	 *            directory's path
	 * @return <code>true</code> if at least one new jar has been loaded
	 */
	public static boolean loadJarsFromDirectory(final String directoryPath) {
		final File demoDir = new File(directoryPath);
		boolean hasLoadSomething = false;
		if (demoDir.isDirectory()) {
			for (final File f : Objects.requireNonNull(demoDir.listFiles())) {
				if (f.getName().endsWith(".jar")) {
					try {
						if (loadUrl(f.toURI().toURL()))
							hasLoadSomething = true;
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return hasLoadSomething;
	}



	private static void scanClassPathForAgentClasses() {
		if (scannedURLs == null) {
			scannedURLs = new HashSet<>();
			agentClasses = new TreeSet<>();
			demos = new HashSet<>();
			// mdkFiles = new HashSet<>();
			xmlFiles = new HashSet<>();
			mains = new HashSet<>();
		}
		for (URL dir : getLoader().getURLs()) {

			if (!scannedURLs.add(dir))
				continue;

			if (dir.getFile().endsWith(".jar")) {

				try (JarFile jarFile = ((JarURLConnection) new URL("jar:" + dir + "!/").openConnection())
						.getJarFile()) {
					scanJarFileForLaunchConfig(jarFile);
					// agentClasses.addAll(scanJarFileForLaunchConfig(jarFile));
				} catch (IOException e) {
					// madkit.getLogger().log(Level.SEVERE, "web repo conf is not valid", e);
					e.printStackTrace();
				}
			} else {
				scanFolderForAgentClasses(new File(dir.getFile()), null, dir.getPath());
			}
		}
	}

	/**
	 * 
	 * Adds a directory or a jar file to the class path.
	 * 
	 * @param url
	 *            the resource to add
	 * @return <code>true</code> if this url was not already loaded
	 */
	public static boolean loadUrl(URL url) {
		final int size = getLoader().getURLs().length;// TODO could check if present
		getLoader().addURL(url);
		if (size != getLoader().getURLs().length) {// truly loaded
			System.setProperty("java.class.path",
					System.getProperty("java.class.path") + File.pathSeparator + url.getPath());
			GUIProvider.updateAllMenus();
			return true;
		}
		return false;
	}

	/**
	 * Returns all the session configurations available on the class path
	 * 
	 * @return a set of session configurations available on the class path
	 */
	public static Set<MASModel> getAvailableConfigurations() {
		scanClassPathForAgentClasses();
		return new HashSet<>(demos);
	}

	/**
	 * Returns the names of all the available agent classes
	 * 
	 * @return All the agent classes available on the class path
	 */
	public static Set<String> getAllAgentClasses() {
		scanClassPathForAgentClasses();
		return new TreeSet<>(agentClasses);
	}

	/**
	 * Returns the names of all the xml configuration files available
	 * 
	 * @return All the xml configuration file available on the class path
	 */
	public static Set<File> getXMLConfigurations() {
		scanClassPathForAgentClasses();
		return new TreeSet<>(xmlFiles);
	}

	/**
	 * @return all the agent classes having a <code>main</code> method.
	 */
	public static Set<String> getAgentsWithMain() {
		scanClassPathForAgentClasses();
		return new TreeSet<>(mains);
	}

	private static void scanJarFileForLaunchConfig(final JarFile jarFile) {
		Attributes projectInfo = null;
		try {
			projectInfo = jarFile.getManifest().getAttributes("MaDKit-Project-Info");
		} catch (Exception e) {
			// not a valid MDK jar file
		}
		if (projectInfo != null) {
			// Logger l = madkit.getLogger();
			String mdkArgs = projectInfo.getValue("MaDKit-Args");// TODO MDK files
			if (check(mdkArgs)) {
				final String projectName = projectInfo.getValue("Project-Name").trim();
				final String projectDescription = projectInfo.getValue("Description").trim();
				MASModel mas = new MASModel(projectName, mdkArgs.trim().split("\\s+"), projectDescription);
				demos.add(mas);
			}
			mdkArgs = projectInfo.getValue("Main-Classes");// recycling
			if (check(mdkArgs)) {
				mains.addAll(Arrays.asList(mdkArgs.split(",")));
			}
			mdkArgs = projectInfo.getValue("XML-Files");// recycling
			if (check(mdkArgs)) {
				for (String f : mdkArgs.split(",")) {
					xmlFiles.add(new File(f));
				}
			}
			agentClasses.addAll(Arrays.asList(projectInfo.getValue("Agent-Classes").split(",")));
		}
	}

	private static boolean check(String args) {
		return args != null && !args.trim().isEmpty();
	}
	
	
	private static void scanFolderForAgentClasses(final File file, final String pckName, String currentUrlPath) {
		final File[] files = file.listFiles();
		if (files != null) {
			for (File f : files) {
				final String fileName = f.getName();
				if (f.isDirectory()) {
					
					scanFolderForAgentClasses(f, pckName == null ? fileName : pckName + "." + fileName, currentUrlPath);
				} else {
					if (fileName.endsWith(".class")) {
						String className = (pckName == null ? "" : pckName + ".") + fileName.replace(".class", "");
						if (isAgentClass(className)) {
							agentClasses.add(className);
						}
					}
					/*
					 * else if (fileName.endsWith(".mdk")) { mdkFiles.add(new
					 * File(f.getPath().substring(currentUrlPath.length()))); }
					 */
					else if (fileName.endsWith(".xml")) {
						final File xmlFile = new File(f.getPath().substring(currentUrlPath.length()));
						try {
							// System.err.println(xmlFile);
							Document dom = XMLUtilities.getDOM(xmlFile);
							if (dom != null && dom.getDocumentElement().getNodeName().equals(XMLUtilities.MDK)) {
								xmlFiles.add(xmlFile);
							}
						} catch (SAXException | IOException | ParserConfigurationException e) {
							// e.printStackTrace();
							// FIXME should be logged
						}
					}
				}
			}
		}
	}

	private static boolean isAgentClass(final String className) {
		try {
			final Class<?> cl = getLoader().loadClass(className);
			//noinspection ConstantConditions
			if (cl != null && AbstractAgent.class.isAssignableFrom(cl) && cl.getDeclaredConstructor() != null
					&& (!Modifier.isAbstract(cl.getModifiers())) && Modifier.isPublic(cl.getModifiers())) {
				try {
					cl.getDeclaredMethod("main", String[].class);
					mains.add(className);// if previous line succeeded
				} catch (NoSuchMethodException ignored) {
				}
				return true;
			}
		} catch (VerifyError | ClassNotFoundException | NoClassDefFoundError | SecurityException
				| NoSuchMethodException e) {// FIXME just a reminder
		}
		return false;
	}

	private static String findJExecutable(File dir, String executable) {
		dir = new File(dir, "bin");
		if (dir.exists()) {
			for (File candidate : Objects.requireNonNull(dir.listFiles())) {
				if (candidate.getName().contains(executable)) {
					return candidate.getAbsolutePath();
				}
			}
		}
		return null;
	}

	/**
	 * Find a JDK/JRE program
	 * 
	 * @param executable
	 *            the name of the Java program to look for. E.g. "jarsigner",
	 *            without file extension.
	 * @return the path to the executable or <code>null</code> if not found.
	 */
	public static String findJavaExecutable(String executable) {
		File lookupDir = new File(System.getProperty("java.home"));
		String exe = MadkitClassLoader.findJExecutable(lookupDir, executable);
		if (exe != null)// was jdk dir
			return exe;
		lookupDir = lookupDir.getParentFile();
		exe = MadkitClassLoader.findJExecutable(lookupDir, executable);
		if (exe != null)// was jre dir in jdk
			return exe;
		while (lookupDir != null) {
			for (final File dir : Objects.requireNonNull(lookupDir.listFiles(pathname -> {
				if (pathname.isDirectory()) {
					final String dirName = pathname.getName();
					return dirName.contains("jdk") || dirName.contains("java");
				}
				return false;
			}))) {
				exe = MadkitClassLoader.findJExecutable(dir, executable);
				if (exe != null)
					return exe;
			}
			lookupDir = lookupDir.getParentFile();
		}
		return null;
	}

	/*
	 * This is only used by ant scripts for building MDK jar files. This will create
	 * a file in java.io.tmpdir named agents.classes containing the agent classes
	 * which are on the class path and other information
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	/*
	 * public static void main(String[] args) throws FileNotFoundException,
	 * IOException { final java.util.Properties p = new java.util.Properties();
	 * p.setProperty("agents.classes",
	 * normalizeResult(MadkitClassLoader.getAllAgentClasses()));
	 * //p.setProperty("mdk.files",
	 * normalizeResult(MadkitClassLoader.getMDKFiles())); p.setProperty("xml.files",
	 * normalizeResult(MadkitClassLoader.getXMLConfigurations()));
	 * p.setProperty("main.classes",
	 * normalizeResult(MadkitClassLoader.getAgentsWithMain())); final String
	 * findJavaExecutable = findJavaExecutable("jarsigner"); if (findJavaExecutable
	 * != null) { p.setProperty("jarsigner.path", findJavaExecutable); } try(final
	 * FileOutputStream out = new FileOutputStream(new
	 * File(System.getProperty("java.io.tmpdir")+File.separatorChar+
	 * "agentClasses.properties"))){
	 * p.store(out,MadkitClassLoader.getLoader().toString()); } }
	 */

	/**
	 * format the toString of a collection: Remove brackets and space
	 * 
	 * 
	 * @return the parsed string
	 */
	/*
	 * private static String normalizeResult(final Set<?> set) { final String s =
	 * set.toString(); return s.substring(1,s.length()-1).replace(", ", ","); }
	 */

	@Override
	public String toString() {
		return "MCL CP : " + Arrays.deepToString(getURLs())
		/* +"\nmains="+getAgentsWithMain() */;// TODO check why this error occurs
	}

}
