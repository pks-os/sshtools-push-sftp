package com.sshtools.pushsftp;

import java.io.IOException;
import java.util.Optional;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.commands.ChildUpdateCommand;
import com.sshtools.commands.CliCommand;
import com.sshtools.commands.ExceptionHandler;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.jaul.AppCategory;
import com.sshtools.jaul.JaulApp;
import com.sshtools.pushsftp.commands.Bye;
import com.sshtools.pushsftp.commands.Cd;
import com.sshtools.pushsftp.commands.Chgrp;
import com.sshtools.pushsftp.commands.Chmod;
import com.sshtools.pushsftp.commands.Chown;
import com.sshtools.pushsftp.commands.Get;
import com.sshtools.pushsftp.commands.Help;
import com.sshtools.pushsftp.commands.Lcd;
import com.sshtools.pushsftp.commands.Lls;
import com.sshtools.pushsftp.commands.Lpwd;
import com.sshtools.pushsftp.commands.Ls;
import com.sshtools.pushsftp.commands.Mkdir;
import com.sshtools.pushsftp.commands.Push;
import com.sshtools.pushsftp.commands.Put;
import com.sshtools.pushsftp.commands.Pwd;
import com.sshtools.pushsftp.commands.Rm;
import com.sshtools.pushsftp.commands.Rmdir;
import com.sshtools.pushsftp.commands.Umask;
import com.sshtools.sequins.ArtifactVersion;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "push-sftp-interactive", description = "Push secure file transfer", subcommands = { Ls.class, Cd.class, Lcd.class, Pwd.class, Lls.class, 
		Lpwd.class, Help.class, Rm.class, Rmdir.class,
		Mkdir.class, Umask.class, Bye.class, Chgrp.class, 
		Chown.class, Chmod.class, Push.class, Put.class, Get.class,
		ChildUpdateCommand.class
		}, versionProvider = PSFTPInteractive.Version.class)
@JaulApp(id = "com.sshtools.PushSFTP", category = AppCategory.CLI, updaterId = "47", updatesUrl = "https://sshtools-public.s3.eu-west-1.amazonaws.com/push-sftp/${phase}/updates.xml")
public class PSFTPInteractive extends CliCommand {
	
	public final static class Version implements IVersionProvider {

		@Override
		public String[] getVersion() throws Exception {
			return new String[] { ArtifactVersion.getVersion("push-sftp", "com.sshtools", "push-sftp") };
		}
		
	}

	SftpClient sftp;

	@Option(names = { "-h", "--host" }, paramLabel = "HOSTNAME", description = "the hostname of the SSH server. Either this must be supplied or the username can be supplied in the destination")
    Optional<String> host;

	@Option(names = { "--help" }, usageHelp = true)
    boolean help;

	@Option(names = { "-v", "--version" }, versionHelp = true)
    boolean version;

	@Option(names = { "--prompt" }, description = "promt for the hostname and username if not supplied.")
    boolean promptHostAndUser;

	@Option(names = { "-p", "--port" }, paramLabel = "PORT", description = "the port of the SSH server")
    int port = 22;

	@Option(names = { "-u", "--user" }, paramLabel = "USER", description = "the username to authenticate with. Either this must be supplied or the username can be supplied in the destination")
    Optional<String> username;
	
	@Parameters(index = "0", arity = "0..1", description = "The remote server, with optional username.")
	private Optional<String> destination;
	
	private Optional<String> cachedHostname = Optional.empty();
	private Optional<String> cachedUsername = Optional.empty();
	private Optional<Integer> cachedPort = Optional.empty();

	public PSFTPInteractive() {
		super(Optional.empty());
	}

	public SftpClient getSftpClient() {
		return sftp;
	}

	@Override
	protected void onConnected(SshClient ssh) {
		try {
			sftp = new SftpClient(ssh);
			sftp.lcd(getLcwd().getAbsolutePath());
		} catch (SshException | PermissionDeniedException | IOException | SftpStatusException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public int getPort() {
		return cachedPort.orElse(super.getPort());
	}

	@Override
	public String getHost() {
		if(destination.isPresent()) {
			var dst = destination.get();
			var idx = dst.lastIndexOf('@');
			return idx == -1 ? dst : dst.substring(idx  +1);
		}
		return host.orElseGet(() -> {
			if(promptHostAndUser) {
				if(cachedHostname.isEmpty()) {
					var h = getTerminal().prompt("Hostname (Enter for {0}):", "localhost");
					if(h == null) {
						throw new IllegalArgumentException("Host must be supplied either as an option or as part of the destination.");					
					}
					else {
						if(h.equals(""))
							h = "localhost";
						var idx = h.indexOf(':');
						if(idx == -1) {
							cachedHostname = Optional.of(h);
							cachedPort = Optional.empty();
						}
						else {
							cachedHostname = Optional.of(h.substring(0, idx));
							cachedPort = Optional.of(Integer.parseInt(h.substring(idx  +1)));
						}
						return cachedHostname.get();
					}
				}
				else 
					return cachedHostname.get();
			}
			else
				throw new IllegalArgumentException("Host must be supplied either as an option or as part of the destination.");
		});
	}

	@Override
	public String getUsername() {
		if(destination.isPresent()) {
			var dst = destination.get();
			var idx = dst.lastIndexOf('@');
			if(idx > -1)
				return dst.substring(0, idx);
		}
		return username.orElseGet(() -> {
			if(promptHostAndUser) {
				if(cachedUsername.isEmpty()) {
					var u = getTerminal().prompt("Username (Enter for {0}):", System.getProperty("user.name"));
					if(u == null) {
						throw new IllegalArgumentException("Username must be supplied either as an option or as part of the destination.");					
					}
					else {
						if(u.equals(""))
							u = System.getProperty("user.name");
						cachedUsername = Optional.of(u);
						return cachedUsername.get();
					}
				}
				else 
					return cachedUsername.get();
			}
			else 
				throw new IllegalArgumentException("Username must be supplied either as an option or as part of the destination.");
		});
	}

	@Override
	protected void beforeCommand() { 
		if(getHost().equals(""))
			throw new IllegalArgumentException("Host name must be supplied.");	
	}

	@Override
	protected boolean canExit() {
		return false;
	}

	@Override
	protected void error(String msg, Exception e) {
		getTerminal().error(msg, e);
	}

	@Override
	protected boolean isQuiet() {
		return true;
	}

	@Override
	protected String getCommandName() {
		return "push-sftp";
	}
	
	public static void main(String[] args) throws Exception {
		var cmd = new PSFTPInteractive();
		System.exit(new CommandLine(cmd).setExecutionExceptionHandler(new ExceptionHandler(cmd)).execute(args));
	}

	public SshClient getSshClient() {
		return ssh;
	}

	@Override
	protected Object createInteractiveCommand() {
		return new PSFTPCommands(this);
	}
}
