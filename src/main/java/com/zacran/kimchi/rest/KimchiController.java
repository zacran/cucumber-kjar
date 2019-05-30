package com.zacran.kimchi.rest;

import com.zacran.kimchi.model.RulesTestProfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/kimchi")
public class KimchiController {

	@Autowired
	KimchiService service;

	@PostMapping("/upload")
	public void uploadFile(@RequestParam("uploadedFile") MultipartFile uploadedFile) {
		service.persistFile(uploadedFile);
	}

	@PostMapping("/test")
	public RulesTestProfile runTests() {
		return service.runRulesTest();
	}

	@PostMapping("/install")
	public String installRules() {
		return service.installRules();
	}

}