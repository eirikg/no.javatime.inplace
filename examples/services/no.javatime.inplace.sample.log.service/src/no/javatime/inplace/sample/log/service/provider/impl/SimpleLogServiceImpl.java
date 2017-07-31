package no.javatime.inplace.sample.log.service.provider.impl;

import no.javatime.inplace.sample.log.service.provider.SimpleLogService;

public class SimpleLogServiceImpl implements SimpleLogService {

	public void log(String message) {
		System.out.println(message);
	}

	@Override
	public void log(int message) {
		System.out.println(message);		
	}
	
}
