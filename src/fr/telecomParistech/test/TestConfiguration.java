package fr.telecomParistech.test;

import java.util.logging.Level;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;






public class TestConfiguration {

	/**
	 * @param args
	 * @throws ConfigurationException 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		dash-mapreduce-config.xml
		Configuration config = new XMLConfiguration("war/WEB-INF/dash-mapreduce-config.xml");
		System.out.println(config.getInt("mapreduce.map-task"));
		
		Level l = Level.parse("SEVERE");
		
		System.out.println(l == Level.SEVERE);
	}
}
