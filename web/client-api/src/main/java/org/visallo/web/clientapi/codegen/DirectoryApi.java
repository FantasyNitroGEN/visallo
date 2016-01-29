package org.visallo.web.clientapi.codegen;

import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.ApiInvoker;

import org.visallo.web.clientapi.model.DirectoryEntity;
import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class DirectoryApi {
  protected String basePath = "http://visallo-dev:8889";
  protected ApiInvoker apiInvoker = ApiInvoker.getInstance();

  public ApiInvoker getInvoker() {
    return apiInvoker;
  }
  
  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }
  
  public String getBasePath() {
    return basePath;
  }

  public DirectoryEntity get (String id) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(id == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/directory/get".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(id)))
      queryParams.put("id", String.valueOf(id));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (DirectoryEntity) ApiInvoker.deserialize(response, "", DirectoryEntity.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  public DirectoryEntity search (String search, Boolean people, Boolean groups) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(search == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/directory/search".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(search)))
      queryParams.put("search", String.valueOf(search));
    if(!"null".equals(String.valueOf(people)))
      queryParams.put("people", String.valueOf(people));
    if(!"null".equals(String.valueOf(groups)))
      queryParams.put("groups", String.valueOf(groups));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (DirectoryEntity) ApiInvoker.deserialize(response, "", DirectoryEntity.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  }

