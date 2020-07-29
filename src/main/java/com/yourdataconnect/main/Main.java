package com.yourdataconnect.main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sforce.ws.ConnectionException;
import com.yourdataconnect.p360.P360;
import com.yourdataconnect.p360.Ydc;



public class Main {
	private final static Logger log = LoggerFactory.getLogger(Main.class);
	public static void main(String[] args) throws IOException, ConnectionException {
		
		Properties config = new Properties ();
		InputStream input = new FileInputStream(args[0]);
		
			config.load(input);
		
			Ydc obj = new Ydc(args[0]);
			P360 o = new P360(args[0]);
			obj.GetCodeValues();
			log.info("+++++++++++++++++++++ Integration Completed +++++++++++++++++++++");
				

	}	

}
