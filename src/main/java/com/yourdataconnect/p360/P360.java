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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import sun.misc.BASE64Encoder;

public class P360 {

	private final static Logger log = LoggerFactory.getLogger(P360.class);

	static Properties config = new Properties ();
	public P360(String arg) throws IOException {
		
		InputStream input = new FileInputStream(arg);
		
			config.load(input);
	}
	public static String postcodelist(String CodeList) throws IOException {
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse("application/json");
		String authString2 = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
		@SuppressWarnings("restriction")
		String auth = new BASE64Encoder().encode(authString2.getBytes());
		RequestBody body = RequestBody.create(mediaType,
				"{\"entityIdentifier\":\"Lookup\",\"columns\": [{\"identifier\":\"Lookup.Identifier\" },{\"identifier\":\"LookupLang.Name(9)\" }],\"rows\":[{\"object\":{ \"id\": \"'"
						+ CodeList + "'\" },\"values\":[\"" + CodeList + "\",\"" + CodeList + "\"]}]}");
		Request request = new Request.Builder()

				.url(config.getProperty("p360.url") + "/rest/V1.0/list/Lookup").post(body)
				.addHeader("authorization", "Basic " + auth).addHeader("content-type", "application/json")
				.addHeader("cache-control", "no-cache").build();
		Response response = client.newCall(request).execute();
		String Reportdata1 = response.body().string();
		JSONObject res = new JSONObject(Reportdata1);
		String js = res.getJSONArray("objects").getJSONObject(0).getJSONObject("object").getString("id");
		return js;

	}

	public static List<String> GetCodeValues(String CodeList) throws IOException {
		String lookup = null;
		String authString = config.getProperty("p360.username") + ":" + config.getProperty("p360.password");
		@SuppressWarnings("restriction")
		String auth = new BASE64Encoder().encode(authString.getBytes());
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder()
				.url("http://18.211.66.86:1512/rest/V1.0/list/LookupValue/byLookup?lookup=" + CodeList
						+ "&fields=LookupValue.MIMEValue")
				.method("GET", null).addHeader("Accept", "application/json").addHeader("Authorization", "Basic " + auth)
				.build();
		Response response = client.newCall(request).execute();
		String Reportdata = response.body().string();
		JSONObject res = new JSONObject(Reportdata);
		JSONArray rowArr = res.getJSONArray("rows");
		List<String> valuesfromp = new ArrayList<String>();
		if (rowArr != null) {
			for (int i = 0; i < rowArr.length(); i++) {
				JSONObject r = rowArr.getJSONObject(i);
				JSONObject s = r.getJSONObject("object");
				lookup = s.getString("label");
				valuesfromp.add(lookup);
			}
		}
		log.info("P360 codelist:  " + CodeList + "    " + valuesfromp);

		return valuesfromp;
	}

}
