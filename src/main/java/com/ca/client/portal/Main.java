package com.ca.client.portal;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.commons.cli.CommandLine;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.ca.client.config.ConfigProperties;
import com.ca.client.portal.util.CommandLineUtil;

@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties.class)
public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	@Autowired
	private PortalMigrationUtility migrationUtil;
	
	@Autowired
	private CommandLineUtil commandLineUtil;

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return new RestTemplate(clientHttpRequestFactory());
	}

	private ClientHttpRequestFactory clientHttpRequestFactory() {
		try {
			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy).build();
			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf)
					// .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.build();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			return requestFactory;

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error SSL Config");
			return null;
		}
	}

	@Bean
	public CommandLineRunner run(RestTemplate restTemplate) throws Exception {
		return args -> {
			if (args.length == 0 || StringUtils.isEmpty(args[0])) {
				printHelp();
			} else {
				CommandLine commandLine = commandLineUtil.generateCommandLine(args);
				// "interrogation" stage of processing with Apache Commons CLI
				if (commandLine != null) {
					final boolean isEncPasswordOption = commandLine.hasOption(CommandLineUtil.ENCODE_PASSWORD_OPTION);
					final boolean isListOption = commandLine.hasOption(CommandLineUtil.LIST_OPTION);
					final boolean isMigrateOption = commandLine.hasOption(CommandLineUtil.MIGRATE_OPTION);
					final boolean isTypeOption = commandLine.hasOption(CommandLineUtil.TYPE_OPTION);
					final boolean isSourceOption = commandLine.hasOption(CommandLineUtil.SOURCE_OPTION);
					
					if(isEncPasswordOption) {
						migrationUtil.encodePassword();
					} else if(isMigrateOption) {
						migrationUtil.runMigration();
					} else if(isListOption) {
						String typeOptionValue = StringUtils.trimWhitespace(commandLine.getOptionValue(CommandLineUtil.TYPE_OPTION));
						String sourceOptionValue = StringUtils.trimWhitespace(commandLine.getOptionValue(CommandLineUtil.SOURCE_OPTION));
						validateCommandOptions(isTypeOption, isSourceOption, typeOptionValue, sourceOptionValue);
						
						if("api".equalsIgnoreCase(typeOptionValue)) {
							migrationUtil.listAPIMetaData(sourceOptionValue);
						} else if("eula".equalsIgnoreCase(typeOptionValue)) {
							migrationUtil.listAPIEulasMetaData(sourceOptionValue);
						} else {
							migrationUtil.listProxyMetaData(sourceOptionValue);
						}
					}
				} else {
					printHelp();
				}
			}
		};
	}

	private void validateCommandOptions(final boolean isTypeOption, final boolean isSourceOption,
			String typeOptionValue, String sourceOptionValue) {
		if (!isTypeOption || !isSourceOption) {
			printHelp();
		}
		if(!("api".equalsIgnoreCase(typeOptionValue) || "proxy".equalsIgnoreCase(typeOptionValue) 
				|| "eula".equalsIgnoreCase(typeOptionValue))) {
			printHelp();
		}
		if(!("from".equalsIgnoreCase(sourceOptionValue) || "to".equalsIgnoreCase(sourceOptionValue))) {
			printHelp();
		}
	}

	private void printHelp() {
		commandLineUtil.printUsage();
		commandLineUtil.printHelp();
		System.exit(-1);
	}


}
