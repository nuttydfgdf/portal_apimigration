package com.ca.client.portal.util;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class PortalUtil {
	private Logger log = LoggerFactory.getLogger(PortalUtil.class);
	
	@Autowired
	ConfigProperties props;
	
	
	public boolean isPortalPublishedAPI(String apiPayload) {
		boolean isPortalPublished = false;
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode rootNode = mapper.readTree(apiPayload);
			isPortalPublished = rootNode.get("PublishedByPortal").asBoolean();
			log.debug("Is Published by Portal: {}", isPortalPublished);

		} catch (IOException e) {
			log.error("Error preparing POST data for new API: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return isPortalPublished;
	}
	
	/**
	 * @param apiPayloadList
	 * @return
	 */
	public String prepareAPIPayloadWrapperForExport(List<JsonNode> apiPayloadList) {
		ObjectMapper exportObjectMapper = new ObjectMapper();
		ObjectNode rootNode = exportObjectMapper.createObjectNode();
		ArrayNode arrayNode = exportObjectMapper.createArrayNode();
		apiPayloadList.forEach(apiPayload -> {
			log.debug("apiPayload: {}", apiPayload);
			arrayNode.add(apiPayload);
		});
		rootNode.set("apiPayload", arrayNode);
		return rootNode.toString();
	}
	
	/**
	 * @param apiPayload
	 * @param apiSpecPayload
	 * @return
	 */
	public JsonNode prepareAPIPayLoadForExport(String apiPayload, String apiSpecPayload) {
		JsonNode rootNode;
		try {
			ObjectMapper mapper = new ObjectMapper();
			rootNode = mapper.readTree(apiPayload);
			ObjectNode objectNode = (ObjectNode) rootNode;
			
			//Remove following json nodes
			objectNode.remove("__metadata");
			objectNode.remove("InSync");
			objectNode.remove("ServiceDisruption");
			objectNode.remove("ApplicationUsage");
			objectNode.remove("ApiGroupUsage");
			objectNode.remove("OrgUuid");
			objectNode.remove("OrganizationUsage");
			objectNode.remove("Pending");
			objectNode.remove("PossibleStatuses");
			
			// Check if backendAPIURL is configured for this Uuid and replace it with configured value
			String apiUuid = objectNode.get("Uuid").asText();
			String backendAPIUrl = props.getDst().getBackendAPIUrlMap().get(apiUuid);
			if(!StringUtils.isEmpty(backendAPIUrl)) {
				log.debug("Replacing {} with {}", objectNode.get("ApiLocationUrl"), backendAPIUrl);
				objectNode.put("ApiLocationUrl", backendAPIUrl);
			} else {
				log.warn("Skipping replacing of backend API URL as it's not configured");
			}
			
			//Add base64 encoded swagger specs
			if(!StringUtils.isEmpty(apiSpecPayload)) {
				ObjectMapper swaggerMapper = new ObjectMapper();
				JsonNode rootSwaggerNode = swaggerMapper.readTree(apiSpecPayload);
				((ObjectNode)rootSwaggerNode).put("host", props.getDst().getProxyUrl());
				String encodedAPISpec = Base64.getEncoder().encodeToString(rootSwaggerNode.toString().getBytes("UTF-8"));
				objectNode.put("SpecContent", "data:;base64,"+encodedAPISpec);
			}
		} catch (UnsupportedOperationException|IOException e) {
			log.error("Error preparing API Payload for API: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return rootNode;
	}
	/**
	 * @param apiPayload
	 * @return
	 */
	public Map<String, JsonNode> prepareAPIPayloadMapForImport(String apiPayload) {
		Map<String, JsonNode> apiPayloadMap = new HashMap<String, JsonNode>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode rootNode = mapper.readTree(apiPayload);
			ArrayNode apiPayloadArrayNode = (ArrayNode)rootNode.get("apiPayload");
			
			if(apiPayloadArrayNode.isArray() && apiPayloadArrayNode.size() > 0) {
				Iterator<JsonNode> itr = apiPayloadArrayNode.iterator();
				itr.forEachRemaining(node -> {
					String apiUuid = node.get("Uuid").asText().replaceAll("\"", "");
					apiPayloadMap.put(apiUuid, node);
				});
			}
			
		} catch (IOException e) {
			log.error("Error preparing API Payload map for import: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return apiPayloadMap;
	}
	
	/**
	 * @param node
	 * @param apiEulaUuid
	 * @param apiExists
	 * @return
	 */
	public JsonNode prepareAPIPayloadForImport(JsonNode node, String apiEulaUuid, boolean apiExists) {
		ObjectNode objectNode = (ObjectNode) node;
		if(!apiExists) {
			objectNode.remove("PortalStatus");
		} 
		//Update APIEulaUUid
		objectNode.put("ApiEulaUuid", apiEulaUuid);
		return node;
	}
	
	/*
	public JsonNode prepareCreateOrUpdateAPIPayLoad(String apiPayload, String apiSpecPayload, String apiEulaUuid, boolean apiExists) {
		//String postData = "";
		JsonNode rootNode;
		try {
			ObjectMapper mapper = new ObjectMapper();
			rootNode = mapper.readTree(apiPayload);
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
//			boolean isPublishedByPortal = rootNode.get("PublishedByPortal").asBoolean();
//			log.debug("Is Published by Portal: {}", isPublishedByPortal);
//			if(!isPublishedByPortal) {
//				throw new PortalAPIRuntimeException("Gateway Published APIs are not supported. Use GMU to migrate service and enable from Portal.");
//			}
//			if(!isPublishedByPortal) {
//				objectNode.put("PublishedByPortal", false);
//				String apiLocationUrl = rootNode.get("ApiLocationUrl").asText();
//				if(StringUtils.isEmpty(apiLocationUrl) || StringUtils.hasText("null")) {
//					objectNode.put("ApiLocationUrl", "http://localhost:8443/");
//				}
//				ArrayNode resultsNode = (ArrayNode)rootNode.get("PolicyEntities").get("results");
//				
//				if(resultsNode.isArray() && resultsNode.size() == 0) {
//					ObjectNode resultObjectNode = resultsNode.addObject();
//					resultObjectNode.put("PolicyEntityUuid", "72093738-871a-45bd-b114-ad3a61893ac0");
//					ObjectNode policyTemplateArgObjectNode = mapper.createObjectNode();
//					
//					ArrayNode resultNode = mapper.createArrayNode();
//					ObjectNode resultNodeObj = resultNode.addObject();
//					resultNodeObj.put("Label", "SSL");
//					resultNodeObj.put("Name", "sslEnabled");
//					resultNodeObj.put("Type", "boolean");
//					resultNodeObj.put("Uuid", "54044a0b-0360-498a-978d-aaef6098d721");
//					resultNodeObj.put("Required", false);
//					resultNodeObj.put("ApiUuid", apiUuid);
//					resultNodeObj.put("Value", "true");
//					resultNodeObj.put("PolicyTemplateUuid", "72093738-871a-45bd-b114-ad3a61893ac0");
//					
//					policyTemplateArgObjectNode.put("results", resultNode);
//					resultObjectNode.put("PolicyTemplateArguments", policyTemplateArgObjectNode);
//					//((ObjectNode)resultsNode).put(fieldName, v)
//					//log.info("Results is an array node");
//				}
//			}

			//Add base64 encoded swagger specs
			
			if(!StringUtils.isEmpty(apiSpecPayload)) {
				ObjectMapper swaggerMapper = new ObjectMapper();
				JsonNode rootSwaggerNode = swaggerMapper.readTree(apiSpecPayload);
				((ObjectNode)rootSwaggerNode).put("host", props.getDst().getProxyUrl());
				String encodedAPISpec = Base64.getEncoder().encodeToString(rootSwaggerNode.toString().getBytes("UTF-8"));
				objectNode.put("SpecContent", "data:;base64,"+encodedAPISpec);
			}
			//postData = rootNode.toString();
			log.debug("Post Data: {}", rootNode.toString());
		} catch (UnsupportedOperationException|IOException e) {
			log.error("Error preparing POST data for new API: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return rootNode;
	}
	
	*/
	public String getAPIMetaData(String apisPayload) {
		String newLine = System.getProperty("line.separator");
		StringBuffer apiMetaData = new StringBuffer(newLine);
		ObjectMapper mapper = new ObjectMapper();
		String name="";
		String uuid="";
		boolean isPublishedByPortal = false;
		
		apiMetaData.append(String.format("%-40s", "API")).append(String.format("%-45s", "UUID")).append("PORTAL PUBLISHED").append(newLine);
		apiMetaData.append(String.format("%-40s", "----")).append(String.format("%-45s", "-----")).append("----------------").append(newLine);
		
		try {
			ArrayNode rootArrayNode = (ArrayNode)mapper.readTree(apisPayload);
			if(rootArrayNode.isArray() && rootArrayNode.has(0)) {
				for(JsonNode item : rootArrayNode) {
					name = item.get("Name").asText().replaceAll("\"", "");
					uuid = item.get("Uuid").asText().replaceAll("\"", "");
					isPublishedByPortal = item.get("PublishedByPortal").asBoolean();
					apiMetaData.append(String.format("%-40s", name))
						.append(String.format("%-45s", uuid))
						.append(String.valueOf(isPublishedByPortal))
						.append(newLine);
				}
			}
		} catch (IOException e) {
			log.error("Error retrieving API metadata: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return apiMetaData.toString();
	}
	
	public String getAPIEulaMetaData(String apiEulaPayload) {
		String newLine = System.getProperty("line.separator");
		StringBuffer apiEulaMetaData = new StringBuffer(newLine);
		ObjectMapper mapper = new ObjectMapper();
		String name="";
		String uuid="";
		
		apiEulaMetaData.append(String.format("%-30s", "EULA Name")).append(String.format("%-45s", "UUID")).append(newLine);
		apiEulaMetaData.append(String.format("%-30s", "---------")).append(String.format("%-45s", "-----")).append(newLine);
		try {
			ArrayNode rootArrayNode = (ArrayNode)mapper.readTree(apiEulaPayload);
			if(rootArrayNode.isArray() && rootArrayNode.has(0)) {
				for(JsonNode item : rootArrayNode) {
					name = item.get("Name").asText().replaceAll("\"", "");
					uuid = item.get("Uuid").asText().replaceAll("\"", "");
					apiEulaMetaData.append(String.format("%-30s", name))
					.append(String.format("%-45s", uuid))
					.append(newLine); 
				}
			}
		} catch (IOException e) {
			log.error("Error retrieving APIEula metadata: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return apiEulaMetaData.toString();
	}
	
	public String getProxyMetaData(String proxiesPayload) {
		String newLine = System.getProperty("line.separator");
		StringBuffer proxyMetaData = new StringBuffer(newLine);
		ObjectMapper mapper = new ObjectMapper();
		String name="";
		String uuid="";
		String deploymentType="";
		
		proxyMetaData.append(String.format("%-30s", "Proxy")).append(String.format("%-45s", "UUID")).append("DEPLOYMENT TYPE").append(newLine);
		proxyMetaData.append(String.format("%-30s", "----")).append(String.format("%-45s", "-----")).append("----------------").append(newLine);
		try {
			ArrayNode rootArrayNode = (ArrayNode)mapper.readTree(proxiesPayload);
			if(rootArrayNode.isArray() && rootArrayNode.has(0)) {
				for(JsonNode item : rootArrayNode) {
					name = item.get("name").asText().replaceAll("\"", "");
					uuid = item.get("uuid").asText().replaceAll("\"", "");
					deploymentType = item.get("deploymentType").asText().replaceAll("\"", "");
					proxyMetaData.append(String.format("%-30s", name))
					.append(String.format("%-45s", uuid))
					.append(deploymentType)
					.append(newLine); 
				}
			}
		} catch (IOException e) {
			log.error("Error retrieving Proxy metadata: {}", e.getMessage());
			throw new PortalAPIRuntimeException(e.getMessage());
		}
		return proxyMetaData.toString();
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
