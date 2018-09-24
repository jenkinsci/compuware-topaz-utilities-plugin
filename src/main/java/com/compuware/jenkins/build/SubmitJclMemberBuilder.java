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
import jenkins.tasks.SimpleBuildStep;

/**
 * Captures the configuration information for a Submit JCL Member build step.
 */
public class SubmitJclMemberBuilder extends SubmitJclBaseBuilder implements SimpleBuildStep {

	private String jclMember;

	public SubmitJclMemberBuilder(String connectionId) {
		super(connectionId);
		jclMember = null;
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
	 * @param jclMember
	 *            the JCL datasets / members
	 */
	@DataBoundConstructor
	public SubmitJclMemberBuilder(String connectionId, String credentialsId, String maxConditionCode, String jclMember) {
		super(connectionId, credentialsId, maxConditionCode);
		this.jclMember = StringUtils.trimToEmpty(jclMember);
	}

	/**
	 * Gets the value of the 'Dataset(member)' statements.
	 * 
	 * @return <code>String</code> value of jclMember
	 */
	public String getJclMember() {
		return jclMember;
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
	 * DescriptorImpl is used to create instances of <code>SubmitJclMemberBuilder</code>. It also contains the global configuration options as
	 * fields, just like the <code>SubmitJclMemberBuilder</code> contains the configuration options for a job
	 */
	@Symbol("topazSubmitJclMembers")
	@Extension
	public static final class DescriptorImpl extends JclDescriptorImpl<Builder> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.jclMemberDescriptorDisplayName();
		}

		/**
		 * Validator for the 'Dataset(member)' field.
		 * 
		 * @param jclMember
		 *            the JCL passed from the config.jelly "jclMember" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckJclMember(@QueryParameter String jclMember) {
			String tempValue = StringUtils.trimToEmpty(jclMember);
			if (tempValue.isEmpty()) {
				return FormValidation.error(Messages.checkJclMemberError());
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

		String escapedJclMember = ArgumentUtils.escapeForScript(StringUtils.replaceChars(getJclMember(), '\n', ','));
		args.add(TopazUtilitiesConstants.JCL_DSNS, escapedJclMember);

		logger.println("jclMember: " + escapedJclMember); //$NON-NLS-1$
	}

}