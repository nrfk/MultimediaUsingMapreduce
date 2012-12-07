package fr.telecomParistech.parser;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This exception is thrown when size of file exceeds Integer.MAX_VALUE, that's
 * 2^31 - 1 (~2GB)
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class FileTooLargeException extends RuntimeException {
	private static final Logger LOGGER = 
			Logger.getLogger(FileTooLargeException.class.getName());
	private static final long serialVersionUID = -4556272804446718350L;
	long fileSize;
	
	/**
	 * Contructor
	 * @param fileSize the file size
	 */
	public FileTooLargeException(long fileSize) {
		this.fileSize = fileSize;
		LOGGER.setLevel(Level.SEVERE);
	}
	
	@Override
	public void printStackTrace() {
		LOGGER.severe("File too large, file size: " + fileSize);
		super.printStackTrace();
		
	}
}
