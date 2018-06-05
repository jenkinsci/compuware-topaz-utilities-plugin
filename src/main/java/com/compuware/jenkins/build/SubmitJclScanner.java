/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2018 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.compuware.jenkins.build;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.build.utils.TopazUtilitiesConstants;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CLIVersionUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

/**
 * Class used to initiate a JCL submit. This class will utilize the Topaz command line interface to do the submission.
 */
public class SubmitJclScanner
{
	// Member Variables
	private SubmitJclBuilder m_jclBuilder;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *            the <code>SubmitJclBuilder</code> to use for the scan
	 */
	public SubmitJclScanner(SubmitJclBuilder config)
	{
		m_jclBuilder = config;
	}

	/**
	 * Performs the JCL Submit.
	 * 
	 * @param run
	 *            the current running Jenkins build
	 * @param workspace
	 *            the Jenkins job workspace directory
	 * @param launcher
	 *            the way to start a process
	 * @param listener
	 *            the build listener
	 * 
	 * @throws IOException
	 *             if an error occurs performing the submit
	 * @throws InterruptedException
	 *             if the user cancels the submit
	 */
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException
	{
		// obtain argument values to pass to the CLI
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		VirtualChannel vChannel = launcher.getChannel();

		// Check CLI compatibility
		FilePath cliDirectory = new FilePath(vChannel, globalConfig.getTopazCLILocation(launcher));
		CLIVersionUtils.checkCLICompatibility(cliDirectory, TopazUtilitiesConstants.JCL_MINIMUM_CLI_VERSION);

		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? TopazUtilitiesConstants.SUBMIT_JCL_CLI_SH : TopazUtilitiesConstants.SUBMIT_JCL_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		HostConnection connection = globalConfig.getHostConnection(m_jclBuilder.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(),
				m_jclBuilder.getCredentialsId());
		String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
		String topazCliWorkspace = workspace.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE;
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		String maxConditionCodeStr = ArgumentUtils.escapeForScript(m_jclBuilder.getMaxConditionCode());

		// build the list of arguments to pass to the CLI
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(cliScriptFileRemote);
		args.add(CommonConstants.HOST_PARM, host);
		args.add(CommonConstants.PORT_PARM, port);
		args.add(CommonConstants.USERID_PARM, userId);
		args.add(CommonConstants.PW_PARM);
		args.add(password, true);
		args.add(CommonConstants.CODE_PAGE_PARM, codePage);
		args.add(CommonConstants.TIMEOUT_PARM, timeout);
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);
		args.add(TopazUtilitiesConstants.MAX_CC_PARM, maxConditionCodeStr);

		if (m_jclBuilder.isSubmitTypeJclMembers())
		{
			String jclMembers = ArgumentUtils
					.escapeForScript(StringUtils.replaceChars(m_jclBuilder.getJclMembers(), '\n', ','));
			args.add(TopazUtilitiesConstants.JCL_DSNS, jclMembers);
		}
		else if (m_jclBuilder.isSubmitTypeJcl())
		{
			String jcl = ArgumentUtils.escapeForScript(m_jclBuilder.getJcl());
			args.add(TopazUtilitiesConstants.JCL, jcl);
		}

		logger.println();

		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = run.getEnvironment(listener);
		FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		workDir.mkdirs();

		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(logger).pwd(workDir).join();
		if (exitValue != 0)
		{
			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}