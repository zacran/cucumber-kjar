package com.zacran.kimchi.cucumber.copied;

import java.io.IOException;
import java.io.InputStream;

import cucumber.runtime.io.Resource;

/**
* Based on Adaptors in Cucumber's runtime API
*/
public class CucumberResourceAdaptor implements Resource {

	org.springframework.core.io.Resource springResource;

	public CucumberResourceAdaptor(org.springframework.core.io.Resource springResource) {
		this.springResource = springResource;
	}

	@Override
	public String getPath() {
		try {
			return springResource.getFile().getPath();
		} catch (IOException e) {
			try {
				return springResource.getURL().toString();
			} catch (IOException e1) {
				e1.printStackTrace();
				return "";
			}
		}
	}

	@Override
	public String getAbsolutePath() {
		try {
			return springResource.getFile().getAbsolutePath();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.springResource.getInputStream();
	}

	@Override
	public String getClassName(String extension) {

		String path = this.getPath();
		if (path.startsWith("jar:")) {
			path = path.substring(path.lastIndexOf("!") + 2);
			return path.substring(0, path.length() - extension.length()).replace('/', '.');
		} else {
			path = path.substring(path.lastIndexOf("classes") + 8);
			return path.substring(0, path.length() - extension.length()).replace('\\', '.');
		}

	}
}