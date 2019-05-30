package com.zacran.kimchi.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RulesTestProfile {

	Boolean testPassed;
	List<Map<String, Object>> testOutput;
	String testRunPerformance;
	String rulesCreationPerformance;

}