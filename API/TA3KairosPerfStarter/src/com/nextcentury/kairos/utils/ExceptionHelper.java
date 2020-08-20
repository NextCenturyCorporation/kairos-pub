package com.nextcentury.kairos.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Captures an exception stack into a string
 * 
 * @author kdeshpande
 *
 */
public class ExceptionHelper {

	public static String getExceptionTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
