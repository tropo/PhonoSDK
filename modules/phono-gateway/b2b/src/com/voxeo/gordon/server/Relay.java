package com.voxeo.gordon.server;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Relay {
  private String _relayUri;

  public String rtmpUri;

  public String localHost;

  public String localPort;

  public String remoteHost;

  public String remotePort;

  public String playName;

  public String publishName;

  public String payloadType;

  public String codec;
  
  public boolean initialReset = false;

  private boolean allocated = false;

  private static HttpClient _httpClient = new DefaultHttpClient();

  private static final Logger LOG = Logger.getLogger(Relay.class);

  public Relay(String relayURL) {
    // Allocate a new relay
    HttpPost postMethod = new HttpPost(relayURL);
    try {
      HttpResponse response = _httpClient.execute(postMethod);
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        long len = entity.getContentLength();
        if (len != -1 && len < 2048) {
          String entityString = EntityUtils.toString(entity);
          entity.consumeContent();
          // Pull the JSON parameters for the new relay
          JSONObject obj = (JSONObject) JSONValue.parse(entityString);
          _relayUri = obj.get("uri").toString();
          rtmpUri = obj.get("rtmpUri").toString();
          localHost = obj.get("localHost").toString();
          localPort = obj.get("localPort").toString();
          playName = obj.get("playName").toString();
          payloadType = obj.get("payloadType").toString();
          codec = obj.get("codec").toString();
          try {
        	  String initialResetStr = obj.get("initialReset").toString();
        	  if (initialResetStr.contentEquals("true")) {
        		  LOG.debug(">>>>>>>>>>>>>> RTMPD has restarted ---> Terminate all live calls.");
        		  initialReset = true;
        	  }
          } catch (Exception e) {
        	  LOG.debug("Failed to get initialReset from json");
          }
          allocated = true;
        }
      }
    }
    catch (Exception e) {
      LOG.error("Problem allocating relay: " + e);
    }
  }

  public void SetDestination(String remoteHost, Integer remotePort, Integer payloadType) {
    HttpPut putMethod = new HttpPut(_relayUri);
    try {
      JSONObject jsonRequest = new JSONObject();
      jsonRequest.put("remoteHost", remoteHost);
      jsonRequest.put("remotePort", remotePort);
      jsonRequest.put("payloadType", payloadType);
      LOG.info("Requesting relay setDestination:" + jsonRequest.toString());
      StringEntity putEntity = new StringEntity(jsonRequest.toString(), "UTF-8");
      putMethod.setEntity(putEntity);
      putMethod.setHeader("Accept", "application/json");
      putMethod.setHeader("Content-type", "application/json");
      HttpResponse response = _httpClient.execute(putMethod);
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        long len = entity.getContentLength();
        if (len != -1 && len < 2048) {
          String entityString = EntityUtils.toString(entity);
          entity.consumeContent();
          // Pull the JSON parameters for the new relay
          JSONObject obj = (JSONObject) JSONValue.parse(entityString);
          rtmpUri = obj.get("rtmpUri").toString();
          playName = obj.get("playName").toString();
          publishName = obj.get("publishName").toString();
        }
      }
    }
    catch (Exception e) {
      LOG.error("Problem setting relay destination: " + e);
    }
  }

  public void Delete() {
    try {
      if (allocated) {
        HttpDelete deleteMethod = new HttpDelete(_relayUri);
        HttpResponse response = _httpClient.execute(deleteMethod);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          entity.consumeContent();
        }
        allocated = false;
      }
    }
    catch (Exception e) {
      LOG.error("Problem destroying relay: " + e);
    }
  }

  protected void finalize() throws Throwable {
    try {
      this.Delete();
    }
    finally {
      super.finalize();
    }
  }
}
