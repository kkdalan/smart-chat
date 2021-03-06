package com.alan.smart.chat.util;

import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);

    public static ClassLoader getClassLoader() {
	return Thread.currentThread().getContextClassLoader();
    }

    public static Class<?> loadClass(String className) {
	return loadClass(className, true);
    }

    public static Class<?> loadClass(String className, boolean isInitialized) {
	Class<?> clazz;
	try {
	    // 將isInitialized設定為false，如此在載入類別時並不會馬上執行靜態區塊，而會在使用類別建立物件時才執行靜態區塊
	    clazz = Class.forName(className, isInitialized, getClassLoader());
	} catch (ClassNotFoundException e) {
	    LOGGER.error("load class failure", e);
	    throw new RuntimeException(e);
	}
	return clazz;
    }

    public static Set<Class<?>> getClassSet(String packageName) {
	Set<Class<?>> classSet = new HashSet<Class<?>>();
	try {
	    Enumeration<URL> urls = getClassLoader().getResources(packageName.replace(".", "/"));
	    while (urls.hasMoreElements()) {
		URL url = urls.nextElement();
		if (url != null) {

		    String protocal = url.getProtocol();
		    if (protocal.equals("file")) {
			String packagePath = url.getPath().replaceAll("%20", " ");
			addClass(classSet, packagePath, packageName);

		    } else if (protocal.equals("jar")) {
			JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
			if (jarURLConnection != null) {
			    JarFile jarFile = jarURLConnection.getJarFile();
			    if (jarFile != null) {
				Enumeration<JarEntry> jarEntries = jarFile.entries();
				while (jarEntries.hasMoreElements()) {
				    JarEntry jarEntry = jarEntries.nextElement();
				    String jarEntryName = jarEntry.getName();
				    if (jarEntryName.endsWith(".class")) {
					String className = jarEntryName.substring(0, jarEntryName.lastIndexOf("."))
						.replaceAll("/", ".");
					doAddClass(classSet, className);
				    }
				}
			    }
			}
		    }
		}
	    }

	} catch (Exception e) {
	    LOGGER.error("get class set failure", e);
	    throw new RuntimeException(e);
	}
	return classSet;
    }

    private static void addClass(Set<Class<?>> classSet, String packagePath, String packageName) {

	File[] files = new File(packagePath).listFiles(new FileFilter() {
	    @Override
	    public boolean accept(File file) {
		return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
	    }
	});

	for (File file : files) {
	    String fileName = file.getName();

	    if (file.isFile()) {
		String className = fileName.substring(0, fileName.lastIndexOf("."));
		if (StringUtil.isNotEmpty(packageName)) {
		    className = packageName + "." + className;
		}
		doAddClass(classSet, className);
	    } else {
		String subPackagePath = fileName;
		if (StringUtil.isNotEmpty(packagePath)) {
		    subPackagePath = packagePath + "/" + subPackagePath;
		}
		String subPackageName = fileName;
		if (StringUtil.isNotEmpty(subPackageName)) {
		    subPackageName = packageName + "." + subPackageName;
		}
		addClass(classSet, subPackagePath, subPackageName);
	    }
	}

    }

    private static void doAddClass(Set<Class<?>> classSet, String className) {
	Class<?> clazz = loadClass(className, false);
	classSet.add(clazz);
    }

}
