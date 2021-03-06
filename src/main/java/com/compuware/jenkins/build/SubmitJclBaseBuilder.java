/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2018, 2019 Compuware Corporation
 * (c) Copyright 2015 - 2019, 2021 BMC Software, Inc.
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
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import com.compuware.jenkins.build.utils.TopazUtilitiesConstants;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
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
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;

/**
 * Captures the common configuration information for a Submit JCL or JCL Member build step.
 */
public abstract class SubmitJclBaseBuilder extends Builder implements SimpleBuildStep {

	private final String connectionId;
	private final String credentialsId;
	private final String maxConditionCode;

	public SubmitJclBaseBuilder() {
		connectionId = null;
		credentialsId = null;
		maxConditionCode = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param connectionId
	 *            a unique host connection identifier
	 */
	protected SubmitJclBaseBuilder(String connectionId) {
		this.connectionId = StringUtils.trimToEmpty(connectionId);
		credentialsId = null;
		maxConditionCode = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param connectionId
	 *            a unique host connection identifier
	 * @param credentialsId
	 *            unique id of the selected credential
	 * @param maxConditionCode
	 *            a maximum condition code
	 */
	protected SubmitJclBaseBuilder(String connectionId, String credentialsId, String maxConditionCode) {
		this.connectionId = StringUtils.trimToEmpty(connectionId);
		this.credentialsId = StringUtils.trimToEmpty(credentialsId);
		this.maxConditionCode = StringUtils.trimToEmpty(maxConditionCode);
	}

	/**
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of connectionId
	 */
	public String getConnectionId() {
		return connectionId;
	}

	/**
	 * Gets the value of the 'Login credentials'.
	 * 
	 * @return <code>String</code> value of credentialsId
	 */
	public String getCredentialsId() {
		return credentialsId;
	}

	/**
	 * Gets the value of the 'Maximum Condition Code'.
	 * 
	 * @return <code>String</code> value of maxConditionCode
	 */
	public String getMaxConditionCode() {
		return maxConditionCode;
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
	 *             if an error in checking CLI compatibility, retrieving the environment or launching the CLI command
	 * @throws InterruptedException
	 *             if an error in checking CLI compatibility, retrieving the environment or launching the CLI command
	 */
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		// obtain argument values to pass to the CLI
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		VirtualChannel vChannel = launcher.getChannel();
		if (vChannel == null) {
			throw new IllegalStateException("Could not get virtual channel from launcher."); //$NON-NLS-1$
		}

		// Check CLI compatibility
		FilePath cliDirectory = new FilePath(vChannel, globalConfig.getTopazCLILocation(launcher));
		String cliVersion = CLIVersionUtils.getCLIVersion(cliDirectory, TopazUtilitiesConstants.JCL_MINIMUM_CLI_VERSION);
		CLIVersionUtils.checkCLICompatibility(cliVersion, TopazUtilitiesConstants.JCL_MINIMUM_CLI_VERSION);

		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? TopazUtilitiesConstants.SUBMIT_JCL_CLI_SH : TopazUtilitiesConstants.SUBMIT_JCL_CLI_BAT;	

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$

		String topazCliWorkspace = workspace.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE
				+ UUID.randomUUID().toString();
		FilePath topazDataDir = new FilePath(vChannel, topazCliWorkspace);
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		String maxConditionCodeStr = ArgumentUtils.escapeForScript(getMaxConditionCode());

		ArgumentListBuilder args = globalConfig.getArgumentBuilder(cliScriptFileRemote, cliVersion, run.getParent(), getCredentialsId(), getConnectionId());
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);
		args.add(TopazUtilitiesConstants.MAX_CC_PARM, maxConditionCodeStr);

		// Let extending classes add their arguments.
		addArguments(run, workspace, launcher, listener, args);

		logger.println();

		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = run.getEnvironment(listener);
		FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		workDir.mkdirs();

		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(logger).pwd(workDir).join();
		if (exitValue != 0) {
			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
			topazDataDir.deleteRecursive();
		}
	}

	/**
	 * Adds arguments to be sent to the CLI.
	 * <p>
	 * Extensions of this class should call this method to add their own arguments.
	 * 
	 * @param run
	 *            the current running Jenkins build
	 * @param workspace
	 *            the Jenkins job workspace directory
	 * @param launcher
	 *            the way to start a process
	 * @param listener
	 *            the build listener
	 * @param args
	 *            the argument list builder to add CLI arguments to
	 * 
	 * @throws IOException
	 *             if an error in the communication between {@link VirtualChannel}s when attempting to get remote system properties.
	 * @throws InterruptedException
	 *             if the current thread is interrupted while waiting for the completion of a call to get remote system properties.
	 */
	protected abstract void addArguments(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener,
			ArgumentListBuilder args) throws IOException, InterruptedException;

}