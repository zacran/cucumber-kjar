package com.zacran.kimchi.rules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.internal.io.ResourceFactory;

public class RulesResourceAdaptor {
	public static Resource[] convertFilesToResources(File... files) {
		List<Resource> rulesResources = new ArrayList<>();
		for (File file : files) {
			Resource rulesResource = convertFileToResource(file);
			rulesResources.add(rulesResource);
		}

		return rulesResources.toArray(new Resource[rulesResources.size()]);
	}

	public static Resource convertFileToResource(File file) {
		Resource rulesResource = ResourceFactory.newFileResource(file);

		return processResource(rulesResource, file.getName());
	}

	static Resource processResource(Resource rulesResource, String resourceName) {
		ResourceType resourceType = ResourceType.determineResourceType(resourceName);
		if (resourceType == null) {
			return null;
		}
		rulesResource.setResourceType(resourceType);
		String targetPath = resourceType.getDefaultPath() + "/" + resourceName;
		rulesResource.setTargetPath(targetPath);

		return rulesResource;
	}
}