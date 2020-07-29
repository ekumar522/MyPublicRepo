package com.yourdataconnect.p360;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sforce.soap.enterprise.Connector;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.sobject.Code_List__c;
import com.sforce.soap.enterprise.sobject.Code_Value__c;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sun.misc.BASE64Encoder;

public class Ydc {

//	public static void main(String[] args) throws IOException, ConnectionException {
//		Ydc obj = new Ydc();
//		obj.GetCodeValues();
//		log.info("+++++++++++++++++++++ Integration Completed +++++++++++++++++++++");
//	}

	private final static Logger log = LoggerFactory.getLogger(Ydc.class);

	static Properties config = new Properties ();
	public Ydc(String arg) throws IOException {
		
		InputStream input = new FileInputStream(arg);
		
			config.load(input);
		
		
	}

	public EnterpriseConnection SfConnection() throws ConnectionException {
		ConnectorConfig sfconn = new ConnectorConfig();
		sfconn.setAuthEndpoint(config.getProperty("sf.url") + "/services/Soap/c/48.0/");
		sfconn.setUsername(config.getProperty("sf.username"));
		sfconn.setPassword(config.getProperty("sf.password"));
		EnterpriseConnection connection = Connector.newConnection(sfconn);
		return connection;
	}

	public void GetCodeValues() throws IOException, ConnectionException {
		EnterpriseConnection conn = SfConnection();
		QueryResult Query = conn.query(" Select Name, Description__c From Code_List__c where Status__c='Approved'");
		for (int i = 0; i < Query.getSize(); i++) {
			Code_List__c schemaid = (Code_List__c) Query.getRecords()[i];
			String CodeListName = schemaid.getName();
//			log.info("CodeListName    " + CodeListName);

			String CodeListId = P360.postcodelist(CodeListName);

			QueryResult Query1 = conn.query(
					"Select Name, Description__c,Code__c,Parent__r.Name from Code_Value__c where Code_List__r.Name='"
							+ CodeListName + "' and Status__c='Approved'");
			List<String> codeValue = P360.GetCodeValues(CodeListName);

			for (int j = 0; j < Query1.getSize(); j++) {
				Code_Value__c codevalue = (Code_Value__c) Query1.getRecords()[j];
				String Codevaluename = codevalue.getName();
				String CodevalueDesc = codevalue.getDescription__c();
				String CodevalueCode = codevalue.getCode__c();
				// System.err.println("YDC code values " + Codevaluename + " of code list" + CodeListName);
				if (!codeValue.contains(Codevaluename)) {

					String authString = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
					@SuppressWarnings("restriction")
					String auth = new BASE64Encoder().encode(authString.getBytes());
					OkHttpClient client = new OkHttpClient().newBuilder().build();
					MediaType mediaType = MediaType.parse("application/json");
					//System.err.println(CodeListId+Codevaluename);
					RequestBody body = RequestBody.create(mediaType,
							"{\"entityIdentifier\":\"LookupValueLang\",\"columns\":[{\"identifier\":\"LookupValueLang.Name(9)\"},{\"identifier\":\"LookupValueLang.Description(9)\"},{\"identifier\":\"LookupValue.IsActive\"},{\"identifier\":\"LookupValue.Code(9)\"}],\"rows\":[{\"object\":{\"id\":\"'4'@"
									+ CodeListId + "\"},\"values\":[\"" + Codevaluename + "\",\"" + CodevalueDesc
									+ "\",\"true\",\"" + CodevalueCode + "\"]}]}");
					Request request = new Request.Builder().url("http://18.211.66.86:1512/rest/V1.0/list/LookupValue")
							.method("POST", body).addHeader("Accept", "application/json")
							.addHeader("Authorization", "Basic " + auth).addHeader("Content-Type", "application/json")
							.build();
					Response response = client.newCall(request).execute();
					String Rep = response.body().string();
					
					//log.info(Codevaluename + "   " + Rep);
				} else {
					log.info(Codevaluename + " already exist");
					codeValue.remove(Codevaluename);

				}

			}
			//System.err.println(CodeListName + "             " + codeValue);
			List<String> upserts = new ArrayList<String>();
			if(!codeValue.isEmpty()) {
			for(String a:codeValue) {
		
				String authString = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
				@SuppressWarnings("restriction")
				String auth = new BASE64Encoder().encode(authString.getBytes());
				OkHttpClient client = new OkHttpClient().newBuilder().build();
				Request request = new Request.Builder()
						.url("http://18.211.66.86:1512/rest/V1.0/list/LookupValue/byLookup?lookup=" + CodeListName
								+ "&fields=LookupValue.MIMEValue")
						.method("GET", null).addHeader("Accept", "application/json")
						.addHeader("Authorization", "Basic " + auth).build();
				Response response = client.newCall(request).execute();
				String Reportdata = response.body().string();
				JSONObject res = new JSONObject(Reportdata);
				JSONArray rowArr = res.getJSONArray("rows");
				
				if (rowArr != null) {
					for (int k = 0; k < rowArr.length(); k++) {
						JSONObject r = rowArr.getJSONObject(k);
						JSONObject s = r.getJSONObject("object");
	if( s.getString("label").equals(a)) {
//		System.err.println(s.getString("label")+"   "+s.getString("id"));
		String temp= s.getString("label").replace(" (inactive)","");
	
		upserts.add(temp);
		
		String codevalueid=s.getString("id");
		String authString1 = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
		@SuppressWarnings("restriction")
		String auth1 = new BASE64Encoder().encode(authString1.getBytes());
		OkHttpClient client1 = new OkHttpClient().newBuilder()
				  .build();
				MediaType mediaType = MediaType.parse("application/json");
				RequestBody body = RequestBody.create(mediaType, "{\"entityIdentifier\":\"LookupValue\",\"columns\":[{\"identifier\":\"LookupValue.IsActive\"}],\"rows\":[{\"object\":{\"id\":\""+codevalueid+"\"},\"values\":[\"false\"]}]}");
				Request request1 = new Request.Builder()
				  .url("http://18.211.66.86:1512/rest/V1.0/list/LookupValue")
				  .method("POST", body)
				  .addHeader("Authorization", "Basic "+auth1)
				  .addHeader("Content-Type", "application/json")
				  .build();
				Response response1 = client1.newCall(request1).execute();
				String Rep = response1.body().string();
				
				log.info( Rep);
	}
	
//
//	String x= s.getString("label");
//	 Matcher m = Pattern.compile("\\((.*?)\\)").matcher(x);
//	while(m.find()) {
//	    System.err.println(m.group(1));
//	}
					}
					
				}
			}
		

		}
//			System.err.println(upserts);
//			if(upserts != null) {
//			QueryResult Query2 = conn.query(
//					"Select Name, Description__c,Code__c,Parent__r.Name from Code_Value__c where Code_List__r.Name='"
//							+ CodeListName + "' and Status__c='Approved'");
//
//			
//			for (int l = 0; l < Query2.getSize(); l++) {
//				Code_Value__c codevalu = (Code_Value__c) Query2.getRecords()[l];
//				String Codevalname = codevalu.getName();
//				//System.err.println(upserts+"   finallllllllllll  "+Codevalname);
//				String CodevalDesc = codevalu.getDescription__c();
//				String CodevalCode = codevalu.getCode__c();
//				// log.info("YDC code values " + Codevaluename + " of code list" +
//				// CodeListName);
//				if (upserts.contains(Codevalname)) {
//					System.err.println("finallllllllllll  "+Codevalname);
//					
//					String authString = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
//					@SuppressWarnings("restriction")
//					String auth = new BASE64Encoder().encode(authString.getBytes());
//					OkHttpClient client = new OkHttpClient().newBuilder().build();
//					Request request = new Request.Builder()
//							.url("http://18.211.66.86:1512/rest/V1.0/list/LookupValue/byLookup?lookup=" + CodeListName
//									+ "&fields=LookupValue.MIMEValue")
//							.method("GET", null).addHeader("Accept", "application/json")
//							.addHeader("Authorization", "Basic " + auth).build();
//					Response response = client.newCall(request).execute();
//					String Reportdata = response.body().string();
//					JSONObject res = new JSONObject(Reportdata);
//					JSONArray rowArr = res.getJSONArray("rows");
//					
//					if (rowArr != null) {
//						for (int k = 0; k < rowArr.length(); k++) {
//							JSONObject r = rowArr.getJSONObject(k);
//							JSONObject s = r.getJSONObject("object");
//							String codevalid=s.getString("id");
//							if(Codevalname.equals(s.getString("label")))
//									{
//								System.err.println(Codevalname+s.getString("label"));
//					String authString1 = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
//					@SuppressWarnings("restriction")
//					String auth1 = new BASE64Encoder().encode(authString1.getBytes());
//					OkHttpClient clien = new OkHttpClient().newBuilder()
//							  .build();
//							MediaType mediaType = MediaType.parse("application/json");
//							RequestBody body = RequestBody.create(mediaType, "{\"entityIdentifier\":\"LookupValue\",\"columns\":[{\"identifier\":\"LookupValue.IsActive\"}],\"rows\":[{\"object\":{\"id\":\""+codevalid+"\"},\"values\":[\"true\"]}]}");
//							Request reque = new Request.Builder()
//							  .url("http://18.211.66.86:1512/rest/V1.0/list/LookupValue")
//							  .method("POST", body)
//							  .addHeader("Authorization", "Basic "+auth1)
//							  .addHeader("Content-Type", "application/json")
//							  .build();
//							Response respon = clien.newCall(reque).execute();
//							String Rep = respon.body().string();
//							
//							System.err.println( Rep);
//}
//				}}}
//		
//			
//			
//	}
//		
//
//}
			}
		}
		
}
