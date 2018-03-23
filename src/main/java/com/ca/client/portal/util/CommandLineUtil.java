package com.ca.client.portal.util;

import static java.lang.System.out;

import java.io.PrintWriter;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.stereotype.Component;

@Component
public class CommandLineUtil {
	public final static String MIGRATE_OPTION = "migrate";
	public final static String ENCODE_PASSWORD_OPTION = "encodePassword";
	public final static String LIST_OPTION = "list";
	public final static String TYPE_OPTION = "type";
	public final static String SOURCE_OPTION = "source";
	private Options options;
	
	
	/**
	 * "Definition" stage of command-line parsing.
	 *  Initializes command-line options.
	 */
	@PostConstruct
	private void initializeOptions() {
		final Option migrateOption = Option.builder("m").required(false).hasArg(false).longOpt(MIGRATE_OPTION)
				.desc("Migrate APIs from source to destination portal as specified in application.properties.").build();
		final Option encPasswordOption = Option.builder("e").required(false).hasArg(false).longOpt(ENCODE_PASSWORD_OPTION)
				.desc("Encode password").build();
		final Option listOption = Option.builder("l").required(false).hasArg(false).longOpt(LIST_OPTION)
				.desc("List apis or proxies on source or destination portal.").build();
		final Option typeOption = Option.builder("t").required(false).longOpt(TYPE_OPTION).hasArg()
				.desc("api|proxy|eula, used together with list option i.e. --list --type api or --list --type proxy.").build();
		final Option sourceOption = Option.builder("s").required(false).longOpt(SOURCE_OPTION).hasArg()
				.desc("from|to, used together with list and type option i.e. --list --type api --source from").build();
		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addOption(migrateOption);
		optionGroup.addOption(encPasswordOption);
		optionGroup.addOption(listOption);
		this.options = new Options();
		options.addOptionGroup(optionGroup);
		options.addOption(typeOption);
		options.addOption(sourceOption);
	}
	
	/**
	 * "Parsing" stage of command-line processing
	 *
	 * @param commandLineArguments
	 *            Command-line arguments provided to application.
	 * @return Instance of CommandLine as parsed from the provided Options and
	 *         command line arguments; may be {@code null} if there is an exception
	 *         encountered while attempting to parse the command line options.
	 */
	public CommandLine generateCommandLine(final String[] commandLineArguments) {
		final CommandLineParser cmdLineParser = new DefaultParser();
		CommandLine commandLine = null;
		try {
			commandLine = cmdLineParser.parse(options, commandLineArguments);
		} catch (ParseException parseException) {
			out.println("ERROR: Unable to parse command-line arguments " + Arrays.toString(commandLineArguments)
					+ " due to: " + parseException);
		}
		return commandLine;
	}
	
	/**
	 * Generate usage information with Apache Commons CLI.
	 *
	 * @param options
	 *            Instance of Options to be used to prepare usage formatter.
	 * @return HelpFormatter instance that can be used to print usage information.
	 */
	public void printUsage() {
		final HelpFormatter formatter = new HelpFormatter();
		final String syntax = "PortalMigrationUtil";
		//out.println("\n=====");
		//out.println("USAGE");
		//out.println("=====");
		final PrintWriter pw = new PrintWriter(out);
		formatter.printUsage(pw, 80, syntax, this.options);
		pw.flush();
	}

	/**
	 * Generate help information with Apache Commons CLI.
	 *
	 * @param options
	 *            Instance of Options to be used to prepare help formatter.
	 * @return HelpFormatter instance that can be used to print help information.
	 */
	public void printHelp() {
		final HelpFormatter formatter = new HelpFormatter();
		final String syntax = "PortalMigrationUtil";
		final String usageHeader = "";
		final String usageFooter = "";
		//out.println("\n====");
		//out.println("HELP");
		//out.println("====");
		formatter.printHelp(syntax, usageHeader, this.options, usageFooter);
	}
	
}
