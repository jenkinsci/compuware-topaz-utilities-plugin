/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2018 Compuware Corporation
 * (c) Copyright 2018, 2021 BMC Software, Inc.
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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * DescriptorImpl is used to create instances of <code>SubmitJclBuilder</code>. It also contains the global configuration options as fields,
 * just like the <code>SubmitJclBuilder</code> contains the configuration options for a job
 */
public abstract class JclDescriptorImpl<T extends BuildStep & Describable<T>> extends BuildStepDescriptor<Builder> {
	/**
	 * Constructor.
	 * <p>
	 * In order to load the persisted global configuration, you have to call load() in the constructor.
	 */
	public JclDescriptorImpl() {
		load();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
	 */
	@Override
	public boolean isApplicable(Class<? extends AbstractProject> aClass) {
		// Indicates that this builder can be used with all kinds of project types
		return true;
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
		List<StandardCredentials> creds = CredentialsProvider.lookupCredentials(StandardCredentials.class,
				project, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

		ListBoxModel model = new ListBoxModel();
		model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

		for (StandardCredentials c : creds) {
			boolean isSelected = false;
			if (credentialsId != null) {
				isSelected = credentialsId.matches(c.getId());
			}

			String description = Util.fixEmptyAndTrim(c.getDescription());
			model.add(new Option(CpwrGlobalConfiguration.get().getCredentialsUser(c) + (description != null ? (" (" + description + ')') : StringUtils.EMPTY), //$NON-NLS-1$
					c.getId(), isSelected));
		}

		return model;
	}
}
