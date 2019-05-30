package com.zacran.kimchi.config;

import com.zacran.kimchi.cucumber.CucumberEngine;
import com.zacran.kimchi.rules.RulesEngine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

	@Bean
	public RulesEngine rulesEngine() {
		return RulesEngine.getInstance();
	}

	@Bean
	public CucumberEngine cucumberEngine() {
		return CucumberEngine.getInstance();
	}

}