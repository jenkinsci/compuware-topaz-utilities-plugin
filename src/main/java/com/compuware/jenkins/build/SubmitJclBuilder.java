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

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.compuware.jenkins.build.utils.TopazUtilitiesConstants;
import com.compuware.jenkins.common.utils.ArgumentUtils;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

/**
 * Captures the configuration information for a Submit JCL build step.
 */
public class SubmitJclBuilder extends SubmitJclBaseBuilder {

	private String jcl;
	private FilePath jclFile;

	public SubmitJclBuilder(String connectionId) {
		super(connectionId);
		jcl = null;
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
	 * @param jcl
	 *            the JCL statements
	 */
	@DataBoundConstructor
	public SubmitJclBuilder(String connectionId, String credentialsId, String maxConditionCode, String jcl) {
		super(connectionId, credentialsId, maxConditionCode);
		this.jcl = StringUtils.trimToEmpty(jcl);
	}

	/**
	 * Gets the value of the 'JCL' statements.
	 * 
	 * @return <code>String</code> value of jcl
	 */
	public String getJcl() {
		return jcl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.Builder#getDescriptor()
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * DescriptorImpl is used to create instances of <code>SubmitJclBuilder</code>. It also contains the global configuration options as
	 * fields, just like the <code>SubmitJclBuilder</code> contains the configuration options for a job
	 */
	@Symbol("topazSubmitFreeFormJcl")
	@Extension
	public static final class DescriptorImpl extends JclDescriptorImpl<Builder> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.jclDescriptorDisplayName();
		}

		/**
		 * Validator for the 'JCL' field.
		 * 
		 * @param jcl
		 *            the JCL passed from the config.jelly "jcl" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckJcl(@QueryParameter String jcl) {
			String tempValue = StringUtils.trimToEmpty(jcl);
			if (tempValue.isEmpty()) {
				return FormValidation.error(Messages.checkJclError());
			}

			return FormValidation.ok();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.compuware.jenkins.build.SubmitJclBaseBuilder#addArguments(hudson.model.Run, hudson.FilePath, hudson.Launcher,
	 * hudson.model.TaskListener, hudson.util.ArgumentListBuilder)
	 */
	@Override
	protected void addArguments(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, ArgumentListBuilder args)
			throws IOException, InterruptedException {
		PrintStream logger = listener.getLogger();

		jclFile = workspace.createTextTempFile("jcl", ".txt", getJcl()); //$NON-NLS-1$ //$NON-NLS-2$
		String escapedJclFileName = ArgumentUtils.escapeForScript(jclFile.getRemote());
		logger.println("jcl: " + escapedJclFileName); //$NON-NLS-1$

		args.add(TopazUtilitiesConstants.JCL, escapedJclFileName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.compuware.jenkins.build.SubmitJclBaseBuilder#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher,
	 * hudson.model.TaskListener)
	 */
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		try {
			super.perform(run, workspace, launcher, listener);
		} finally {
			cleanUp();
		}
	}

	/**
	 * Handle clean up when finished builder execution.
	 * <p>
	 * A temporary JCL file is created during builder execution that is consumed by the CLI and is 
	 * deleted when execution is finished.
	 * 
	 * @throws IOException
	 *             if unable to delete temporary JCL file
	 * @throws InterruptedException
	 *             if unable to delete temporary JCL file
	 */
	protected void cleanUp() throws IOException, InterruptedException {
		if (jclFile != null) {
			jclFile.delete();
		}
	}

}