package org.matthiaszimmermann.bitcoin.pwg;

public class BaseTest {
	
	private boolean silent = false;

	protected void log(String format, Object... args) {
		log(String.format(format, args));
	}
	
	protected void log(String message) {
		if(!silent) {
			System.out.println(message);
		}
	}
}
