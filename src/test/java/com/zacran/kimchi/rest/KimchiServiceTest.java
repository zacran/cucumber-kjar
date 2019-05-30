package com.zacran.kimchi.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zacran.kimchi.config.KimchiConstants;
import com.zacran.kimchi.cucumber.CucumberEngine;
import com.zacran.kimchi.model.RulesTestProfile;
import com.zacran.kimchi.rules.RulesEngine;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class KimchiServiceTest {

	KimchiService service;
	final String TMP = "tmp/";
	final String OUTPUT_FILE = "src/test/resources/sample-output.json";

	@Before
	public void setup() {
		service = new KimchiService();
		new File(TMP).mkdirs();
		service.uploadedDir = TMP;
		service.mapper = new ObjectMapper();
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(new File(service.uploadedDir));
	}

	@Test
	public void givenUploadedRules_whenPersistFile_thenSaveFilesToUploadDir() {
		// Given rules
		String rulesName = "rules.drl";
		MultipartFile rules = new MockMultipartFile(rulesName, rulesName, null, "rulesName".getBytes());
		// When persist file
		service.persistFile(rules);
		// Then save to uploaded file directory
		File persistedRules = new File(TMP + service.RULES_FILE_NAME);
		assertTrue(persistedRules.exists());
		assertEquals(persistedRules.length(), 9);
	}

	@Test
	public void givenUploadedTests_whenPersistFile_thenSaveFilesToUploadDir() {
		// Given test
		String testName = "test.feature";
		MultipartFile test = new MockMultipartFile(testName, testName, null, "testName".getBytes());
		// When persist file
		service.persistFile(test);
		// Then save to uploaded file directory
		File persistedRules = new File(TMP + service.CUCUMBER_FILE_NAME);
		assertTrue(persistedRules.exists());
		assertEquals(persistedRules.length(), 8);
	}

	@Test
	public void givenInvalidUploadedFile_whenPersistFile_thenDoNotSaveFile() {
		// Given invalid file type
		String invalidName = "wrong.exe";
		MultipartFile invalid = new MockMultipartFile(invalidName, invalidName, null, "invalidName".getBytes());
		// When persist file
		service.persistFile(invalid);
		// Then do not save to the tmp directory
		File tmpDir = new File(TMP);
		File[] files = tmpDir.listFiles();
		assertEquals(files.length, 0);
	}

	@Test
	public void givenValidTestOutputAsFile_whenReadValue_thenCreateValueAsObject() {
		// Given a sample test output as json
		service.defaultTestOutputFile = OUTPUT_FILE;
		// When value is read
		List<Map<String, Object>> output = service.getTestOutput();
		// Then successfully convert into an object
		assertNotNull(output);
		assertTrue(output.size() > 0);
	}

	@Test
	public void givenInvalidJson_whenReadValue_thenHandleExceptionAndReturnNull() throws IOException {
		// Given a sample test output as json that doesn't map to expected value
		String json = "{}";
		String invalidFileName = "invalid.json";
		File invalid = new File(invalidFileName);
		FileUtils.writeStringToFile(invalid, json, StandardCharsets.UTF_8);
		service.defaultTestOutputFile = invalidFileName;
		// When value is read
		Object obj = service.getTestOutput();
		// Then handle exception and return null
		assertNull(obj);
		invalid.delete();
	}

	@Test
	public void givenInvalidFile_whenReadValue_thenReturnNull() {
		// Given an invalid file location (file doesn't exist)
		String invalidFileName = "invalid.fake";
		service.defaultTestOutputFile = invalidFileName;
		// When value is read
		Object obj = service.getTestOutput();
		// Then return as a null
		assertNull(obj);
	}

	@Test
	public void givenDefaultFiles_whenRunRulesTest_thenReturnRulesTestProfile() {
		// Given default files and the rules engine + cucumber engine
		service.defaultRulesFile = KimchiConstants.DEFUALT_RULES_FILE;
		service.defaultTestFile = KimchiConstants.DEFUALT_TEST_FILE;
		service.defaultTestOutputFile = KimchiConstants.DEFAULT_OUTPUT_FILE;
		service.rulesEngine = RulesEngine.getInstance();
		service.cucumberEngine = CucumberEngine.getInstance();
		// When run the rules test
		RulesTestProfile rulesTestProfile = service.runRulesTest();
		// Then the object is not null and is populated
		assertNotNull(rulesTestProfile);
		assertTrue(rulesTestProfile.getTestPassed());
		assertNotNull(rulesTestProfile.getTestOutput());
		assertNotNull(rulesTestProfile.getRulesCreationPerformance());
		assertNotNull(rulesTestProfile.getTestRunPerformance());
	}

	@Test
	public void givenUploadedFiles_whenRunRulesTest_thenReturnRulesTestProfile() throws IOException {
		// Given uploaded files and the rules engine + cucumber engine
		String rulesContent = FileUtils.readFileToString(new File(KimchiConstants.DEFUALT_RULES_FILE),
				StandardCharsets.UTF_8);
		String testContent = FileUtils.readFileToString(new File(KimchiConstants.DEFUALT_TEST_FILE),
				StandardCharsets.UTF_8);
		File uploadedRules = new File(service.uploadedDir + service.RULES_FILE_NAME);
		File uploadedTest = new File(service.uploadedDir + service.CUCUMBER_FILE_NAME);
		FileUtils.writeStringToFile(uploadedRules, rulesContent, StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(uploadedTest, testContent, StandardCharsets.UTF_8);
		service.defaultTestOutputFile = KimchiConstants.DEFAULT_OUTPUT_FILE;
		service.rulesEngine = RulesEngine.getInstance();
		service.cucumberEngine = CucumberEngine.getInstance();
		// When run the rules test
		RulesTestProfile rulesTestProfile = service.runRulesTest();
		// Then the object is not null and is populated
		assertNotNull(rulesTestProfile);
		assertTrue(rulesTestProfile.getTestPassed());
		assertNotNull(rulesTestProfile.getTestOutput());
		assertNotNull(rulesTestProfile.getRulesCreationPerformance());
		assertNotNull(rulesTestProfile.getTestRunPerformance());
	}

}