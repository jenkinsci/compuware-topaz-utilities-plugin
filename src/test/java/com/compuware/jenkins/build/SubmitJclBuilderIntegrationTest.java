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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.Stapler;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Test cases for {@link SubmitJclBuilder}.
 */
@SuppressWarnings("nls")
public class SubmitJclBuilderIntegrationTest {
	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_CONNECTION_ID = "12345";
	private static final String EXPECTED_CREDENTIALS_ID = "67890";
	private static final String EXPECTED_MAX_CONDITION_CODE = "4";
	private static final String EXPECTED_JCLDSNS_ARG_VALUE = "A.B.MYJCL,A.B.MYJCL2,MYJCL(JCLMEM3)";
	private static final String EXPECTED_JCL_MEMBERS = 
			"A.B.MYJCL\n" + 
			"A.B.MYJCL2\n" + 
			"MYJCL(JCLMEM3)";
	private static final String EXPECTED_JCL = 
			"//* This JCL simply migrates a file.\n" +
			"//*\n" +
			"//* 'IKJEFT01' is a utility program that can be used to run TSO\n" +
			"//* commands from JCL (in this case, Migrate)\n" +
			"//*\n" +
			"//TESTMIG JOB ('ACCT#',LOCAL),'NAME',CLASS=A,\n" +
            "//			             MSGCLASS=R,NOTIFY=&SYSUID,PRTY=13,MSGLEVEL=(1,1)\n" +
			"//STEP1 EXEC PGM=IKJEFT01\n" +
			"//SYSTSPRT DD SYSOUT=*\n" +
			"//SYSTSIN DD *\n" +
			"//   HMIGRATE 'TEST.COBOL.PDS'\n" +
			"//";
	private static final String EXPECTED_HOST = "cw01";
	private static final String EXPECTED_PORT = "30947";
	private static final String EXPECTED_CES_URL = "https://expectedcesurl/";
	private static final String EXPECTED_CODE_PAGE = "1047";
	private static final String EXPECTED_TIMEOUT = "123";
	private static final String EXPECTED_USER_ID = "xdevreg";
	private static final String EXPECTED_PASSWORD = "********";
	/* @formatter:on */

	// Member variables
	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();
	private CpwrGlobalConfiguration globalConfig;

	@ClassRule
	public static BuildWatcher bw = new BuildWatcher();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setup() {
		try {
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

			globalConfig = CpwrGlobalConfiguration.get();
			globalConfig.configure(Stapler.getCurrentRequest(), json);

			SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(CredentialsScope.USER,
					EXPECTED_CREDENTIALS_ID, null, EXPECTED_USER_ID, EXPECTED_PASSWORD));
			SystemCredentialsProvider.getInstance().save();
		} catch (Exception e) {
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
	public void testJclMemberExecution() {
		try {
			FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
			project.getBuildersList().add(new SubmitJclMemberBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID,
					EXPECTED_MAX_CONDITION_CODE, EXPECTED_JCL_MEMBERS));

			// don't expect the build to succeed since no CLI exists
			if (project.scheduleBuild(null)) {
				while (project.getLastCompletedBuild() == null) {
					// wait for the build to complete before obtaining the log
					continue;
				}

				FreeStyleBuild build = project.getLastCompletedBuild();
				String logFileOutput = JenkinsRule.getLog(build);

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

				String expectedMaxCcStr = String.format("-maxcc \"%s\"", EXPECTED_MAX_CONDITION_CODE);
				assertThat("Expected log to contain maximum condition code: " + expectedMaxCcStr + '.', logFileOutput,
						containsString(expectedMaxCcStr));

				String expectedJclMembersStr = String.format("-jcldsns \"%s\"", EXPECTED_JCLDSNS_ARG_VALUE);
				assertThat("Expected log to contain jcl members: " + expectedJclMembersStr + '.', logFileOutput,
						containsString(expectedJclMembersStr));
			}
		} catch (Exception e) {
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
	public void testFreeFormJclExecution() {
		try {
			FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
			project.getBuildersList()
					.add(new SubmitJclBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID, EXPECTED_MAX_CONDITION_CODE, EXPECTED_JCL));

			// don't expect the build to succeed since no CLI exists
			if (project.scheduleBuild(null)) {
				while (project.getLastCompletedBuild() == null) {
					// wait for the build to complete before obtaining the log
					continue;
				}

				FreeStyleBuild build = project.getLastCompletedBuild();
				String logFileOutput = JenkinsRule.getLog(build);

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

				String expectedMaxCcStr = String.format("-maxcc \"%s\"", EXPECTED_MAX_CONDITION_CODE);
				assertThat("Expected log to contain maximum condition code: " + expectedMaxCcStr + '.', logFileOutput,
						containsString(expectedMaxCcStr));

				String workspaceRemotePath = build.getWorkspace().getRemote();
				String expectedJclStr = "-jcl \"" + workspaceRemotePath + "\\jcl.*(?:.txt)\"";
				Pattern pattern = Pattern.compile(StringUtils.replace(expectedJclStr, "\\", "\\\\"));
				Matcher matcher = pattern.matcher(logFileOutput);
				assertThat(String.format("Expected log to contain jcl file: -jcl \"%s\\jcl<timestamp>.txt\"", workspaceRemotePath),
						matcher.find(), is(true));
			}
		} catch (Exception e) {
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Perform a round trip test on the Submit JCL configuration builder.
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against the reloaded
	 * configuration for equality.
	 */
	@Test
	public void testRoundTrip() {
		try {
			FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
			SubmitJclBuilder before = new SubmitJclBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID, EXPECTED_MAX_CONDITION_CODE,
					EXPECTED_JCL_MEMBERS);
			project.getBuildersList().add(before);

			// workaround for eclipse compiler Ambiguous method call
			project.save();
			jenkinsRule.jenkins.reload();

			FreeStyleProject reloaded = jenkinsRule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);
			assertNotNull(reloaded);

			SubmitJclBuilder after = reloaded.getBuildersList().get(SubmitJclBuilder.class);
			assertNotNull(after);

			jenkinsRule.assertEqualBeans(before, after, "connectionId,credentialsId,maxConditionCode,jcl");
		} catch (Exception e) {
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}