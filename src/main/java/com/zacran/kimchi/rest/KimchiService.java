package com.zacran.kimchi.rest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zacran.kimchi.config.KimchiConstants;
import com.zacran.kimchi.cucumber.CucumberEngine;
import com.zacran.kimchi.model.RulesTestProfile;
import com.zacran.kimchi.rules.RulesEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KimchiService {

	public final String CUCUMBER_FILE_NAME = "kimchi-tests.feature";
	public final String CUCUMBER_FILE_EXTENSION = ".feature";
	public final String RULES_FILE_NAME = "kimchi-rules.drl";
	public final String RULES_FILE_EXTENSION = ".drl";

	@Value("${default.rules.file:" + KimchiConstants.DEFUALT_RULES_FILE + "}")
	public String defaultRulesFile;

	@Value("${default.test.file:" + KimchiConstants.DEFUALT_TEST_FILE + "}")
	public String defaultTestFile;

	@Value("${default.test.output.file:" + KimchiConstants.DEFAULT_OUTPUT_FILE + "}")
	public String defaultTestOutputFile;

	@Value("${uploaded.file.dir:" + KimchiConstants.DEFAULT_UPLOADED_DIR + "}")
	public String uploadedDir;

	@Autowired
	public RulesEngine rulesEngine;

	@Autowired
	public CucumberEngine cucumberEngine;

	@Autowired
	public ObjectMapper mapper;

	@PostConstruct
	private void initializeRulesEngine() {
		File defaultRules = new File(defaultRulesFile);
		rulesEngine.compileAndLoadRules(defaultRules);
	}

	private String[] getCucumberArgs(String featureFileLocation) {
		return new String[] { "-m", "--strict", "--plugin", "com.zacran.kimchi.cucumber.copied.JSONFileFormatter",
				"--plugin", "null_summary", "--glue", "com.zacran.kimchi.cucumber", featureFileLocation };
	}

	public void persistFile(MultipartFile uploadedFile) {
		log.info("Uploading file: {}", uploadedFile.getOriginalFilename());

		String name;
		if (uploadedFile.getOriginalFilename().contains(CUCUMBER_FILE_EXTENSION)) {
			name = CUCUMBER_FILE_NAME;
		} else if (uploadedFile.getOriginalFilename().contains(RULES_FILE_EXTENSION)) {
			name = RULES_FILE_NAME;
		} else {
			log.warn("Invalid file type!");
			return;
		}

		try {
			Path path = Paths.get(uploadedDir + name);
			Files.write(path, uploadedFile.getBytes());
		} catch (Exception e) {
			log.error("Error while persisting file", e);
		}
	}

	public String installRules() {
		long start = System.nanoTime();
		String version = rulesEngine.installAsKjar();
		long end = System.nanoTime();
		String rulesInstallationPerformance = (end - start) / 1000000 + "ms";
		log.info("Rules installed in {}", rulesInstallationPerformance);

		return version;
	}

	public RulesTestProfile runRulesTest() {
		// get files from uploadedFiles
		File rules = new File(uploadedDir + RULES_FILE_NAME);
		File tests = new File(uploadedDir + CUCUMBER_FILE_NAME);

		if (!rules.exists()) {
			log.info("No rules uploaded, using default rules...");
			rules = new File(defaultRulesFile);
		}

		if (!tests.exists()) {
			log.info("No tests uploaded, using default tests...");
			tests = new File(defaultTestFile);
		}

		// generate kjar & load statelessKieSession
		log.info("Compiling and loading rules...");
		long startTimeRules = System.nanoTime();
		rulesEngine.compileAndLoadRules(rules);
		long endTimeRules = System.nanoTime();
		String rulesCreationPerformance = (endTimeRules - startTimeRules) / 1000000 + "ms";
		log.info("Rules created and loaded in {}", rulesCreationPerformance);

		// use cucumber runner
		log.info("Starting tests...");
		long startTimeTests = System.nanoTime();
		int exitCode = cucumberEngine.startTests(getCucumberArgs(tests.getAbsolutePath()));
		Boolean testPassed = (exitCode == KimchiConstants.SUCCESS);
		long endTimeTests = System.nanoTime();
		String testRunPerformance = (endTimeTests - startTimeTests) / 1000000 + "ms";
		log.info("Cucumber tests run in {}", testRunPerformance);

		// get results from file
		List<Map<String, Object>> testOutput = getTestOutput();

		RulesTestProfile rulesTestProfile = RulesTestProfile.builder().testPassed(testPassed).testOutput(testOutput)
				.rulesCreationPerformance(rulesCreationPerformance).testRunPerformance(testRunPerformance).build();

		String totalTimePerformance = (endTimeTests - startTimeRules) / 1000000 + "ms";
		log.info("Total time for building rules and running tests: {}", totalTimePerformance);
		return rulesTestProfile;
	}

	public List<Map<String, Object>> getTestOutput() {
		File testOutputFile = new File(defaultTestOutputFile);
		if (testOutputFile.exists()) {
			try {
				List<Map<String, Object>> testOutput = mapper.readValue(testOutputFile,
						new TypeReference<List<Map<String, Object>>>() {
						});
				return testOutput;
			} catch (Exception e) {
				log.error("Error while processing test output", e);
			}
		}

		return null;
	}

}