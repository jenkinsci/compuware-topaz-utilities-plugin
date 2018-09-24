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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.compuware.jenkins.build.SubmitJclMemberBuilder.DescriptorImpl;
import com.compuware.jenkins.build.utils.TopazUtilitiesConstants;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.LogTaskListener;

/**
 * Test cases for {@link SubmitJclMemberBuilder}.
 */
@SuppressWarnings("nls")
public class SubmitJclMemberBuilderTest {

	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_CONNECTION_ID = "12345";
	private static final String EXPECTED_CREDENTIALS_ID = "67890";
	private static final String EXPECTED_MAX_CONDITION_CODE = "4";
	private static final String EXPECTED_JCL_MEMBERS = 
			"A.B.MYJCL\n" + 
			"A.B.MYJCL2\n" + 
			"MYJCL(JCLMEM3)";
	private static final String EXPECTED_JCL_MEMBERS_CMD_ARG = "\"A.B.MYJCL,A.B.MYJCL2,MYJCL(JCLMEM3)\"";
	/* @formatter:on*/

	public @Rule JenkinsRule rule = new JenkinsRule();

	/**
	 * Test method for
	 * {@link com.compuware.jenkins.build.SubmitJclMemberBuilder#SubmitJclMemberBuilder(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
	 */
	@Test
	public void SubmitJclMemberBuilder() {
		SubmitJclMemberBuilder builder = new SubmitJclMemberBuilder(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID,
				EXPECTED_MAX_CONDITION_CODE, EXPECTED_JCL_MEMBERS);

		assertThat(String.format("Expected SubmitJclMemberBuilder.getConnectionId() to return %s", EXPECTED_CONNECTION_ID),
				builder.getConnectionId(), is(equalTo(EXPECTED_CONNECTION_ID)));

		assertThat(String.format("Expected SubmitJclMemberBuilder.getCredentialsId() to return %s", EXPECTED_CREDENTIALS_ID),
				builder.getCredentialsId(), is(equalTo(EXPECTED_CREDENTIALS_ID)));

		assertThat(String.format("Expected SubmitJclMemberBuilder.getMaxConditionCode() to return %s", EXPECTED_MAX_CONDITION_CODE),
				builder.getMaxConditionCode(), is(equalTo(EXPECTED_MAX_CONDITION_CODE)));

		assertThat(String.format("Expected SubmitJclMemberBuilder.getJclMember() to return %s", EXPECTED_JCL_MEMBERS),
				builder.getJclMember(), is(equalTo(EXPECTED_JCL_MEMBERS)));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclMemberBuilder#getDescriptor()}
	 */
	@Test
	public void testGetDescriptor() {
		DescriptorImpl descriptor = new DescriptorImpl();

		assertThat("Expected SubmitJclMemberBuilder.DescriptorImpl.getDisplayName() to not be null.", descriptor.getDisplayName(),
				is(notNullValue()));
		assertThat("Expected SubmitJclMemberBuilder.DescriptorImpl.getDisplayName() to not be empty.",
				descriptor.getDisplayName().isEmpty(), is(false));
		assertEquals("Topaz submit JCL members", descriptor.getDisplayName());
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclMemberBuilder.JclDescriptorImpl#doCheckJcl(java.lang.String)}.
	 */
	@Test
	public void testDoCheckJcl() {
		DescriptorImpl descriptor = new DescriptorImpl();

		assertEquals(Messages.checkJclMemberError(), descriptor.doCheckJclMember(null).getMessage());
		assertEquals(Messages.checkJclMemberError(), descriptor.doCheckJclMember(StringUtils.EMPTY).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckJclMember(EXPECTED_JCL_MEMBERS));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclMemberBuilder#getJcl()}
	 */
	// @Test
	public void testGetJcl() {
		// Test via other test methods.
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclMemberBuilder#buildArgumentList(Run<?, ?>, FilePath, Launcher,
	 * TaskListener)}
	 */
	@Test
	public void testBuildArgumentList() throws IOException, InterruptedException {
		SubmitJclMemberBuilder submitJclMemberBuilder = Mockito
				.spy(new SubmitJclMemberBuilder("connectionId", "credentialsId", "4", EXPECTED_JCL_MEMBERS));
		FilePath workspace = new FilePath((VirtualChannel) null, "");
		TaskListener listener = Mockito.spy(new LogTaskListener(null, null));

		File testLog = null;
		try {
			testLog = new File("testLog");
			testLog.deleteOnExit();
			Mockito.doReturn(new PrintStream(testLog)).when(listener).getLogger();

			ArgumentListBuilder args = new ArgumentListBuilder();
			submitJclMemberBuilder.addArguments(null, workspace, null, listener, args);

			assertThat("Expected submitJclMemberBuilder.buildArgumentList() to not be null.", args, is(notNullValue()));

			List<String> argsList = args.toList();

			assertThat("Expected submitJclMemberBuilder.buildArgumentList() to not be empty.", argsList.isEmpty(), is(false));
			assertThat("Expected submitJclMemberBuilder.buildArgumentList() to have two entries.", argsList.size(), is(2));

			assertThat(String.format("Expected submitJclMemberBuilder.buildArgumentList() to contain key: %s",
					TopazUtilitiesConstants.JCL_DSNS), argsList.get(0).contains(TopazUtilitiesConstants.JCL_DSNS), is(true));
			assertThat(
					String.format("Expected submitJclMemberBuilder.buildArgumentList() to contain value: %s", EXPECTED_JCL_MEMBERS_CMD_ARG),
					argsList.get(1).contains(EXPECTED_JCL_MEMBERS_CMD_ARG), is(true));
		} finally {
			if (listener != null) {
				listener.getLogger().close();
			}
		}
	}

}