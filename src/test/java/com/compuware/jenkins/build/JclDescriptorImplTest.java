/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2018 - 2019 Compuware Corporation
 * (c) Copyright 2018 - 2019, 2021 BMC Software, Inc.
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

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

/**
 * Test cases for {@link JclDescriptorImpl}.
 */
@SuppressWarnings("nls")
public class JclDescriptorImplTest {
	private static final String EXPECTED_MAX_CONDITION_CODE = "4";
	private static final String EXPECTED_HOST = "host";
	private static final String EXPECTED_PORT = "port";
	private static final String EXPECTED_USER_ID = "userid";

	public @Rule JenkinsRule rule = new JenkinsRule();

	private static final class BuilderImpl extends Builder {
		@Override
		public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
				throws InterruptedException, IOException {
			return true;
		}

		@Override
		public DescriptorImpl getDescriptor() {
			return (DescriptorImpl) super.getDescriptor();
		}

		@TestExtension
		public static final class DescriptorImpl extends JclDescriptorImpl<Builder> {
			@Override
			public Builder newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
				return new BuilderImpl();
			}

			@Override
			public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
				return true;
			}
		}
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.JclDescriptorImpl#isApplicable(java.lang.Class)}.
	 */
	@Test
	public void testIsApplicable() {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		assertTrue(descriptor.isApplicable(null));
	}

	/**
	 * Tests a null configuration.
	 */
	@Test
	public void testNullJsonConfigure() throws FormException {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		final JSONObject json = new JSONObject();
		final StaplerRequest req = null;
		assertTrue(descriptor.configure(req, json));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.JclDescriptorImpl#doCheckConnectionId(java.lang.String)}.
	 */
	@Test
	public void testDoCheckConnectionId() {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		assertEquals(Messages.checkHostConnectionError(), descriptor.doCheckConnectionId(null).getMessage());
		assertEquals(Messages.checkHostConnectionError(), descriptor.doCheckConnectionId(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckConnectionId(EXPECTED_HOST + ":" + EXPECTED_PORT));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.JclDescriptorImpl#doCheckCredentialsId(java.lang.String)}.
	 */
	@Test
	public void testDoCheckCredentialsId() {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		assertEquals(Messages.checkLoginCredentialsError(), descriptor.doCheckCredentialsId(null).getMessage());
		assertEquals(Messages.checkLoginCredentialsError(), descriptor.doCheckCredentialsId(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckCredentialsId(EXPECTED_USER_ID));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.JclDescriptorImpl#doCheckMaxConditionCode(java.lang.String)}.
	 */
	@Test
	public void testDoCheckMaxConditionCode() {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		assertEquals(Messages.checkMaxConditionCodeError(), descriptor.doCheckMaxConditionCode(null).getMessage());
		assertEquals(Messages.checkMaxConditionCodeError(), descriptor.doCheckMaxConditionCode(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckMaxConditionCode(EXPECTED_MAX_CONDITION_CODE));
	}

	/**
	 * Test method for
	 * {@link com.compuware.jenkins.build.JclDescriptorImpl#doFillConnectionIdItems(jenkins.model.Jenkins, java.lang.String, hudson.model.Item)}.
	 */
	@Test
	public void testDoFillConnectionIdItems() {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		ListBoxModel dropDownList = descriptor.doFillConnectionIdItems(null, null, null);
		assertNotNull(dropDownList);
		assertEquals("", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);

		String connectionId = null;
		final HostConnection hostConnection = new HostConnection("description", "hostPort", "protocol", "codePage", "timeOut", "connectionId",
				"cesUrl");
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		globalConfig.addHostConnection(hostConnection);

		connectionId = hostConnection.getConnectionId();
		dropDownList = descriptor.doFillConnectionIdItems(null, connectionId, null);

		assertEquals("", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);
		final String connection = dropDownList.get(1).value;
		assertEquals(connectionId, connection);
	}

	/**
	 * Test method for
	 * {@link com.compuware.jenkins.build.JclDescriptorImpl#doFillCredentialsIdItems(jenkins.model.Jenkins, java.lang.String, hudson.model.Item)}.
	 */
	@Test
	public void testDoFillCredentialsIdItems() {
		BuilderImpl.DescriptorImpl descriptor = new BuilderImpl.DescriptorImpl();

		ListBoxModel dropDownList = descriptor.doFillCredentialsIdItems(null, null, null);
		assertNotNull(dropDownList);
		assertEquals("", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);
		String credentialId = null;
		try {
			final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
					"TEST_USERNAME", "TEST_PASSWORD");
			UserFacingAction action = new UserFacingAction();
			action.getStore().addCredentials(Domain.global(), credential);
			credentialId = credential.getId();
		} catch (final IOException e) {
			e.printStackTrace();
			assertNull(e);
		}
		dropDownList = descriptor.doFillCredentialsIdItems(null, credentialId, null);
		assertEquals("", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);
		final String credentials = dropDownList.get(1).value;
		assertEquals(credentialId, credentials);
	}

}
