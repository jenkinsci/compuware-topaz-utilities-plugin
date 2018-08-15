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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for a Submit JCL build step.
 */
public class SubmitJclBuilder extends Builder implements SimpleBuildStep {
	// Member Variables
	private final String m_connectionId;
	private final String m_credentialsId;
	private final String m_maxConditionCode;
	private String m_jcl;

	public SubmitJclBuilder(String connectionId) {
		m_connectionId = StringUtils.trimToEmpty(connectionId);
		m_credentialsId = null;
		m_maxConditionCode = null;
		m_jcl = null;
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
		m_connectionId = StringUtils.trimToEmpty(connectionId);
		m_credentialsId = StringUtils.trimToEmpty(credentialsId);
		m_maxConditionCode = StringUtils.trimToEmpty(maxConditionCode);
		m_jcl = StringUtils.trimToEmpty(jcl);
	}

	/**
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of m_connectionId
	 */
	public String getConnectionId() {
		return m_connectionId;
	}

	/**
	 * Gets the value of the 'Login credentials'.
	 * 
	 * @return <code>String</code> value of m_credentialsId
	 */
	public String getCredentialsId() {
		return m_credentialsId;
	}

	/**
	 * Gets the value of the 'Max Condition Code'.
	 * 
	 * @return <code>String</code> value of m_maxConditionCode
	 */
	public String getMaxConditionCode() {
		return m_maxConditionCode;
	}

	/**
	 * Gets the value of the 'JCL' statements.
	 * 
	 * @return <code>String</code> value of m_jcl
	 */
	public String getJcl() {
		return m_jcl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.Builder#getDescriptor()
	 */
	@Override
	public JclDescriptorImpl getDescriptor() {
		return (JclDescriptorImpl) super.getDescriptor();
	}

	/**
	 * DescriptorImpl is used to create instances of <code>SubmitJclBuilder</code>. It also contains the global configuration options as
	 * fields, just like the <code>SubmitJclBuilder</code> contains the configuration options for a job
	 */
	@Extension
	public static final class JclDescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * Constructor.
		 * <p>
		 * In order to load the persisted global configuration, you have to call load() in the constructor.
		 */
		public JclDescriptorImpl() {
			super(SubmitJclBuilder.class);
			load();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@Override
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.descriptorDisplayName();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		/**
		 * Validator for the 'Host connection' field.
		 * 
		 * @param connectionId
		 *            unique identifier for the host connection passed from the config.jelly "connectionId" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckConnectionId(@QueryParameter String connectionId) {
			String tempValue = StringUtils.trimToEmpty(connectionId);
			if (tempValue.isEmpty()) {
				return FormValidation.error(Messages.checkHostConnectionError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Login credentials' field.
		 * 
		 * @param credentialsId
		 *            login credentials passed from the config.jelly "credentialsId" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId) {
			String tempValue = StringUtils.trimToEmpty(credentialsId);
			if (tempValue.isEmpty()) {
				return FormValidation.error(Messages.checkLoginCredentialsError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Max Condition Code' field.
		 * 
		 * @param maxConditionCode
		 *            a maximum condition code passed from the config.jelly "maxConditionCode" field
		 * 
		 * @return validation message
		 */
		public FormValidation doCheckMaxConditionCode(@QueryParameter String maxConditionCode) {
			String tempValue = StringUtils.trimToEmpty(maxConditionCode);
			if (tempValue.isEmpty()) {
				return FormValidation.error(Messages.checkMaxConditionCodeError());
			}

			return FormValidation.ok();
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

		/**
		 * Fills in the Host Connection selection box with applicable connections.
		 * 
		 * @param context
		 *            filter for host connections
		 * @param connectionId
		 *            an existing host connection identifier; can be null
		 * @param project
		 *            the Jenkins project
		 * 
		 * @return host connection selections
		 */
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project) {
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection[] hostConnections = globalConfig.getHostConnections();

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (HostConnection connection : hostConnections) {
				boolean isSelected = false;
				if (connectionId != null) {
					isSelected = connectionId.matches(connection.getConnectionId());
				}

				model.add(new Option(connection.getDescription() + " [" + connection.getHostPort() + ']', //$NON-NLS-1$
						connection.getConnectionId(), isSelected));
			}

			return model;
		}

		/**
		 * Fills in the Login Credentials selection box with applicable connections.
		 * 
		 * @param context
		 *            filter for login credentials
		 * @param credentialsId
		 *            existing login credentials; can be null
		 * @param project
		 *            the Jenkins project
		 * 
		 * @return login credentials selection
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project) {
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (StandardUsernamePasswordCredentials c : creds) {
				boolean isSelected = false;
				if (credentialsId != null) {
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ')' : StringUtils.EMPTY), //$NON-NLS-1$
						c.getId(), isSelected));
			}

			return model;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		SubmitJclScanner scanner = new SubmitJclScanner(this);
		scanner.perform(run, workspace, launcher, listener);
	}
}