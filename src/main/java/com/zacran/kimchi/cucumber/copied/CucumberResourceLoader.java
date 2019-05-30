package com.zacran.kimchi.cucumber.copied;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import cucumber.runtime.io.ClasspathResourceLoader;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.io.Resource;
import cucumber.runtime.io.ResourceLoader;

/**
* Based on ResourceLoaders from Cucumber's runtime API
*/
public class CucumberResourceLoader implements ResourceLoader {
	public static final String CLASSPATH_SCHEME = "classpath*:";
	public static final String CLASSPATH_SCHEME_TO_REPLACE = "classpath:";

	final ClasspathResourceLoader classpath;
	final FileResourceLoader fs;

	public CucumberResourceLoader(ClassLoader classLoader) {
		classpath = new ClasspathResourceLoader(classLoader);
		fs = new FileResourceLoader();
	}

	@Override
	public Iterable<Resource> resources(String path, String suffix) {
		if (isClasspathPath(path)) {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			String locationPattern = path.replace(CLASSPATH_SCHEME_TO_REPLACE, CLASSPATH_SCHEME) + "/**/*" + suffix;
			org.springframework.core.io.Resource[] resources;
			try {
				resources = resolver.getResources(locationPattern);
			} catch (IOException e) {
				resources = null;
				e.printStackTrace();
			}
			return convertToCucumberIterator(resources);
		} else {
			return fs.resources(path, suffix);
		}
	}

	Iterable<Resource> convertToCucumberIterator(org.springframework.core.io.Resource[] resources) {
		List<Resource> results = new ArrayList<>();
		for (org.springframework.core.io.Resource resource : resources) {
			results.add(new CucumberResourceAdaptor(resource));
		}
		return results;
	}

	static String pasrsePackageName(String gluePath) {
		if (isClasspathPath(gluePath)) {
			gluePath = stripClasspathPrefix(gluePath);
		}
		return gluePath.replace('/', '.').replace('\\', '.');
	}

	static String parseClasspath(String path) {
		if (path.startsWith(CLASSPATH_SCHEME_TO_REPLACE)) {
			path = path.replace(CLASSPATH_SCHEME_TO_REPLACE, CLASSPATH_SCHEME);
		}
		return path;
	}

	static boolean isClasspathPath(String path) {
		path = parseClasspath(path);
		return path.startsWith(CLASSPATH_SCHEME);
	}

	static String stripClasspathPrefix(String path) {
		path = parseClasspath(path);
		return path.substring(CLASSPATH_SCHEME.length());
	}
}
