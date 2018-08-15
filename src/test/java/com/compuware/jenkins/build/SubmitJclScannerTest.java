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
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Test cases for {@link SubmitJclScanner}.
 */
@SuppressWarnings("nls")
public class SubmitJclScannerTest {
	// scanner expected values
	/* @formatter:off */
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

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclScanner#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)}.
	 */
	// @Test
	public void testPerform() {
		// Tested via other test methods.
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.SubmitJclScanner#isJcl(java.lang.String)}.
	 */
	@Test
	public void testIsJcl() {
		assertThat(String.format("Expected isJcl() to return true for %s", EXPECTED_JCL), isJcl(EXPECTED_JCL), is(equalTo(true)));
	}

	/**
	 * Utility method for determining if given JCL argument is indeed JCL syntax.
	 * 
	 * @param jcl
	 *            JCL to examine
	 * 
	 * @return TRUE if given JCL argument is JCL syntax
	 * 
	 * @see {@link com.compuware.jenkins.build.SubmitJclScanner#isJcl()}
	 */
	private boolean isJcl(String jcl) {
		boolean isJcl = false;
		try {
			SubmitJclBuilder builder = new SubmitJclBuilder("", "", "", "");
			SubmitJclScanner scanner = new SubmitJclScanner(builder);

			Method method = SubmitJclScanner.class.getDeclaredMethod("isJcl", new Class[] { String.class });
			method.setAccessible(true);
			isJcl = (Boolean) method.invoke(scanner, new Object[] { jcl });
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}

		return isJcl;
	}
}