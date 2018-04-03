package com.ca.client.portal;

import java.security.cert.X509Certificate;
import java.util.Formatter;

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
	
	@Autowired
	private ConfigProperties props;

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
					final boolean isMigrateOutOption = commandLine.hasOption(CommandLineUtil.MIGRATE_OUT_OPTION);
					final boolean isMigrateInOption = commandLine.hasOption(CommandLineUtil.MIGRATE_IN_OPTION);
					final boolean isTypeOption = commandLine.hasOption(CommandLineUtil.TYPE_OPTION);
					final boolean isSourceOption = commandLine.hasOption(CommandLineUtil.SOURCE_OPTION);
					
					if(isEncPasswordOption) {
						migrationUtil.encodePassword();
					} else if(isMigrateOutOption) {
						validateApplicationProperties(CommandLineUtil.MIGRATE_OUT_OPTION, "");
						runMigration(commandLine, CommandLineUtil.MIGRATE_OUT_OPTION);
					} else if(isMigrateInOption) {
						validateApplicationProperties(CommandLineUtil.MIGRATE_IN_OPTION, "");
						runMigration(commandLine, CommandLineUtil.MIGRATE_IN_OPTION);
					} else if(isListOption) {
						String typeOptionValue = StringUtils.trimWhitespace(commandLine.getOptionValue(CommandLineUtil.TYPE_OPTION));
						String sourceOptionValue = StringUtils.trimWhitespace(commandLine.getOptionValue(CommandLineUtil.SOURCE_OPTION));
						validateApplicationProperties("", sourceOptionValue);
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

	/**
	 * @param commandLine
	 * @param option
	 */
	private void runMigration(CommandLine commandLine, String option) {
		String optionValue = StringUtils.trimWhitespace(commandLine.getOptionValue(option));
		if(!StringUtils.hasLength(optionValue)) {
			printHelp();
		}
		migrationUtil.runMigration(option, optionValue);
	}

	/**
	 * @param isTypeOption
	 * @param isSourceOption
	 * @param typeOptionValue
	 * @param sourceOptionValue
	 */
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
	
	private void validateApplicationProperties(final String option, final String source) {
		String msg = "Property %s cannot be blank.%n";
		StringBuilder errorMsg = new StringBuilder();
		Formatter fmt = new Formatter(errorMsg);
		
		if("from".equals(source) || CommandLineUtil.MIGRATE_OUT_OPTION.equals(option)) {
			if(StringUtils.isEmpty(props.getSrc().getClientId())) {
				fmt.format(msg, "'portal.src.clientId'");
			}
			if(StringUtils.isEmpty(props.getSrc().getClientSecret())) {
				fmt.format(msg, "'portal.src.clientSecret'");
			}
			if(StringUtils.isEmpty(props.getSrc().getTokenUrl())) {
				fmt.format(msg, "'portal.src.tokenUrl'");
			}
			if(StringUtils.isEmpty(props.getSrc().getUrl())) {
				fmt.format(msg, "'portal.src.url'");
			}
			if(CommandLineUtil.MIGRATE_OUT_OPTION.equals(option)) {
				if(StringUtils.isEmpty(props.getApiUuids())) {
					fmt.format(msg, "'portal.apiUuids'");
				}
				if(StringUtils.isEmpty(props.getDst().getProxyUrl())) {
					fmt.format(msg, "'portal.dst.proxyUrl'");
				}
			}
		} else if("to".equals(source) || CommandLineUtil.MIGRATE_IN_OPTION.equals(option)) {
			if(StringUtils.isEmpty(props.getDst().getClientId())) {
				fmt.format(msg, "'portal.dst.clientId'");
			}
			if(StringUtils.isEmpty(props.getDst().getClientSecret())) {
				fmt.format(msg, "'portal.dst.clientSecret'");
			}
			if(StringUtils.isEmpty(props.getDst().getTokenUrl())) {
				fmt.format(msg, "'portal.dst.tokenUrl'");
			}
			if(StringUtils.isEmpty(props.getDst().getUrl())) {
				fmt.format(msg, "'portal.dst.url'");
			}
			if(CommandLineUtil.MIGRATE_IN_OPTION.equals(option)) {
				if(StringUtils.isEmpty(props.getDst().getApiEulaUuid())) {
					fmt.format(msg, "'portal.dst.apiEulaUuid'");
				}
			}
		}
		if(StringUtils.hasLength(errorMsg)) {
			System.out.println(errorMsg);
			System.exit(-1);
		}
	}

	/**
	 * Print help for the command line usage
	 */
	private void printHelp() {
		commandLineUtil.printUsage();
		commandLineUtil.printHelp();
		System.exit(-1);
	}


}
