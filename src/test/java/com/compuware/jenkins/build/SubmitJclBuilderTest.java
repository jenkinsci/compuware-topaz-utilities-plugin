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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.compuware.jenkins.build.SubmitJclBuilder.DescriptorImpl;
import com.compuware.jenkins.build.utils.TopazUtilitiesConstants;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.LogTaskListener;

/**
 * Test cases for {@link SubmitJclBuilder}.
 */
@SuppressWarnings("nls")
public class SubmitJclBuilderTest {

	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_CONNECTION_ID = "12345";
	private static final String EXPECTED_CREDENTIALS_ID = "67890";
	private static final String EXPECTED_MAX_CONDITION_CODE = "4";
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
	/* @formatter:on */

	public @Rule JenkinsRule rule = new JenkinsRule();

	/**
	 * Sets this class up for test execution.
	 * 
	 * @throws URISyntaxException
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), "jcl*.{txt}");
		for (Path entry : stream) {
			Files.deleteIfExists(entry);
		}
	}

	/**
	 * Test method for
	 * {@link com.compuware.jenkins.build.SubmitJclBuilder#SubmitJclBuilder(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
	 */
	@Test
	public void testSubmitJclBuilder() {
		SubmitJclBuilder builder = new SubmitJclBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID, EXPECTED_MAX_CONDITION_CODE,
				EXPECTED_JCL);

		assertThat(String.format("Expected SubmitJclBuilder.getConnectionId() to return %s", EXPECTED_CONNECTION_ID),
				builder.getConnectionId(), is(equalTo(EXPECTED_CONNECTION_ID)));

		assertThat(String.format("Expected SubmitJclBuilder.getCredentialsId() to return %s", EXPECTED_CREDENTIALS_ID),
				builder.getCredentialsId(), is(equalTo(EXPECTED_CREDENTIALS_ID)));

		assertThat(String.format("Expected SubmitJclBuilder.getMaxConditionCode() to return %s", EXPECTED_MAX_CONDITION_CODE),
				builder.getMaxConditionCode(), is(equalTo(EXPECTED_MAX_CONDITION_CODE)));

		assertThat(String.format("Expected SubmitJclBuilder.getJcl() to return %s", EXPECTED_JCL), builder.getJcl(),
				is(equalTo(EXPECTED_JCL)));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder#getDescriptor()}
	 */
	@Test
	public void testGetDescriptor() {
		SubmitJclBuilder submitJclBuilder = new SubmitJclBuilder("connectionId", "credentialsId", "4", "jcl");
		DescriptorImpl descriptor = submitJclBuilder.getDescriptor();

		assertThat("Expected SubmitJclBuilder.DescriptorImpl.getDisplayName() to not be null.", descriptor.getDisplayName(),
				is(notNullValue()));
		assertThat("Expected SubmitJclBuilder.DescriptorImpl.getDisplayName() to not be empty.", descriptor.getDisplayName().isEmpty(),
				is(false));
		assertEquals("Topaz submit free-form JCL", descriptor.getDisplayName());
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder.JclDescriptorImpl#doCheckJcl(java.lang.String)}.
	 */
	@Test
	public void testDoCheckJcl() {
		final DescriptorImpl descriptor = new DescriptorImpl();

		assertEquals(Messages.checkJclError(), descriptor.doCheckJcl(null).getMessage());
		assertEquals(Messages.checkJclError(), descriptor.doCheckJcl(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckJcl(EXPECTED_JCL));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder#getJcl()}
	 */
	// @Test
	public void testGetJcl() {
		// Test via other test methods.
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder#addArguments(Run<?, ?>, FilePath, Launcher, TaskListener, ArgumentListBuilder)}
	 */
	@Test
	public void testBuildArgumentList() throws IOException, InterruptedException {
		SubmitJclBuilder submitJclBuilder = Mockito.spy(new SubmitJclBuilder("connectionId", "credentialsId", "4", EXPECTED_JCL));
		FilePath workspace = new FilePath((VirtualChannel) null, "");
		TaskListener listener = Mockito.spy(new LogTaskListener(null, null));

		List<String> argsList = null;
		File testLog = null;
		try {
			testLog = new File("testLog");
			testLog.deleteOnExit();
			Mockito.doReturn(new PrintStream(testLog)).when(listener).getLogger();
			ArgumentListBuilder args = new ArgumentListBuilder();
			submitJclBuilder.addArguments(null, workspace, null, listener, args);

			argsList = args.toList();

			assertThat("Expected SubmitJclBuilder.buildArgumentList() to not be empty.", argsList.isEmpty(), is(false));
			assertThat("Expected SubmitJclBuilder.buildArgumentList() to have two entries.", argsList.size(), is(2));

			assertThat(String.format("Expected SubmitJclBuilder.buildArgumentList() to contain key: %s", TopazUtilitiesConstants.JCL),
					argsList.get(0).contains(TopazUtilitiesConstants.JCL), is(true));

			String workspaceRemotePath = workspace.absolutize().getRemote();
			String expectedJclStr = "\"" + workspaceRemotePath + "\\jcl.*(?:.txt)\"";
			Pattern pattern = Pattern.compile(StringUtils.replace(expectedJclStr, "\\", "\\\\"));
			Matcher matcher = pattern.matcher(argsList.get(1));

			assertThat(String.format("Expected SubmitJclBuilder.buildArgumentList() to contain value: \"%s\\jcl<timestamp>.txt\"",
					workspaceRemotePath), matcher.find(), is(true));
		} finally {
			if (listener != null) {
				listener.getLogger().close();
			}

			String trimmedFilePathString = StringUtils.stripStart(argsList.get(1), "\"");
			trimmedFilePathString = StringUtils.stripEnd(trimmedFilePathString, "\"");
			Path filePath = Paths.get(trimmedFilePathString);
			assertThat("Expected SubmitJclBuilder.buildArgumentList() to create a temporary JCL file.", Files.exists(filePath), is(true));

			submitJclBuilder.cleanUp();

			assertThat("Expected SubmitJclBuilder.cleanUp() to delete temporary JCL file.", Files.exists(filePath), is(false));
		}
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclBuilder#cleanUp()}
	 */
	// @Test
	public void testCleanUp() {
		// Test via other test methods.
	}
}