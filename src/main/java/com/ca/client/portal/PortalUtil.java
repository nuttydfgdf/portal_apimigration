package com.ca.client.portal;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ca.client.config.ConfigProperties;
import com.ca.client.portal.ex.PortalAPIRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class PortalUtil {
	private Logger log = LoggerFactory.getLogger(PortalUtil.class);
	
	@Autowired
	ConfigProperties props;
	
	public String prepareCreateOrUpdateAPIPayLoad(String apiPayload, String apiSpecPayload, String apiEulaUuid, boolean apiExists) {
		String postData = "";
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(apiPayload);
			ObjectNode objectNode = (ObjectNode) rootNode;
			//String apiUuid = rootNode.get("Uuid").asText();
			
			//Remove following json nodes
			objectNode.remove("__metadata");
			objectNode.remove("InSync");
			objectNode.remove("ServiceDisruption");
			objectNode.remove("ApplicationUsage");
			objectNode.remove("OrganizationUsage");
			objectNode.remove("Pending");
			objectNode.remove("PossibleStatuses");
			
			if(!apiExists) {
				objectNode.remove("PortalStatus");
				
			}
			//Update APIEulaUUid
			objectNode.put("ApiEulaUuid", apiEulaUuid);
			
			//Check if it is gateway published
			/*
			boolean isPublishedByPortal = rootNode.get("PublishedByPortal").asBoolean();
			log.info("Is Published by Portal: {}", isPublishedByPortal);
			
			if(!isPublishedByPortal) {
				objectNode.put("PublishedByPortal", false);
				String apiLocationUrl = rootNode.get("ApiLocationUrl").asText();
				if(StringUtils.isEmpty(apiLocationUrl) || StringUtils.hasText("null")) {
					objectNode.put("ApiLocationUrl", "http://localhost:8443/");
				}
				ArrayNode resultsNode = (ArrayNode)rootNode.get("PolicyEntities").get("results");
				
				if(resultsNode.isArray() && resultsNode.size() == 0) {
					ObjectNode resultObjectNode = resultsNode.addObject();
					resultObjectNode.put("PolicyEntityUuid", "72093738-871a-45bd-b114-ad3a61893ac0");
					ObjectNode policyTemplateArgObjectNode = mapper.createObjectNode();
					
					ArrayNode resultNode = mapper.createArrayNode();
					ObjectNode resultNodeObj = resultNode.addObject();
					resultNodeObj.put("Label", "SSL");
					resultNodeObj.put("Name", "sslEnabled");
					resultNodeObj.put("Type", "boolean");
					resultNodeObj.put("Uuid", "54044a0b-0360-498a-978d-aaef6098d721");
					resultNodeObj.put("Required", false);
					resultNodeObj.put("ApiUuid", apiUuid);
					resultNodeObj.put("Value", "true");
					resultNodeObj.put("PolicyTemplateUuid", "72093738-871a-45bd-b114-ad3a61893ac0");
					
					policyTemplateArgObjectNode.put("results", resultNode);
					resultObjectNode.put("PolicyTemplateArguments", policyTemplateArgObjectNode);
					//((ObjectNode)resultsNode).put(fieldName, v)
					//log.info("Results is an array node");
				}
			}
			*/
			//Add base64 encoded swagger specs
			if(!StringUtils.isEmpty(apiSpecPayload)) {
				ObjectMapper swaggerMapper = new ObjectMapper();
				JsonNode rootSwaggerNode = swaggerMapper.readTree(apiSpecPayload);
				((ObjectNode)rootSwaggerNode).put("host", props.getDst().getProxyUrl());
				String encodedAPISpec = Base64.getEncoder().encodeToString(rootSwaggerNode.toString().getBytes("UTF-8"));
				objectNode.put("SpecContent", "data:;base64,"+encodedAPISpec);
			}
			postData = rootNode.toString();
			log.debug("Post Data: {}", postData);
		} catch (UnsupportedOperationException|IOException e) {
			log.error("Error preparing POST data for new API: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return postData;
	}
	
	public String getAccessToken(String tokenPayload) {
		String token = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(tokenPayload);
			token = rootNode.get("access_token").asText();
		} catch (UnsupportedOperationException|IOException e) {
			log.error("Error extracting token: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return token;
	}
	
	public String preparePOSTForNewAPIDeployment(String proxyUuid) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> dataMap = new HashMap<String, String>();
			dataMap.put("proxyUuid", proxyUuid);
			return mapper.writeValueAsString(dataMap);
		} catch (JsonProcessingException e) {
			log.error("Error preparing POST for new API Deployment: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
	}
	
	public String preparePOSTForUpdateDeploymentStatus(String status) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> dataMap = new HashMap<String, String>();
			dataMap.put("status", status);
			return mapper.writeValueAsString(dataMap);
		} catch (JsonProcessingException e) {
			log.error("Error preparing POST for update API Deployment: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
	}

	public String prettyPrintJsonString(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Object jsonObj = mapper.readValue(json, Object.class);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
		} catch (Exception e) {
			return "Sorry, pretty print didn't work";
		}
	}
}
