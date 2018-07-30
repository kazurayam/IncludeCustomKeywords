import org.junit.runner.JUnitCore
import org.junit.runner.Result
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import com.kazurayam.ksbackyard.codesharing.test.KeywordPortabilityTest

/**
 * 
 * @author kazurayam
 *
 */

// force compile Keywords
WebUI.callTestCase(findTestCase('test/Prologue'), [:],
	FailureHandling.CONTINUE_ON_FAILURE)

// run DownloaderTest
Result result = JUnitCore.runClasses(KeywordPortabilityTest.class)

// print test result
WebUI.callTestCase(findTestCase('test/Epilogue'), ['result':result],
	FailureHandling.CONTINUE_ON_FAILURE)
