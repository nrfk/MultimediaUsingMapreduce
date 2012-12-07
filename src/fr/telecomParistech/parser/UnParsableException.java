package fr.telecomParistech.parser;

import java.util.logging.Logger;

/**
 * This exception is thrown when an parsing error occurs
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class UnParsableException extends Exception {
	private static final long serialVersionUID = -5831700799878987512L;
	private static final Logger LOGGER = 
			Logger.getLogger(UnParsableException.class.getName());
	private final String filePath;
	
	/**
	 * Create an UnparsableException
	 * @param filePath path of the file where exception occurs
	 */
	public UnParsableException(String filePath) {
		this.filePath = filePath;
	}
	
	@Override
	public void printStackTrace() {
		LOGGER.severe("Cannot parse file: " + filePath);
		super.printStackTrace();
		
	}
	
	
}
