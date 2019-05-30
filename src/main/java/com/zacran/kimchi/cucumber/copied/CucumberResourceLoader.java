package com.zacran.kimchi.cucumber.copied;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.io.Resource;
import cucumber.runtime.io.ResourceLoader;

/**
* Based on ResourceLoaders from Cucumber's runtime API
*/
public class CucumberResourceLoader implements ResourceLoader {
	public static final String CLASSPATH_SCHEME = "classpath*:";
	public static final String CLASSPATH_SCHEME_TO_REPLACE = "classpath:";

	private final FileResourceLoader fs;

	public CucumberResourceLoader(ClassLoader classLoader) {
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

	private Iterable<Resource> convertToCucumberIterator(org.springframework.core.io.Resource[] resources) {
		List<Resource> results = new ArrayList<>();
		for (org.springframework.core.io.Resource resource : resources) {
			results.add(new CucumberResourceAdaptor(resource));
		}
		return results;
	}

	private static String parseClasspath(String path) {
		String parsedPath = path;
		if (path.startsWith(CLASSPATH_SCHEME_TO_REPLACE)) {
			parsedPath = path.replace(CLASSPATH_SCHEME_TO_REPLACE, CLASSPATH_SCHEME);
		}
		return parsedPath;
	}

	private static boolean isClasspathPath(String path) {
		String parsedPath = parseClasspath(path);
		return parsedPath.startsWith(CLASSPATH_SCHEME);
	}
}
