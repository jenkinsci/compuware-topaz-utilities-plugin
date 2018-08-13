/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2018 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.build;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;

import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 
 */
public class JclDescriptorImplTest {
	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_CONNECTION_ID = "12345";
	private static final String EXPECTED_CREDENTIALS_ID = "67890";
	private static final String EXPECTED_MAX_CONDITION_CODE = "4";
	private static final String EXPECTED_JCL_MEMBERS_STRING = 
			"A.B.MYJCL\n" + 
			"A.B.MYJCL2\n" + 
			"MYJCL(JCLMEM3)";
	private static final String EXPECTED_JCL = 
			"//* This JCL simply migrates a file.\r\n" + 
			"//*\r\n" + 
			"//* 'IKJEFT01' is a utility program that can be used to run TSO\r\n" + 
			"//* commands from JCL (in this case, Migrate)\r\n" + 
			"//*\r\n" + 
			"//TESTMIG JOB ('ACCT#',LOCAL),'NAME',CLASS=A,\r\n" + 
			"//             MSGCLASS=R,NOTIFY=&SYSUID,PRTY=13,MSGLEVEL=(1,1)\r\n" + 
			"//STEP1 EXEC PGM=IKJEFT01\r\n" + 
			"//SYSTSPRT DD SYSOUT=*\r\n" + 
			"//SYSTSIN DD *\r\n" + 
			"   HMIGRATE 'TEST.COBOL.PDS'\r\n" + 
			"//";
	private static final String EXPECTED_HOST = "cw01";
	private static final String EXPECTED_PORT = "30947";
	private static final String EXPECTED_CES_URL = "https://expectedcesurl/";
	private static final String EXPECTED_CODE_PAGE = "1047";
	private static final String EXPECTED_TIMEOUT = "123";
	private static final String EXPECTED_USER_ID = "xdevreg";
	private static final String EXPECTED_PASSWORD = "********";
	/* @formatter:on */

	@Rule
	public JenkinsRule j = new JenkinsRule();

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#isApplicable(java.lang.Class)}.
	 */
	@Test
	public void testIsApplicable() {
		JclDescriptorImpl descriptor = new JclDescriptorImpl();
		assertTrue(descriptor.isApplicable(null));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#getDisplayName()}.
	 */
	@Test
	public void testGetDisplayName() {
		JclDescriptorImpl descriptor = new JclDescriptorImpl();
		assertEquals(Messages.descriptorDisplayName(), descriptor.getDisplayName());
	}

	/**
	 * Tests a null configuration.
	 */
	@Test
	public void testNullJsonConfigure() throws FormException {
		final JclDescriptorImpl descriptor = new JclDescriptorImpl();
		final JSONObject json = new JSONObject();
		final StaplerRequest req = null;
		assertTrue(descriptor.configure(req, json));
	}
	
//	/**
//	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)}.
//	 */
//	@Test
//	public void testConfigureStaplerRequestJSONObject() throws FormException {
//		final JclDescriptorImpl descriptor = new JclDescriptorImpl();
//		
//		final JSONObject hostConnection = new JSONObject();
//		hostConnection.put("description", "TestConnection");
//		hostConnection.put("hostPort", EXPECTED_HOST + ':' + EXPECTED_PORT);
//		hostConnection.put("codePage", EXPECTED_CODE_PAGE);
//		hostConnection.put("timeout", EXPECTED_TIMEOUT);
//		hostConnection.put("connectionId", EXPECTED_CONNECTION_ID);
//		hostConnection.put("cesUrl", EXPECTED_CES_URL);
//
//		JSONArray hostConnections = new JSONArray();
//		hostConnections.add(hostConnection);
//
//		final JSONObject json = new JSONObject();
//		json.put("hostConn", hostConnections);
//		json.put("topazCLILocationLinux", "/opt/Compuware/TopazCLI");
//		json.put("topazCLILocationWindows", "C:\\Program Files\\Compuware\\Topaz Workbench CLI");
//
//		final StaplerRequest req = null;
//		assertTrue(descriptor.configure(req, json));
//
//		descriptor.load();
//		
//		System.out.print("here");
//
////		assertEquals(1, servers.size());
////		assertEquals("Test Server Name", servers.get(0).getProtexPostServerName());
////		assertEquals("https://www.google.com", servers.get(0).getProtexPostServerUrl());
////		assertEquals("999", servers.get(0).getProtexPostServerTimeOut());
//	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doCheckConnectionId(java.lang.String)}.
	 */
	@Test
	public void testDoCheckConnectionId() {
		final JclDescriptorImpl descriptor = new JclDescriptorImpl();

		assertEquals(Messages.checkHostConnectionError(), descriptor.doCheckConnectionId(null).getMessage());
		assertEquals(Messages.checkHostConnectionError(), descriptor.doCheckConnectionId(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckConnectionId(EXPECTED_HOST + ":" + EXPECTED_PORT)); //$NON-NLS-1$
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doCheckCredentialsId(java.lang.String)}.
	 */
	@Test
	public void testDoCheckCredentialsId() {
		final JclDescriptorImpl descriptor = new JclDescriptorImpl();

		assertEquals(Messages.checkLoginCredentialsError(), descriptor.doCheckCredentialsId(null).getMessage());
		assertEquals(Messages.checkLoginCredentialsError(), descriptor.doCheckCredentialsId(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckCredentialsId(EXPECTED_USER_ID));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doCheckMaxConditionCode(java.lang.String)}.
	 */
	@Test
	public void testDoCheckMaxConditionCode() {
		final JclDescriptorImpl descriptor = new JclDescriptorImpl();

		assertEquals(Messages.checkMaxConditionCodeError(), descriptor.doCheckMaxConditionCode(null).getMessage());
		assertEquals(Messages.checkMaxConditionCodeError(), descriptor.doCheckMaxConditionCode(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckMaxConditionCode(EXPECTED_MAX_CONDITION_CODE));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doCheckJcl(java.lang.String)}.
	 */
	@Test
	public void testDoCheckJcl() {
		final JclDescriptorImpl descriptor = new JclDescriptorImpl();

		assertEquals(Messages.checkJclError(), descriptor.doCheckJcl(null).getMessage());
		assertEquals(Messages.checkJclError(), descriptor.doCheckJcl(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckJcl(EXPECTED_JCL_MEMBERS_STRING));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doFillConnectionIdItems(jenkins.model.Jenkins, java.lang.String, hudson.model.Item)}.
	 */
	//@Test
	public void testDoFillConnectionIdItems() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doFillCredentialsIdItems(jenkins.model.Jenkins, java.lang.String, hudson.model.Item)}.
	 */
	@Test
	public void testDoFillCredentialsIdItems() {
		final JclDescriptorImpl descriptor = new JclDescriptorImpl();
		ListBoxModel dropDownList = descriptor.doFillCredentialsIdItems(null, null, null);
		assertNotNull(dropDownList);
		assertEquals("", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);
		String credentialId = null;
		try {
			final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null, "TEST_USERNAME", "TEST_PASSWORD");
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
