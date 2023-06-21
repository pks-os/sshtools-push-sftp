package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rmdir", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Remove directory")
public class Rmdir extends SftpCommand {

	@Parameters(index = "0", arity = "1", description = "Directory to remove")
	private String file;
	
	@Override
	protected Integer onCall() throws Exception {
		var expandedPath = expandRemoteSingle(file);
		getTerminal().messageln("Removing dir {0}", expandedPath);
		getSftpClient().rmdir(expandedPath);
		return 0;
	}

}
