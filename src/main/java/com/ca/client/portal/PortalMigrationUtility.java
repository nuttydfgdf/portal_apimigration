package com.ca.client.portal;

import java.io.Console;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ca.client.config.ConfigProperties;
import com.ca.client.model.PortalAPIVO;
import com.ca.client.portal.ex.PortalAPIRuntimeException;
import com.ca.client.portal.util.CommandLineUtil;
import com.ca.client.portal.util.CryptoUtil;
import com.ca.client.portal.util.PortalUtil;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class PortalMigrationUtility {
	Logger log = LoggerFactory.getLogger(PortalMigrationUtility.class);

	@Autowired
	private DefaultPortalAPIClient portalClient;
	
	@Autowired
	private ConfigProperties props;
	
	@Autowired
	private PortalUtil portalUtil;
	
	private String srcToken;
	private String dstToken;
	private boolean isInitialized = false;
	
	private void initializeTokens() {
		String srcBaseTokenUrl = props.getSrc().getTokenUrl();
		String dstBaseTokenUrl = props.getDst().getTokenUrl();
		String srcTokenUrl;
		String dstTokenUrl;

		if(StringUtils.hasLength(srcBaseTokenUrl) && StringUtils.hasLength(props.getSrc().getClientId()) 
				&& StringUtils.hasLength(props.getSrc().getClientSecret())) {
			log.debug("Started retreiving OAuth token for Source Portal");
			srcTokenUrl = srcBaseTokenUrl + "&client_id=" + props.getSrc().getClientId() + "&client_secret=" + CryptoUtil.decrypt(props.getSrc().getClientSecret());
			this.srcToken = portalClient.getOAuthAccessToken(srcTokenUrl);
			log.debug("Finished retrieving OAuth {} token for Source Portal", srcToken);
		}
		
		if(StringUtils.hasLength(dstBaseTokenUrl) && StringUtils.hasLength(props.getDst().getClientId()) 
				&& StringUtils.hasLength(props.getDst().getClientSecret())) {
			log.debug("Started retreiving OAuth token for Destination Portal");
			dstTokenUrl = dstBaseTokenUrl + "&client_id=" + props.getDst().getClientId() + "&client_secret=" + CryptoUtil.decrypt(props.getDst().getClientSecret());
			this.dstToken = portalClient.getOAuthAccessToken(dstTokenUrl);
			log.debug("Finished retrieving OAuth {} token for Destination Portal", dstToken);
		}

		this.isInitialized = true;
	}
	
	public void encodePassword() {
		Console console = System.console();
		System.out.println("Enter the password to encode:");
		char[] password = console.readPassword();
		while(password.length == 0) {
			System.out.println("Invalid password, enter the password to encode:");
			password = console.readPassword();
		}
		String encryptedPassword = CryptoUtil.encrypt(String.valueOf(password));
		System.out.println(encryptedPassword);
		//System.out.println("Decrypted: " + CryptoUtil.decrypt(encryptedPassword, key));
	}

	public void listAPIMetaData(String source) {
		String apisUrlSuffix = "/2.0/Apis";
		PortalAPIVO portalAPIVO = getPortalAPIVO(source, apisUrlSuffix);
		String apiMetaData = portalClient.getAPIMetaData(portalAPIVO.getUrl(), portalAPIVO.getToken());
		System.out.println(apiMetaData);
	}
	
	public void listAPIEulasMetaData(String source) {
		String apiEulasUrlSuffix = "/ApiEulas";
		PortalAPIVO portalAPIVO = getPortalAPIVO(source, apiEulasUrlSuffix);
		String apiEulaMetaData = portalClient.getAPIEulasMetaData(portalAPIVO.getUrl(), portalAPIVO.getToken());
		System.out.println(apiEulaMetaData);
	}
	
	public void listProxyMetaData(String source) {
		String proxyUrlSuffix = "/deployments/1.0/proxies";
		PortalAPIVO portalAPIVO = getPortalAPIVO(source, proxyUrlSuffix);
		String proxyMetaData = portalClient.getProxyMetaData(portalAPIVO.getUrl(), portalAPIVO.getToken());
		System.out.println(proxyMetaData);
	}
	
	public PortalAPIVO getPortalAPIVO(String source, String apisUrlSuffix) {
		String apisUrl = "";
		String token = "";
		if(!isInitialized) {
			initializeTokens();
		}
		if("from".equalsIgnoreCase(source)) {
			apisUrl = props.getSrc().getUrl() + apisUrlSuffix;
			token = this.srcToken;
		} else if("to".equalsIgnoreCase(source)) {
			apisUrl = props.getDst().getUrl() + apisUrlSuffix;
			token = this.dstToken;
		}
		PortalAPIVO portalAPIVO = new PortalAPIVO();
		portalAPIVO.setUrl(apisUrl);
		portalAPIVO.setToken(token);
		return portalAPIVO;
	}

	/**
	 * @param option
	 * @param fileName
	 */
	public void runMigration(String option, String fileName) {
		try {
			if(!isInitialized) {
				initializeTokens();
			}
			
			if(CommandLineUtil.MIGRATE_OUT_OPTION.equals(option)) {
				List<JsonNode> apiPayloadList = new ArrayList<JsonNode>();
				Arrays.asList(props.getApiUuids().split(",")).forEach(apiUuid -> {
					log.info("==================Exporting API, Uuid: {} ====================", apiUuid);
					apiPayloadList.add(getApiPayloadForExport(StringUtils.trimWhitespace(apiUuid)));

				});
				String apiPayloadForExport = portalUtil.prepareAPIPayloadWrapperForExport(apiPayloadList);
				File exportFile = new File(fileName);
				FileUtils.writeStringToFile(exportFile, apiPayloadForExport, StandardCharsets.UTF_8.name());
				log.info("==================Finished Exporting API(s)====================");
				
			} else if(CommandLineUtil.MIGRATE_IN_OPTION.equals(option)) {
				log.info("=====================Importing API(s) =============================");
				File importFile = new File(fileName);
				String apiPayloadForImport = FileUtils.readFileToString(importFile, StandardCharsets.UTF_8.name());
				Map<String, JsonNode> apiPayloadMap = portalUtil.prepareAPIPayloadMapForImport(apiPayloadForImport);
				publishAPI(apiPayloadMap);
				log.info("=====================Finished importing API(s) =============================");
			}
		} catch(Exception e) {
			log.error("Process terminated, Exception: {}", e.getMessage());
			//e.printStackTrace();
		}
	}

	/**
	 * @param apiPayloadMap
	 */
	private void publishAPI(Map<String, JsonNode> apiPayloadMap) {
		String dstBaseUrl = props.getDst().getUrl();
		String dstPostApiUrl = dstBaseUrl + "/2.0/Apis";
		String apiEulaUuid = props.getDst().getApiEulaUuid();
		
		apiPayloadMap.forEach((apiUuid, node) -> {
			//log.debug("{} , {}", apiUuid, node.toString());
			log.info("Publishing API for uuid: {}", apiUuid);
			String postAPIResponse;
			String dstApiUrl = dstBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
			
			// Check if API exists on destination portal
			boolean isDstAPIExists = portalClient.checkAPIExists(dstApiUrl, this.dstToken);
			log.info("API with uuid {} exists on destination portal: {}", apiUuid, isDstAPIExists);	
			log.debug("Replacing API Eula Uuid with {}", apiEulaUuid);
			JsonNode apiPayloadForImport = portalUtil.prepareAPIPayloadForImport(node, apiEulaUuid, isDstAPIExists);
			log.debug("Api Json for create/update: {}", apiPayloadForImport.toString());
			if(isDstAPIExists) {
				postAPIResponse = portalClient.updateAPI(dstApiUrl, this.dstToken, apiPayloadForImport.toString());
			} else {
				postAPIResponse = portalClient.postAPI(dstPostApiUrl, this.dstToken, apiPayloadForImport.toString());
			}
			log.debug("POST API Response: {}", postAPIResponse);
		});
	}
	
	/**
	 * @param apiUuid
	 * @return API payload for the specified API Uuid
	 */
	private JsonNode getApiPayloadForExport(String apiUuid) {
		String srcBaseUrl = props.getSrc().getUrl();
		String srcApiUrl = srcBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String srcApiSpecUrl = srcBaseUrl + "/2.0/Apis('" + apiUuid + "')/SpecContent";

		log.info("Retreiving API data for uuid: {}", apiUuid);
		String api = portalClient.getAPI(srcApiUrl, this.srcToken);
		log.info("Finished retrieving API data for uuid: {}", apiUuid);
		
		// Check if PortalPublished API
		boolean isPublishedByPortal = portalUtil.isPortalPublishedAPI(api);
		if(!isPublishedByPortal) {
			throw new PortalAPIRuntimeException("Gateway Published APIs are not supported. Use GMU to migrate service and enable from Portal.");
		}
		log.info("Retrieving API Swagger Specs for uuid: {}", apiUuid);
		String apiSpec = portalClient.getAPISpec(srcApiSpecUrl, this.srcToken);
		log.info("Finished retrieving API Swagger Specs for uuid: {}", apiUuid);

		log.info("Exporting API for uuid: {}", apiUuid);
		
		JsonNode postAPIPayload = portalUtil.prepareAPIPayLoadForExport(api, apiSpec);
		return postAPIPayload;
	}
	
	/*
	private void migrateApi(String apiUuid) {
		String srcBaseUrl = props.getSrc().getUrl();
		String dstBaseUrl = props.getDst().getUrl();
		String proxyUuid = props.getDst().getProxyUuid();
		String apiEulaUuid = props.getDst().getApiEulaUuid();
		
		String srcApiUrl = srcBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String srcApiSpecUrl = srcBaseUrl + "/2.0/Apis('" + apiUuid + "')/SpecContent";
		String dstApiUrl = dstBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String dstPostApiUrl = dstBaseUrl + "/2.0/Apis";
		//String dstPostApiNewDeploymentUrl = destBaseUrl + "/deployments/1.0/apis/" + apiUuid + "/proxies";
		//String dstPostApiUpdateDeploymentStatusUrl = destBaseUrl + "/deployments/1.0/apis/" + apiUuid + "/proxies/" + proxyUuid;
		//String dstProxyUrl = destBaseUrl + "/deployments/1.0/proxies/" + props.getDst().getProxyUuid();
		String postAPIResponse = "";
		
		log.info("Started retreiving API data for uuid: {}", apiUuid);
		String api = portalClient.getAPI(srcApiUrl, this.srcToken);
		log.info("Finished retrieving API data for uuid: {}", apiUuid);
		// Check if PortalPublished API
		boolean isPublishedByPortal = portalUtil.isPortalPublishedAPI(api);
		if(!isPublishedByPortal) {
			throw new PortalAPIRuntimeException("Gateway Published APIs are not supported. Use GMU to migrate service and enable from Portal.");
		}
		log.info("Started retrieving API Swagger Specs for uuid: {}", apiUuid);
		String apiSpec = portalClient.getAPISpec(srcApiSpecUrl, this.srcToken);
		log.info("Finished retrieving API Swagger Specs for uuid: {}", apiUuid);

		
		log.info("Started creating/updating API for uuid: {}", apiUuid);
		// Check if API exists on destination portal
		boolean destAPIExists = portalClient.checkAPIExists(dstApiUrl, this.dstToken);
		String postAPIPayload = portalUtil.prepareCreateOrUpdateAPIPayLoad(api, apiSpec, apiEulaUuid, destAPIExists).toString();
		log.info("API with uuid {} exists on destination portal: {}", apiUuid, destAPIExists);
		
		if(destAPIExists) {
			postAPIResponse = portalClient.updateAPI(dstApiUrl, this.dstToken, postAPIPayload);
		} else {
			postAPIResponse = portalClient.postAPI(dstPostApiUrl, this.dstToken, postAPIPayload);
		}
		log.info("Finished creating/updating API for uuid: {}", apiUuid);
		
		
		//		if(!destAPIExists) {
		//			log.info("Started creating new deployment for uuid: {}", apiUuid);
		//			String postNewAPIDeploymentPayload = portalUtil.preparePOSTForNewAPIDeployment(proxyUuid);
		//			String postNewAPIDeploymentResponse = portalClient.createNewAPIDeployment(dstPostApiNewDeploymentUrl, dstToken, postNewAPIDeploymentPayload);
		//			log.info("Finished creating new deployment for uuid: {}", apiUuid);
		//		}
		//		
		//		log.info("Started updating deployment status to DEPLOYED for uuid: {}", apiUuid);
		//		String postApiUpdateDeploymentPayload = portalUtil.preparePOSTForUpdateDeploymentStatus("DEPLOYED");
		//		String postApiUpdateDeploymentResponse = portalClient.updateAPIDeployment(dstPostApiUpdateDeploymentStatusUrl, dstToken, postApiUpdateDeploymentPayload);
		//		log.info("Finished updating deployment status to DEPLOYED for uuid: {}", apiUuid);
		//		
	}*/
	
}
