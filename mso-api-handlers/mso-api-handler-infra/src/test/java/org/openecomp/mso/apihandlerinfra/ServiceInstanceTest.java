/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.mso.apihandlerinfra;


import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.Response;
import mockit.Mock;
import mockit.MockUp;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openecomp.mso.apihandler.common.CamundaClient;
import org.openecomp.mso.apihandler.common.RequestClient;
import org.openecomp.mso.apihandler.common.RequestClientFactory;
import org.openecomp.mso.apihandler.common.RequestClientParamater;
import org.openecomp.mso.db.catalog.CatalogDatabase;
import org.openecomp.mso.db.catalog.beans.Service;
import org.openecomp.mso.db.catalog.beans.ServiceRecipe;
import org.openecomp.mso.db.catalog.beans.VfModule;
import org.openecomp.mso.db.catalog.beans.VfModuleCustomization;
import org.openecomp.mso.db.catalog.beans.VnfComponentsRecipe;
import org.openecomp.mso.db.catalog.beans.VnfRecipe;
import org.openecomp.mso.db.catalog.beans.VnfResource;
import org.openecomp.mso.properties.MsoJavaProperties;
import org.openecomp.mso.properties.MsoPropertiesFactory;
import org.openecomp.mso.requestsdb.InfraActiveRequests;
import org.openecomp.mso.requestsdb.RequestsDatabase;

public class ServiceInstanceTest {

	/*** Create Service Instance Test Cases ***/

	@BeforeClass
	public static void setUp() throws Exception {
		MsoPropertiesFactory msoPropertiesFactory = new MsoPropertiesFactory();
		msoPropertiesFactory.removeAllMsoProperties();
		msoPropertiesFactory.initializeMsoProperties(Constants.MSO_PROP_APIHANDLER_INFRA, "src/test/resources/mso.apihandler-infra.properties");
	}


	@Test
	public void createServiceInstanceInvalidModelInfo(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v5");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid model-info is specified"));
	}
	
	@Test
	public void createServiceInstanceNormalDuplicate(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return new InfraActiveRequests();
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains(
            "Locked instance - This service (testService) already has a request being worked with a status of null (RequestId - null). The existing request must finish or be cleaned up before proceeding."));
	}
	
	@Test
	public void createServiceInstanceTestDBException(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return null;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public Service getServiceByModelName (String defaultServiceModelName) {
            	Service serviceRecord = new Service();
            	serviceRecord.setModelUUID("2883992993");
            	return serviceRecord;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
            	ServiceRecipe recipe =new ServiceRecipe();
            	recipe.setOrchestrationUri("/test/mso");
            	recipe.setRecipeTimeout(1000);
            	return recipe;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Exception while creating record in DB") && respBody.contains("NullPointerException"));
	}
	
	@Ignore // 1802 merge
	@Test
	public void createServiceInstanceTestBpmnFail(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return null;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public Service getServiceByModelName (String defaultServiceModelName) {
            	Service serviceRecord = new Service();
            	serviceRecord.setModelUUID("2883992993");
            	return serviceRecord;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
            	ServiceRecipe recipe =new ServiceRecipe();
            	recipe.setOrchestrationUri("/test/mso");
            	recipe.setRecipeTimeout(1000);
            	return recipe;
            }
        };
        
        new MockUp<MsoRequest>() {
            @Mock
            public void createRequestRecord (Status status, Action action) {
            	return;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Failed calling bpmn properties"));  // was: Request Failed due to BPEL error
	}
	
	@Test(expected = Exception.class)
	public void createServiceInstanceTest200Http(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return null;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public Service getServiceByModelName (String defaultServiceModelName) {
            	Service serviceRecord = new Service();
            	serviceRecord.setModelUUID("2883992993");
            	return serviceRecord;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
            	ServiceRecipe recipe =new ServiceRecipe();
            	recipe.setOrchestrationUri("/test/mso");
            	recipe.setRecipeTimeout(1000);
            	return recipe;
            }
        };
        
        new MockUp<MsoRequest>() {
            @Mock
            public void createRequestRecord (Status status, Action action) {
            	return;
            }
        };
        
        new MockUp<RequestClientFactory>() {
            @Mock
            public RequestClient getRequestClient(String orchestrationURI, MsoJavaProperties props) throws IllegalStateException{
            	RequestClient client = new CamundaClient();
            	client.setUrl("/test/url");
            	return client;
            }
        };
        
        new MockUp<CamundaClient>() {
            @Mock
            public HttpResponse post(RequestClientParamater requestClientParamater) {
            	ProtocolVersion pv = new ProtocolVersion("HTTP",1,1);
            	HttpResponse resp = new BasicHttpResponse(pv,200, "test response");
            	BasicHttpEntity entity = new BasicHttpEntity();
            	String body = "{\"content\":\"success\",\"message\":\"success\"}";
            	InputStream instream = new ByteArrayInputStream(body.getBytes());
            	entity.setContent(instream);
            	resp.setEntity(entity);
            	return resp;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
	}
	
	@Test
	public void createServiceInstanceTest500Http(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return null;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public Service getServiceByModelName (String defaultServiceModelName) {
            	Service serviceRecord = new Service();
            	serviceRecord.setModelUUID("2883992993");
            	return serviceRecord;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
            	ServiceRecipe recipe =new ServiceRecipe();
            	recipe.setOrchestrationUri("/test/mso");
            	recipe.setRecipeTimeout(1000);
            	return recipe;
            }
        };
        
        new MockUp<MsoRequest>() {
            @Mock
            public void createRequestRecord (Status status, Action action) {
            	return;
            }
        };
        
        new MockUp<RequestClientFactory>() {
            @Mock
            public RequestClient getRequestClient(String orchestrationURI, MsoJavaProperties props) throws IllegalStateException{
            	RequestClient client = new CamundaClient();
            	client.setUrl("/test/url");
            	return client;
            }
        };
        
        new MockUp<CamundaClient>() {
            @Mock
            public HttpResponse post(RequestClientParamater requestClientParamater) {
            	ProtocolVersion pv = new ProtocolVersion("HTTP",1,1);
            	HttpResponse resp = new BasicHttpResponse(pv,500, "test response");
            	BasicHttpEntity entity = new BasicHttpEntity();
            	String body = "{\"content\":\"success\",\"message\":\"success\"}";
            	InputStream instream = new ByteArrayInputStream(body.getBytes());
            	entity.setContent(instream);
            	resp.setEntity(entity);
            	return resp;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Request Failed due to BPEL error with HTTP Status"));
	}
	
	@Test
	public void createServiceInstanceTestVnfModelType(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return null;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public Service getServiceByModelName (String defaultServiceModelName) {
            	Service serviceRecord = new Service();
            	serviceRecord.setModelUUID("2883992993");
            	return serviceRecord;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
            	ServiceRecipe recipe =new ServiceRecipe();
            	recipe.setOrchestrationUri("/test/mso");
            	recipe.setRecipeTimeout(1000);
            	return recipe;
            }
        };
        
        new MockUp<MsoRequest>() {
            @Mock
            public void createRequestRecord (Status status, Action action) {
            	return;
            }
        };
        
        new MockUp<RequestClientFactory>() {
            @Mock
            public RequestClient getRequestClient(String orchestrationURI, MsoJavaProperties props) throws IllegalStateException{
            	RequestClient client = new CamundaClient();
            	client.setUrl("/test/url");
            	return client;
            }
        };
        
        new MockUp<CamundaClient>() {
            @Mock
            public HttpResponse post(RequestClientParamater requestClientParamater) {
            	ProtocolVersion pv = new ProtocolVersion("HTTP",1,1);
            	HttpResponse resp = new BasicHttpResponse(pv,500, "test response");
            	BasicHttpEntity entity = new BasicHttpEntity();
            	String body = "{\"content\":\"success\",\"message\":\"success\"}";
            	InputStream instream = new ByteArrayInputStream(body.getBytes());
            	entity.setContent(instream);
            	resp.setEntity(entity);
            	return resp;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"vnf\",\"modelName\":\"serviceModel\",\"modelCustomizationName\":\"test\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v5");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("No valid modelVersionId is specified"));
	}
	
	@Test
	public void createServiceInstanceTestNullHttpResp(){
		new MockUp<RequestsDatabase>() {
            @Mock
            public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
                return null;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public Service getServiceByModelName (String defaultServiceModelName) {
            	Service serviceRecord = new Service();
            	serviceRecord.setModelUUID("2883992993");
            	return serviceRecord;
            }
        };
        new MockUp<CatalogDatabase>() {
            @Mock
            public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
            	ServiceRecipe recipe =new ServiceRecipe();
            	recipe.setOrchestrationUri("/test/mso");
            	recipe.setRecipeTimeout(1000);
            	return recipe;
            }
        };
        
        new MockUp<MsoRequest>() {
            @Mock
            public void createRequestRecord (Status status, Action action) {
            	return;
            }
        };
        
        new MockUp<RequestClientFactory>() {
            @Mock
            public RequestClient getRequestClient(String orchestrationURI, MsoJavaProperties props) throws IllegalStateException{
            	RequestClient client = new CamundaClient();
            	client.setUrl("/test/url");
            	return client;
            }
        };
        
        new MockUp<CamundaClient>() {
            @Mock
            public HttpResponse post(RequestClientParamater requestClientParamater){
            	return null;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("bpelResponse is null"));
	}
	
	@Test
	public void createServiceInstanceNormalNullDBFatch(){
		new MockUp<RequestsDatabase>() {
            @Mock
            private List<InfraActiveRequests> executeInfraQuery (List <Criterion> criteria, Order order) {
                return Collections.EMPTY_LIST;
            }
        };
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Recipe could not be retrieved from catalog DB null"));
	}
	
	
	@Test
	public void createServiceInstanceInvalidModelVersionId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v5");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid modelVersionId is specified"));
	}
	
	@Ignore // 1802 merge
	@Test
	public void createServiceInstanceNullInstanceName(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid instanceName is specified"));
	}
	
	
	@Test
	public void createServiceInstanceNullModelInfo(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid model-info is specified"));
	}
	
	@Test
	public void createServiceInstanceInvalidModelInvariantId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false},\"modelInfo\":{\"modelInvariantId\": \"1234\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid modelType is specified"));
	}
	
	@Test
	public void createServiceInstanceNullModelVersion(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid modelType is specified"));
	}
	
	
	@Test
	public void createServiceInstanceNullModelType(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid modelType is specified"));
	}
	
	@Test
	public void createServiceInstanceInvalidModelType(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"testmodel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Mapping of request to JSON object failed."));
	}
	
	@Ignore // 1802 merge
	@Test
	public void createServiceInstanceNullModelName(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\":\"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid modelName is specified"));
	}
	
	@Ignore // 1802 merge
	@Test
	public void createServiceInstanceInvalidVersionForAutoBuildVfModules(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": true,\"subscriptionServiceType\":\"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("AutoBuildVfModule is not valid in the v2 version"));
	}
	
	@Test
	public void createServiceInstanceNullRequestParameter(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid subscriptionServiceType is specified"));
	}
	
	@Test
	public void createServiceInstanceNullSubscriptionType(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\"},\"requestParameters\": { \"autoBuildVfModules\": false},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respBody = resp.getEntity().toString();
		assertTrue(respBody.contains("Error parsing request.") && respBody.contains("No valid subscriptionServiceType is specified"));
	}
	
	@Test
	public void createServiceInstanceAnbormalInvalidJson(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"name\":\"test\"}";
		Response resp = instance.createServiceInstance(requestJson, "v2");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Mapping of request to JSON object failed"));
	}
	
	/*** Activate Service Instance Test Cases ***/
	
	@Test
	public void activateServiceInstanceAnbormalInvalidJson(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"name\":\"test\"}";
		Response resp = instance.activateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Mapping of request to JSON object failed"));
	}
	
	@Test
	public void activateServiceInstanceInvalidModelVersionId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d37\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.activateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid modelVersionId in relatedInstance is specified"));
	}
	
	@Test
	public void activateServiceInstanceInvalidServiceInstanceId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d37\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.activateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid serviceInstanceId matching the serviceInstanceId in request URI is specified"));
	}

	@Test
	public void activateServiceInstanceTestNormal(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.activateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("SVC2000"));
	}
	
	/*** Deactivate Service Instance Test Cases ***/
	
	@Test
	public void deactivateServiceInstanceAnbormalInvalidJson(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"name\":\"test\"}";
		Response resp = instance.deactivateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Mapping of request to JSON object failed"));
	}
	
	@Test
	public void deactivateServiceInstanceInvalidModelVersionId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d37\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.deactivateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid modelVersionId in relatedInstance is specified"));
	}
	
	@Test
	public void deactivateServiceInstanceInvalidServiceInstanceId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d37\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.deactivateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid serviceInstanceId matching the serviceInstanceId in request URI is specified"));
	}
	
	@Test
	public void deactivateServiceInstanceTestNormal(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.deactivateServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("SVC2000"));
	}
	
	/*** Delete Service Instance Test Cases ***/
	
	@Test
	public void deleteServiceInstanceAnbormalInvalidJson(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"name\":\"test\"}";
		Response resp = instance.deleteServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Mapping of request to JSON object failed"));
	}
	
	@Test
	public void deleteServiceInstanceInvalidModelVersionId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d37\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.deleteServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid modelVersionId is specified"));
	}
	
	@Test
	public void deleteServiceInstanceInvalidServiceInstanceId(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d37\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\"}}}";
		Response resp = instance.deleteServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid modelVersionId is specified"));
	}
	
	@Test
	public void deleteServiceInstanceTestNormal(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.deleteServiceInstance(requestJson, "v5","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("SVC2000"));
	}
	
	/*** Create Vnf Instance Test Cases ***/
	
	@Ignore // 1802 merge
	@Test
	public void createVNFInstanceTestInvalidCloudConfiguration(){
		ServiceInstances instance = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.createVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid cloudConfiguration is specified"));
	}
	
	@Test
	public void createVNFInstanceTestInvalidIcpCloudRegionId(){
		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"cloudConfiguration\":{}, \"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.createVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid lcpCloudRegionId is specified"));
	}
	
	@Test
	public void createVNFInstanceTestInvalidTenantId(){
		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"cloudConfiguration\":{\"lcpCloudRegionId\":\"2993841\"}, \"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.createVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("Error parsing request.") && respStr.contains("No valid tenantId is specified"));
	}
	
	@Test
	public void createVNFInstanceTestNormal(){

		new MockUp<RequestsDatabase>() {
			@Mock
			public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
				return null;
			}
		};

		new MockUp<RequestsDatabase>() {
			@Mock
			public int updateInfraStatus (String requestId, String requestStatus, long progress, String lastModifiedBy) {
				return 1;
			}
		};

		new MockUp<MsoRequest>() {
			@Mock
			public void createRequestRecord (Status status, Action action) {
				return;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public Service getServiceByModelName (String defaultServiceModelName) {
				Service serviceRecord = new Service();
				serviceRecord.setModelUUID("2883992993");
				return serviceRecord;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
				ServiceRecipe recipe =new ServiceRecipe();
				recipe.setOrchestrationUri("/test/mso");
				recipe.setRecipeTimeout(1000);
				return recipe;
			}
		};
		new MockUp<RequestClientFactory>() {
			@Mock
			public RequestClient getRequestClient(String orchestrationURI, MsoJavaProperties props) throws IllegalStateException{
				RequestClient client = new CamundaClient();
				client.setUrl("/test/url");
				return client;
			}
		};
		new MockUp<CatalogDatabase>() {
			@Mock
			public VnfResource getVnfResourceByModelCustomizationId(String modelCustomizationId) {
				VnfResource vnfResource = new VnfResource();
				return vnfResource;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public VnfRecipe getVnfRecipe (String vnfType, String action) {
				VnfRecipe recipe =new VnfRecipe();
				recipe.setOrchestrationUri("/test/mso");
				recipe.setRecipeTimeout(1000);
				return recipe;
			}
		};


		new MockUp<CamundaClient>() {
			@Mock
			public HttpResponse post(RequestClientParamater requestClientParamater) {
				ProtocolVersion pv = new ProtocolVersion("HTTP",1,1);
				HttpResponse resp = new BasicHttpResponse(pv,200, "test response");
				BasicHttpEntity entity = new BasicHttpEntity();

				final String body = "{\"content\":\"success\",\"message\":\"success\"}";
				InputStream instream = new ByteArrayInputStream(body.getBytes());
				entity.setContent(instream);
				resp.setEntity(entity);
				return resp;
			}
		};

		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"1882939\",\"vnfInstanceId\":\"1882938\"," +
				"\"networkInstanceId\":\"1882937\",\"volumeGroupInstanceId\":\"1882935\",\"vfModuleInstanceId\":\"1882934\"," +
				"\"requestDetails\":{\"cloudConfiguration\":{\"lcpCloudRegionId\":\"2993841\",\"tenantId\":\"2910032\"}," +
				"\"relatedInstanceList\":[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\"," +
				"\"modelInfo\":{\"modelInvariantId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\":{\"source\":\"VID\",\"requestorId\":\"zz9999\",\"instanceName\":\"testService\",\"productFamilyId\":\"productFamilyId1\"}," +
				"\"requestParameters\":{\"autoBuildVfModules\":false,\"subscriptionServiceType\":\"test\",\"aLaCarte\":false},\"modelInfo\":{\"modelInvariantId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"vnf\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\"}}}";
		Response resp = instance.createVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.equals("success"));
	}
	
	/*** Replace Vnf Instance Test Cases ***/
	@Test
	public void replaceVNFInstanceTestNormal(){
		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"cloudConfiguration\":{\"lcpCloudRegionId\":\"2993841\",\"tenantId\":\"2910032\"}, \"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.replaceVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34","557ea944-c83e-43cf-9ed7-3a354abd6d93");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("SVC2000"));
	}
	
	/*** Update Vnf Instance Test Cases ***/
	
	@Test
	public void updateVNFInstanceTestNormal(){
		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"cloudConfiguration\":{\"lcpCloudRegionId\":\"2993841\",\"tenantId\":\"2910032\"}, \"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.updateVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34","557ea944-c83e-43cf-9ed7-3a354abd6d93");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("SVC2000"));
	}
	
	/*** Update Vnf Instance Test Cases ***/

	@Test
	public void deleteVNFInstanceTestNormal(){
		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"1882939\","
				+"\"vnfInstanceId\":\"1882938\","
				+"\"networkInstanceId\":\"1882937\","
				+"\"volumeGroupInstanceId\":\"1882935\","
				+"\"vfModuleInstanceId\":\"1882934\","
				+ "\"requestDetails\": {\"cloudConfiguration\":{\"lcpCloudRegionId\":\"2993841\",\"tenantId\":\"2910032\"}, \"relatedInstanceList\" :[{\"relatedInstance\":{\"instanceName\":\"testInstance\",\"instanceId\":\"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"4839499\"}}}],\"requestInfo\": { \"source\": \"VID\", \"requestorId\": \"zz9999\",\"instanceName\": \"testService\"},\"requestParameters\": { \"autoBuildVfModules\": false,\"subscriptionServiceType\": \"test\"},\"modelInfo\":{\"modelInvariantId\": \"557ea944-c83e-43cf-9ed7-3a354abd6d34\",\"modelVersion\":\"v2\",\"modelType\":\"service\",\"modelName\":\"serviceModel\",\"modelVersionId\":\"288393\",\"modelCustomizationId\":\"389823213\"}}}";
		Response resp = instance.deleteVnfInstance(requestJson, "v3","557ea944-c83e-43cf-9ed7-3a354abd6d34","557ea944-c83e-43cf-9ed7-3a354abd6d93");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.contains("SVC2000"));
	}

	@Test
	public void createVFModuleTestNormal(){

		new MockUp<RequestsDatabase>() {
			@Mock
			public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
				return null;
			}
		};

		new MockUp<RequestsDatabase>() {
			@Mock
			public int updateInfraStatus (String requestId, String requestStatus, long progress, String lastModifiedBy) {
				return 1;
			}
		};

		new MockUp<MsoRequest>() {
			@Mock
			public void createRequestRecord (Status status, Action action) {
				return;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public Service getServiceByModelName (String defaultServiceModelName) {
				Service serviceRecord = new Service();
				serviceRecord.setModelUUID("2883992993");
				return serviceRecord;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public ServiceRecipe getServiceRecipeByModelUUID (String uuid,String action) {
				ServiceRecipe recipe =new ServiceRecipe();
				recipe.setOrchestrationUri("/test/mso");
				recipe.setRecipeTimeout(1000);
				return recipe;
			}
		};
		new MockUp<RequestClientFactory>() {
			@Mock
			public RequestClient getRequestClient(String orchestrationURI, MsoJavaProperties props) throws IllegalStateException{
				RequestClient client = new CamundaClient();
				client.setUrl("/test/url");
				return client;
			}
		};
		new MockUp<CatalogDatabase>() {
			@Mock
			public VnfResource getVnfResourceByModelCustomizationId(String modelCustomizationId) {
				VnfResource vnfResource = new VnfResource();
				return vnfResource;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public VnfComponentsRecipe getVnfComponentsRecipeByVfModuleModelUUId (String vfModuleModelUUId, String vnfComponentType, String action) {
				VnfComponentsRecipe recipe =new VnfComponentsRecipe();
				recipe.setOrchestrationUri("/test/mso");
				recipe.setRecipeTimeout(1000);
				return recipe;
			}
		};
		new MockUp<CatalogDatabase>() {
			@Mock
			public VfModule getVfModuleByModelUuid(String modelUuid) {
				VfModule vfModule =new VfModule();
				return vfModule;
			}
		};

		new MockUp<CatalogDatabase>() {
			@Mock
			public VfModuleCustomization getVfModuleCustomizationByModelCustomizationId(String modelCustomizationUuid) {
				VfModuleCustomization vfModuleCustomization =new VfModuleCustomization();
				final VfModule vfModule = new VfModule();
				vfModule.setModelUUID("296e278c-bfa8-496e-b59e-fb1fe715f726");
				vfModuleCustomization.setVfModule(vfModule);
				return vfModuleCustomization;
			}
		};


		new MockUp<CamundaClient>() {
			@Mock
			public HttpResponse post(RequestClientParamater requestClientParamater){
				ProtocolVersion pv = new ProtocolVersion("HTTP",1,1);
				HttpResponse resp = new BasicHttpResponse(pv,200, "test response");
				BasicHttpEntity entity = new BasicHttpEntity();

				final String body = "{\"content\":\"success\",\"message\":\"success\"}";
				InputStream instream = new ByteArrayInputStream(body.getBytes());
				entity.setContent(instream);
				resp.setEntity(entity);
				return resp;
			}
		};

		ServiceInstances instance = new ServiceInstances();
		String s = "\"cloudConfiguration\":{}";
		String requestJson = "{\"serviceInstanceId\":\"43b34d6d-1ab2-4c7a-a3a0-5471306550c5\",\"vnfInstanceId\":\"7b1ead4f-ea06-45c6-921e-124061e5eae7\",\"networkInstanceId\":\"1882937\",\"volumeGroupInstanceId\":\"1882935\",\"vfModuleInstanceId\":\"1882934\",\"requestDetails\":{\"requestInfo\":{\"instanceName\":\"vf-inst\",\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"123123\"},\"modelInfo\":{\"modelType\":\"vfModule\",\"modelInvariantId\":\"dde10afa-c732-4f0f-8501-2d2e01ea46ef\",\"modelVersionId\":\"296e278c-bfa8-496e-b59e-fb1fe715f726\",\"modelName\":\"CarrierTosca0::module-1\",\"modelCustomizationId\":\"ce0fdd17-c677-4bb5-b047-97016ec1e403\",\"modelCustomizationName\":\"ce0fdd17-c677-4bb5-b047-97016ec1e403\",\"modelVersion\":\"1.0\"},\"requestParameters\":{\"userParams\":[]},\"cloudConfiguration\":{\"lcpCloudRegionId\":\"EastUS\",\"tenantId\":\"48de34f6-65a1-4d09-84b4-68b011151672\"},\"relatedInstanceList\":[{\"relatedInstance\":{\"instanceId\":\"43b34d6d-1ab2-4c7a-a3a0-5471306550c5\",\"modelInfo\":{\"modelType\":\"service\",\"modelInvariantId\":\"1192c9b7-bc24-42c9-8f11-415dc679be88\",\"modelVersionId\":\"acb8b74b-afe6-4cc2-92c3-0a09961ab77e\",\"modelName\":\"service\",\"modelVersion\":\"1.0\"}}},{\"relatedInstance\":{\"instanceId\":\"7b1ead4f-ea06-45c6-921e-124061e5eae7\",\"modelInfo\":{\"modelType\":\"vnf\",\"modelInvariantId\":\"a545165e-9646-4030-824c-b9d9c66a886a\",\"modelVersionId\":\"a0b6dffe-0de3-4099-8b94-dc05be942914\",\"modelName\":\"vnf-mdoel\",\"modelVersion\":\"1.0\",\"modelCustomizationName\":\"vnf-mdoel 0\"}}}]}}";
		Response resp = instance.createVfModuleInstance(requestJson, "v5","43b34d6d-1ab2-4c7a-a3a0-5471306550c5", "7b1ead4f-ea06-45c6-921e-124061e5eae7");
		String respStr = resp.getEntity().toString();
		assertTrue(respStr.equals("success"));
	}

	@Test
	public void createPortConfigurationTestNormal() {

		new MockUp<RequestsDatabase>() {
			@Mock
			public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
				return null;
			}
		};

		new MockUp<RequestsDatabase>() {
			@Mock
			public int updateInfraStatus (String requestId, String requestStatus, long progress, String lastModifiedBy) {
				return 1;
			}
		};

		new MockUp<MsoRequest>() {
			@Mock
			public void createRequestRecord (Status status, Action action) {
				return;
			}
		};

		new MockUp<CamundaClient>() {
			@Mock
			public HttpResponse post(RequestClientParamater requestClientParamater) {
				ProtocolVersion pv = new ProtocolVersion("HTTP",1,1);
				HttpResponse resp = new BasicHttpResponse(pv,200, "test response");
				BasicHttpEntity entity = new BasicHttpEntity();

				final String body = "{\"content\":\"success\",\"message\":\"success\"}";
				InputStream instream = new ByteArrayInputStream(body.getBytes());
				entity.setContent(instream);
				resp.setEntity(entity);
				return resp;
			}
		};

		ServiceInstances sir = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"43b34d6d-1ab2-4c7a-a3a0-5471306550c5\",\"vnfInstanceId\":\"7b1ead4f-ea06-45c6-921e-124061e5eae7\",\"networkInstanceId\":\"1882937\",\"volumeGroupInstanceId\":\"1882935\",\"vfModuleInstanceId\":\"1882934\",\"requestDetails\":{\"requestInfo\":{\"instanceName\":\"vf-inst\",\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"123123\"},\"modelInfo\":{\"modelType\":\"vfModule\",\"modelInvariantId\":\"dde10afa-c732-4f0f-8501-2d2e01ea46ef\",\"modelVersionId\":\"296e278c-bfa8-496e-b59e-fb1fe715f726\",\"modelName\":\"CarrierTosca0::module-1\",\"modelCustomizationId\":\"ce0fdd17-c677-4bb5-b047-97016ec1e403\",\"modelCustomizationName\":\"ce0fdd17-c677-4bb5-b047-97016ec1e403\",\"modelVersion\":\"1.0\"},\"requestParameters\":{\"userParams\":[]},\"cloudConfiguration\":{\"lcpCloudRegionId\":\"EastUS\",\"tenantId\":\"48de34f6-65a1-4d09-84b4-68b011151672\"},\"relatedInstanceList\":[{\"relatedInstance\":{\"instanceId\":\"43b34d6d-1ab2-4c7a-a3a0-5471306550c5\",\"modelInfo\":{\"modelType\":\"service\",\"modelInvariantId\":\"1192c9b7-bc24-42c9-8f11-415dc679be88\",\"modelVersionId\":\"acb8b74b-afe6-4cc2-92c3-0a09961ab77e\",\"modelName\":\"service\",\"modelVersion\":\"1.0\"}}},{\"relatedInstance\":{\"instanceId\":\"7b1ead4f-ea06-45c6-921e-124061e5eae7\",\"modelInfo\":{\"modelType\":\"vnf\",\"modelInvariantId\":\"a545165e-9646-4030-824c-b9d9c66a886a\",\"modelVersionId\":\"a0b6dffe-0de3-4099-8b94-dc05be942914\",\"modelName\":\"vnf-mdoel\",\"modelVersion\":\"1.0\",\"modelCustomizationName\":\"vnf-mdoel 0\"}}}]}}";
		final Response response = sir.createPortConfiguration(requestJson, "v5", "43b34d6d-1ab2-4c7a-a3a0-5471306550c5");
	}

	@Test
	public void createPortConfigurationTestBlankOrchestrationURI() {

		new MockUp<RequestsDatabase>() {
			@Mock
			public InfraActiveRequests checkInstanceNameDuplicate (HashMap<String,String> instanceIdMap, String instanceName, String requestScope) {
				return null;
			}
		};

		new MockUp<RequestsDatabase>() {
			@Mock
			public int updateInfraStatus (String requestId, String requestStatus, long progress, String lastModifiedBy) {
				return 1;
			}
		};

		new MockUp<MsoRequest>() {
			@Mock
			public void createRequestRecord (Status status, Action action) {
				return;
			}
		};

		ServiceInstances sir = new ServiceInstances();
		String requestJson = "{\"serviceInstanceId\":\"43b34d6d-1ab2-4c7a-a3a0-5471306550c5\",\"vnfInstanceId\":\"7b1ead4f-ea06-45c6-921e-124061e5eae7\",\"networkInstanceId\":\"1882937\",\"volumeGroupInstanceId\":\"1882935\",\"vfModuleInstanceId\":\"1882934\",\"requestDetails\":{\"requestInfo\":{\"instanceName\":\"vf-inst\",\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"123123\"},\"modelInfo\":{\"modelType\":\"vfModule\",\"modelInvariantId\":\"dde10afa-c732-4f0f-8501-2d2e01ea46ef\",\"modelVersionId\":\"296e278c-bfa8-496e-b59e-fb1fe715f726\",\"modelName\":\"CarrierTosca0::module-1\",\"modelCustomizationId\":\"ce0fdd17-c677-4bb5-b047-97016ec1e403\",\"modelCustomizationName\":\"ce0fdd17-c677-4bb5-b047-97016ec1e403\",\"modelVersion\":\"1.0\"},\"requestParameters\":{\"userParams\":[]},\"cloudConfiguration\":{\"lcpCloudRegionId\":\"EastUS\",\"tenantId\":\"48de34f6-65a1-4d09-84b4-68b011151672\"},\"relatedInstanceList\":[{\"relatedInstance\":{\"instanceId\":\"43b34d6d-1ab2-4c7a-a3a0-5471306550c5\",\"modelInfo\":{\"modelType\":\"service\",\"modelInvariantId\":\"1192c9b7-bc24-42c9-8f11-415dc679be88\",\"modelVersionId\":\"acb8b74b-afe6-4cc2-92c3-0a09961ab77e\",\"modelName\":\"service\",\"modelVersion\":\"1.0\"}}},{\"relatedInstance\":{\"instanceId\":\"7b1ead4f-ea06-45c6-921e-124061e5eae7\",\"modelInfo\":{\"modelType\":\"vnf\",\"modelInvariantId\":\"a545165e-9646-4030-824c-b9d9c66a886a\",\"modelVersionId\":\"a0b6dffe-0de3-4099-8b94-dc05be942914\",\"modelName\":\"vnf-mdoel\",\"modelVersion\":\"1.0\",\"modelCustomizationName\":\"vnf-mdoel 0\"}}}]}}";
		final Response response = sir.createPortConfiguration(requestJson, "v5", "43b34d6d-1ab2-4c7a-a3a0-5471306550c5");
	}
}
