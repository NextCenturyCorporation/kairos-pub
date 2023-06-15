package com.nextcentury.kairos.experimentstatus;

import com.nextcentury.kairos.experimentstatus.utils.ExceptionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ShellCmdRunner {
	private static final Logger logger = LogManager.getLogger(ShellCmdRunner.class);
	
	public static List<String> runCommand(String cmdLine) {
		List<String> resultList = null;
		Process process = null;
		try {
			logger.debug("running cmd - " + cmdLine);
			ProcessBuilder pb = new ProcessBuilder();
			pb.redirectInput();
			pb.redirectError();
			pb.redirectOutput();
			process = pb.command("/bin/bash", "-c", cmdLine).start();

			int exitCode = process.waitFor();
			assert exitCode == 0;
			resultList = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
					.collect(Collectors.toList());

			//logger.debug("Returning from sh cmd input as list- ");
			//resultList.stream().forEach(logger::debug);
		} catch (Throwable e) {
			logger.error(ExceptionHelper.getExceptionTrace(e));
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return resultList;
	}
}
