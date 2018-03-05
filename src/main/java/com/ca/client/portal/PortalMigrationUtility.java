package com.ca.client.portal;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ca.client.config.ConfigProperties;

@Component
public class PortalMigrationUtility {
	Logger log = LoggerFactory.getLogger(PortalMigrationUtility.class);

	@Autowired
	private DefaultPortalAPIClient portalClient;
	
	@Autowired
	private ConfigProperties props;
	
	@Autowired
	private PortalUtil portalUtil;
	
	public void run() {
		try {
			Arrays.asList(props.getApiUuids().split(",")).forEach(apiUuid -> {
				log.info("=================================Starting Migration of API Uuid: {} ===================================", apiUuid);
				migrateApi(apiUuid);
				log.info("=================================Finished Migration of API Uuid: {} ===================================", apiUuid);
			});
		} catch(Exception e) {
			log.error("Process terminated, Exception: {}", e.getMessage());
		}
	}
	
	private void migrateApi(String apiUuid) {
		String srcBaseUrl = props.getSrc().getUrl();
		String destBaseUrl = props.getDst().getUrl();
		String proxyUuid = props.getDst().getProxyUuid();
		String apiEulaUuid = props.getDst().getApiEulaUuid();
		String baseTokenUrl = props.getTokenUrl();
		
		String srcToken = "";
		String dstToken = "";
		String srcApiUrl = srcBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String srcApiSpecUrl = srcBaseUrl + "/2.0/Apis('" + apiUuid + "')/SpecContent";
		String srcTokenUrl = baseTokenUrl + "&client_id=" + props.getSrc().getClientId() + "&client_secret=" + props.getSrc().getClientSecret();
		String dstApiUrl = destBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String dstPostApiUrl = destBaseUrl + "/2.0/Apis";
		String dstPostApiNewDeploymentUrl = destBaseUrl + "/deployments/1.0/apis/" + apiUuid + "/proxies";
		String dstPostApiUpdateDeploymentStatusUrl = destBaseUrl + "/deployments/1.0/apis/" + apiUuid + "/proxies/" + proxyUuid;
		String dstTokenUrl = baseTokenUrl + "&client_id=" + props.getDst().getClientId() + "&client_secret=" + props.getDst().getClientSecret();
		String dstProxyUrl = destBaseUrl + "/deployments/1.0/proxies/" + props.getDst().getProxyUuid();
		
		String postAPIResponse = "";
		
		log.info("Started retreiving OAuth token for Source Portal");
		srcToken = portalClient.getOAuthAccessToken(srcTokenUrl);
		log.info("Finished retrieving OAuth {} token for Source Portal", srcToken);

		log.info("Started retreiving OAuth token for Destination Portal");
		dstToken = portalClient.getOAuthAccessToken(dstTokenUrl);
		log.info("Finished retrieving OAuth {} token for Destination Portal", dstToken);
		
		
		log.info("Started retreiving API data for uuid: {}", apiUuid);
		String api = portalClient.getAPI(srcApiUrl, srcToken);
		log.info("Finished retrieving API data for uuid: {}", apiUuid);
		
		log.info("Started retrieving API Swagger Specs for uuid: {}", apiUuid);
		String apiSpec = portalClient.getAPISpec(srcApiSpecUrl, srcToken);
		log.info("Finished retrieving API Swagger Specs for uuid: {}", apiUuid);

		
		log.info("Started creating/updating API for uuid: {}", apiUuid);
		// Check if API exists on destination portal
		boolean destAPIExists = portalClient.checkAPIExists(dstApiUrl, dstToken);
		String postAPIPayload = portalUtil.prepareCreateOrUpdateAPIPayLoad(api, apiSpec, apiEulaUuid, destAPIExists);
		log.info("API with uuid {} exists on destination portal: {}", apiUuid, destAPIExists);
		
		if(destAPIExists) {
			postAPIResponse = portalClient.updateAPI(dstApiUrl, dstToken, postAPIPayload);
		} else {
			postAPIResponse = portalClient.postAPI(dstPostApiUrl, dstToken, postAPIPayload);
		}
		log.info("Finished creating/updating API for uuid: {}", apiUuid);
		
		/*
		if(!destAPIExists) {
			log.info("Started creating new deployment for uuid: {}", apiUuid);
			String postNewAPIDeploymentPayload = portalUtil.preparePOSTForNewAPIDeployment(proxyUuid);
			String postNewAPIDeploymentResponse = portalClient.createNewAPIDeployment(dstPostApiNewDeploymentUrl, dstToken, postNewAPIDeploymentPayload);
			log.info("Finished creating new deployment for uuid: {}", apiUuid);
		}
		
		log.info("Started updating deployment status to DEPLOYED for uuid: {}", apiUuid);
		String postApiUpdateDeploymentPayload = portalUtil.preparePOSTForUpdateDeploymentStatus("DEPLOYED");
		String postApiUpdateDeploymentResponse = portalClient.updateAPIDeployment(dstPostApiUpdateDeploymentStatusUrl, dstToken, postApiUpdateDeploymentPayload);
		log.info("Finished updating deployment status to DEPLOYED for uuid: {}", apiUuid);
		*/
		
		//log.info("POST: {}", PortalUtil.prettyPrintJsonString(postAPI));
		//log.info("API: {}", prettyPrintJsonString(rootNode));
		
		//log.info("API-Spec: {}", portalUtil.prettyPrintJsonString(apiSpec));

		// log.info("Response Body: {}", rootNode);
	}
}
