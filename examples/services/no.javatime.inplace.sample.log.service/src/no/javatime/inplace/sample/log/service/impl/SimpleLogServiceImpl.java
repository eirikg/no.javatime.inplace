package no.javatime.inplace.sample.log.service.impl;

import no.javatime.inplace.sample.log.service.SimpleLogService;

public class SimpleLogServiceImpl implements SimpleLogService {

	public void log(String message) {
		System.out.println(message);
	}
	
}
