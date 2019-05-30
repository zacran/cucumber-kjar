package com.zacran.kimchi.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RulesTestProfile {

	public Boolean testPassed;
	public List<Map<String, Object>> testOutput;
	public String testRunPerformance;
	public String rulesCreationPerformance;

}