package com.zacran.kimchi.rules;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.appformer.maven.integration.MavenRepository;
import org.kie.api.builder.ReleaseId;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PomGenerator {

	static String getPomString(ReleaseId releaseId) {
		String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
				+ "<modelVersion>4.0.0</modelVersion>\n" + "<groupId>" + releaseId.getGroupId() + "</groupId>\n"
				+ "<artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" + "<version>" + releaseId.getVersion()
				+ "</version>\n" + "</project>";

		return pom;
	}

	public static File getPomFile(ReleaseId releaseId) {
		String fileName = MavenRepository.toFileName(releaseId, null) + ".pom";
		String fileContent = getPomString(releaseId);
		File outputFile = new File(fileName);

		try {
			FileUtils.writeStringToFile(outputFile, fileContent, StandardCharsets.UTF_8, false);
		} catch (IOException e) {
			log.error("", e);
		}

		return outputFile;
	}

}