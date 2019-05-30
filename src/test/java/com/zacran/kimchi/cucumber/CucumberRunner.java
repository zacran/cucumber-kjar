package com.zacran.kimchi.cucumber;

import java.io.File;

import com.zacran.kimchi.config.KimchiConstants;
import com.zacran.kimchi.rules.RulesEngine;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { KimchiConstants.DEFUALT_TEST_FILE }, glue = { "com.zacran.kimchi.cucumber" }, plugin = {
		"pretty" })
public class CucumberRunner {

	static RulesEngine rulesEngine;

	static final String TEST_VERSION = "kimchi-test";
	static final String DEFAULT_RULES_DIRECTORY = KimchiConstants.DEFUALT_RULES_FILE;

	@BeforeClass
	public static void setupRules() {
		rulesEngine = RulesEngine.getInstance();

		File[] files = getDefaultRules();
		rulesEngine.compileAndLoadRules(files);
	}

	static File[] getDefaultRules() {
		File directory = new File(DEFAULT_RULES_DIRECTORY);
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files == null) {
				return new File[] {};
			}
			return files;
		}
		return new File[] { directory };
	}

}