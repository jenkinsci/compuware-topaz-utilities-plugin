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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.Stapler;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * SubmitJclBuilder unit tests.
 */
@SuppressWarnings("nls")
public class SubmitJclBuilderTest
{
	// Constants
	private static final String EXPECTED_CONNECTION_ID = "12345";
	private static final String EXPECTED_CREDENTIALS_ID = "67890";
	private static final String EXPECTED_MAX_CONDITION_CODE = "4";
	private static final String EXPECTED_TYPE = "jclMembersType";
	private static final String EXPECTED_JCL_MEMBERS_STRING = "MYJCL(JCLMEM1)\nMYJCL(JCLMEM2)\nMYJCL(JCLMEM3)";
	private static final String EXPECTED_JCL = "";
	// "//BOGUS JOB (MMC),'BOGUS',CLASS=A,\r\n" +
	// "// MSGCLASS=X,NOTIFY=&SYSUID\r\n" +
	// "//";

	private static final String EXPECTED_HOST = "cw01";
	private static final String EXPECTED_PORT = "30947";
	private static final String EXPECTED_CES_URL = "https://expectedcesurl/";
	private static final String EXPECTED_CODE_PAGE = "1047";
	private static final String EXPECTED_TIMEOUT = "123";
	private static final String EXPECTED_USER_ID = "xdevreg";
	private static final String EXPECTED_PASSWORD = "********";

	// Member Variables
	@Rule
	public JenkinsRule m_jenkinsRule = new JenkinsRule();
	private CpwrGlobalConfiguration m_globalConfig;

	@Before
	public void setup()
	{
		try
		{
			JSONObject hostConnection = new JSONObject();
			hostConnection.put("description", "TestConnection");
			hostConnection.put("hostPort", EXPECTED_HOST + ':' + EXPECTED_PORT);
			hostConnection.put("codePage", EXPECTED_CODE_PAGE);
			hostConnection.put("timeout", EXPECTED_TIMEOUT);
			hostConnection.put("connectionId", EXPECTED_CONNECTION_ID);
			hostConnection.put("cesUrl", EXPECTED_CES_URL);

			JSONArray hostConnections = new JSONArray();
			hostConnections.add(hostConnection);

			JSONObject json = new JSONObject();
			json.put("hostConn", hostConnections);
			json.put("topazCLILocationLinux", "/opt/Compuware/TopazCLI");
			json.put("topazCLILocationWindows", "C:\\Program Files\\Compuware\\Topaz Workbench CLI");

			m_globalConfig = CpwrGlobalConfiguration.get();
			m_globalConfig.configure(Stapler.getCurrentRequest(), json);

			SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(
					CredentialsScope.USER, EXPECTED_CREDENTIALS_ID, null, EXPECTED_USER_ID, EXPECTED_PASSWORD));
			SystemCredentialsProvider.getInstance().save();
		}
		catch (Exception e)
		{
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the construction of a builder, verifying configuration values.
	 */
	@Test
	public void constructBuilderTest()
	{
		SubmitJclBuilder builder = new SubmitJclBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID,
				EXPECTED_MAX_CONDITION_CODE, EXPECTED_JCL_MEMBERS_STRING, EXPECTED_JCL, EXPECTED_TYPE);

		assertThat(String.format("Expected SubmitJclBuilder.getConnectionId() to return %s", EXPECTED_CONNECTION_ID),
				builder.getConnectionId(), is(equalTo(EXPECTED_CONNECTION_ID)));

		assertThat(String.format("Expected SubmitJclBuilder.getCredentialsId() to return %s", EXPECTED_CREDENTIALS_ID),
				builder.getCredentialsId(), is(equalTo(EXPECTED_CREDENTIALS_ID)));

		assertThat(String.format("Expected SubmitJclBuilder.getMaxConditionCode() to return %s", EXPECTED_MAX_CONDITION_CODE),
				builder.getMaxConditionCode(), is(equalTo(EXPECTED_MAX_CONDITION_CODE)));

		assertThat(String.format("Expected SubmitJclBuilder.getJclMembers() to return %s", EXPECTED_JCL_MEMBERS_STRING),
				builder.getJclMembers(), is(equalTo(EXPECTED_JCL_MEMBERS_STRING)));
	}

	/**
	 * Tests build descriptor values, such as default values and display name.
	 */
	@Test
	public void descriptorValuesTest()
	{
		try
		{
			JclDescriptorImpl descriptor = new JclDescriptorImpl();

			String displayName = descriptor.getDisplayName();
			assertThat("Expected SubmitJclBuilder.DescriptorImpl.getDisplayName() to not be null.", displayName,
					is(notNullValue()));
			assertThat("Expected SubmitJclBuilder.DescriptorImpl.getDisplayName() to not be empty.", displayName.isEmpty(),
					is(false));
		}
		catch (Exception e)
		{
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the results of an execution.
	 * <p>
	 * A project is created, configured and executed where the log is examined to verify results.
	 */
	@Test
	public void executionTest()
	{
		try
		{
			FreeStyleProject project = m_jenkinsRule.createFreeStyleProject("TestProject");
			project.getBuildersList().add(new SubmitJclBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID,
					EXPECTED_MAX_CONDITION_CODE, EXPECTED_JCL_MEMBERS_STRING, EXPECTED_JCL, EXPECTED_TYPE));

			// don't expect the build to succeed since no CLI exists
			if (project.scheduleBuild(null))
			{
				while (project.getLastCompletedBuild() == null)
				{
					// wait for the build to complete before obtaining the log
					continue;
				}

				FreeStyleBuild build = project.getLastCompletedBuild();
				String logFileOutput = JenkinsRule.getLog(build);

				/*
				String expectedConnectionStr = String.format("-host \"%s\" -port \"%s\"", EXPECTED_HOST, EXPECTED_PORT);
				assertThat("Expected log to contain Host connection: " + expectedConnectionStr + '.', logFileOutput,
						containsString(expectedConnectionStr));
				
				String expectedCodePageStr = String.format("-code %s", EXPECTED_CODE_PAGE);
				assertThat("Expected log to contain Host code page: " + expectedCodePageStr + '.', logFileOutput,
						containsString(expectedCodePageStr));
				
				String expectedTimeoutStr = String.format("-timeout \"%s\"", EXPECTED_TIMEOUT);
				assertThat("Expected log to contain Host timeout: " + expectedTimeoutStr + '.', logFileOutput,
						containsString(expectedTimeoutStr));
				
				String expectedCredentialsStr = String.format("-id \"%s\" -pass %s", EXPECTED_USER_ID, EXPECTED_PASSWORD);
				assertThat("Expected log to contain Login credentials: " + expectedCredentialsStr + '.', logFileOutput,
						containsString(expectedCredentialsStr));
				
				assertThat(String.format("Expected log to contain Analysis properties path: \"%s\".",
						EXPECTED_ANALYSIS_PROPERTIES_FILEPATH), logFileOutput, containsString(EXPECTED_ANALYSIS_PROPERTIES_FILEPATH));
				
				assertThat(String.format("Expected log to contain Analysis properties: \"%s\".", EXPECTED_ANALYSIS_PROPERTIES_STRING),
						logFileOutput, containsString(EXPECTED_ANALYSIS_PROPERTIES_STRING));
				*/
			}
		}
		catch (Exception e)
		{
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Perform a round trip test on the Submit JCL configuration builder.
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration for equality.
	 */
	@Test
	public void roundTripTest()
	{
		try
		{
			FreeStyleProject project = m_jenkinsRule.createFreeStyleProject("TestProject");
			SubmitJclBuilder before = new SubmitJclBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID,
					EXPECTED_MAX_CONDITION_CODE, EXPECTED_JCL_MEMBERS_STRING, "", EXPECTED_TYPE);
			project.getBuildersList().add(before);

			// workaround for eclipse compiler Ambiguous method call
			project.save();
			m_jenkinsRule.jenkins.reload();

			FreeStyleProject reloaded = m_jenkinsRule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);
			assertNotNull(reloaded);

			SubmitJclBuilder after = reloaded.getBuildersList().get(SubmitJclBuilder.class);
			assertNotNull(after);

			m_jenkinsRule.assertEqualBeans(before, after, "connectionId,credentialsId,maxConditionCode,jclMembers,jcl");
		}
		catch (Exception e)
		{
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}