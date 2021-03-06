package org.openecomp.mso.bpmn.common.scripts

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.openecomp.mso.bpmn.common.scripts.AbstractServiceTaskProcessor
import org.openecomp.mso.bpmn.common.scripts.ExceptionUtil
import org.openecomp.mso.bpmn.common.scripts.MsoUtils
import org.openecomp.mso.bpmn.core.domain.HomingSolution
import org.openecomp.mso.bpmn.core.domain.ModelInfo
import org.openecomp.mso.bpmn.core.domain.Resource
import org.openecomp.mso.bpmn.core.domain.AllottedResource
import org.openecomp.mso.bpmn.core.domain.ServiceDecomposition
import org.openecomp.mso.bpmn.core.domain.ServiceInstance
import org.openecomp.mso.bpmn.core.domain.Subscriber
import org.openecomp.mso.bpmn.core.domain.VnfResource
import org.openecomp.mso.bpmn.core.json.JsonUtils

import java.lang.reflect.Array

import static org.openecomp.mso.bpmn.common.scripts.GenericUtils.*

class OofUtils {

    ExceptionUtil exceptionUtil = new ExceptionUtil()
    JsonUtils jsonUtil = new JsonUtils()

    private AbstractServiceTaskProcessor utils

    public MsoUtils msoUtils = new MsoUtils()

    public OofUtils(AbstractServiceTaskProcessor taskProcessor) {
        this.utils = taskProcessor
    }

    /**
     * This method builds the service-agnostic
     * OOF json request to get a homing solution
     * and license solution
     *
     * @param execution
     * @param requestId
     * @param decomposition - ServiceDecomposition object
     * @param customerLocation -
     * @param existingCandidates -
     * @param excludedCandidates -
     * @param requiredCandidates -
     *
     * @return request - OOF v1 payload - https://wiki.onap.org/pages/viewpage.action?pageId=25435066
     */
    String buildRequest(DelegateExecution execution,
                        String requestId,
                        ServiceDecomposition decomposition,
                        Subscriber subscriber = null,
                        Map customerLocation,
                        ArrayList existingCandidates = null,
                        ArrayList excludedCandidates = null,
                        ArrayList requiredCandidates = null) {
        def isDebugEnabled = execution.getVariable("isDebugLogEnabled")
        utils.log("DEBUG", "Started Building OOF Request", isDebugEnabled)
        def callbackUrl = utils.createWorkflowMessageAdapterCallbackURL(execution, "oofResponse", requestId)
        def transactionId = requestId
        //ServiceInstance Info
        ServiceInstance serviceInstance = decomposition.getServiceInstance()
        def serviceInstanceId = ""
        def serviceInstanceName = ""

        serviceInstanceId = execution.getVariable("serviceInstanceId")
        serviceInstanceName = execution.getVariable("serviceInstanceName")

        if (serviceInstanceId == null || serviceInstanceId == "null") {
            utils.log("DEBUG", "Unable to obtain Service Instance Id", isDebugEnabled)
            exceptionUtil.buildAndThrowWorkflowException(execution, 400, "Internal Error - Unable to " +
                    "obtain Service Instance Id, execution.getVariable(\"serviceInstanceName\") is null")
        }
        if (serviceInstanceName == null || serviceInstanceName == "null") {
            utils.log("DEBUG", "Unable to obtain Service Instance Name", isDebugEnabled)
            exceptionUtil.buildAndThrowWorkflowException(execution, 400, "Internal Error - Unable to " +
                    "obtain Service Instance Name, execution.getVariable(\"serviceInstanceName\") is null")
        }
        //Model Info
        ModelInfo model = decomposition.getModelInfo()
        String modelType = model.getModelType()
        String modelInvariantId = model.getModelInvariantUuid()
        String modelVersionId = model.getModelUuid()
        String modelName = model.getModelName()
        String modelVersion = model.getModelVersion()
        //Subscriber Info
        String subscriberId = ""
        String subscriberName = ""
        String commonSiteId = ""
        if (subscriber != null){
            subscriberId = subscriber.getGlobalId()
            subscriberName = subscriber.getName()
            commonSiteId = subscriber.getCommonSiteId()
        }

        //Determine RequestType
        //TODO Figure out better way to determine this
        String requestType = "create"
        List<Resource> resources = decomposition.getServiceResources()
        for(Resource r:resources){
            HomingSolution currentSolution = (HomingSolution) r.getCurrentHomingSolution()
            if(currentSolution != null){
                requestType = "speed changed"
            }
        }

        //Demands
        String placementDemands = ""
        StringBuilder sb = new StringBuilder()
        List<AllottedResource> allottedResourceList = decomposition.getServiceAllottedResources()
        List<VnfResource> vnfResourceList = decomposition.getServiceVnfs()

        if (allottedResourceList.isEmpty() || allottedResourceList == null) {
            utils.log("DEBUG", "Allotted Resources List is empty - will try to get service VNFs instead.",
                    isDebugEnabled)
            allottedResourceList = decomposition.getServiceVnfs()
        }

        if (allottedResourceList.isEmpty() || allottedResourceList == null) {
            utils.log("DEBUG", "Resources List is Empty", isDebugEnabled)
        } else {
            for (AllottedResource resource : allottedResourceList) {
                utils.log("DEBUG", "Allotted Resource: " + resource.toString(),
                        isDebugEnabled)
                def serviceResourceId = resource.getResourceId()
                def resourceModuleName = resource.getNfFunction()
                utils.log("DEBUG", "resourceModuleName: " + resourceModuleName,
                        isDebugEnabled)
                def resourceModelInvariantId = "no-resourceModelInvariantId"
                def resourceModelVersionId = "no-resourceModelVersionId"

                List modelIdLst = execution.getVariable("homingModelIds")
                utils.log("DEBUG", "Incoming modelIdLst is: " + modelIdLst.toString(), isDebugEnabled)
                for (Map modelId : modelIdLst )
                    if (resourceModuleName == modelId.resourceModuleName) {
                        resourceModelInvariantId = modelId.resourceModelInvariantId
                        resourceModelVersionId = modelId.resourceModelVersionId
                    }

                def resourceModelName = "" //Optional
                def resourceModelVersion = "" //Optional
                def resourceModelType = "" //Optional
                def tenantId = "" //Optional
                def requiredCandidatesJson = ""

                requiredCandidatesJson = createCandidateJson(
                        existingCandidates,
                        excludedCandidates,
                        requiredCandidates)

                String demand =
                        "      {\n" +
                        "      \"resourceModuleName\": \"${resourceModuleName}\",\n" +
                        "      \"serviceResourceId\": \"${serviceResourceId}\",\n" +
                        "      \"tenantId\": \"${tenantId}\",\n" +
                        "      \"resourceModelInfo\": {\n" +
                        "        \"modelInvariantId\": \"${resourceModelInvariantId}\",\n" +
                        "        \"modelVersionId\": \"${resourceModelVersionId}\",\n" +
                        "        \"modelName\": \"${resourceModelName}\",\n" +
                        "        \"modelType\": \"${resourceModelType}\",\n" +
                        "        \"modelVersion\": \"${resourceModelVersion}\",\n" +
                        "        \"modelCustomizationName\": \"\"\n" +
                        "        }" + requiredCandidatesJson + "\n" +
                        "      },"

                placementDemands = sb.append(demand)
            }
            for (VnfResource vnfResource : vnfResourceList) {
                utils.log("DEBUG", "VNF Resource: " + vnfResource.toString(),
                        isDebugEnabled)
                ModelInfo vnfResourceModelInfo = vnfResource.getModelInfo()
                def serviceResourceId = vnfResource.getResourceId()
                def resourceModuleName = vnfResource.getNfFunction()
                utils.log("DEBUG", "resourceModuleName: " + resourceModuleName,
                        isDebugEnabled)
                def resourceModelInvariantId = vnfResourceModelInfo.getModelInvariantUuid()
                def resourceModelName = vnfResourceModelInfo.getModelName()
                def resourceModelVersion = vnfResourceModelInfo.getModelVersion()
                def resourceModelVersionId = vnfResourceModelInfo.getModelUuid()
                def resourceModelType = vnfResourceModelInfo.getModelType()
                def tenantId = "" //Optional
                def requiredCandidatesJson = ""


                String placementDemand =
                        "      {\n" +
                        "      \"resourceModuleName\": \"${resourceModuleName}\",\n" +
                        "      \"serviceResourceId\": \"${serviceResourceId}\",\n" +
                        "      \"tenantId\": \"${tenantId}\",\n" +
                        "      \"resourceModelInfo\": {\n" +
                        "        \"modelInvariantId\": \"${resourceModelInvariantId}\",\n" +
                        "        \"modelVersionId\": \"${resourceModelVersionId}\",\n" +
                        "        \"modelName\": \"${resourceModelName}\",\n" +
                        "        \"modelType\": \"${resourceModelType}\",\n" +
                        "        \"modelVersion\": \"${resourceModelVersion}\",\n" +
                        "        \"modelCustomizationName\": \"\"\n" +
                        "        }" + requiredCandidatesJson + "\n" +
                        "      },"

                placementDemands = sb.append(placementDemand)
            }
            placementDemands = placementDemands.substring(0, placementDemands.length() - 1)
        }

        /* Commenting Out Licensing as OOF doesn't support for Beijing
        String licenseDemands = ""
        sb = new StringBuilder()
        if (vnfResourceList.isEmpty() || vnfResourceList == null) {
            utils.log("DEBUG", "Vnf Resources List is Empty", isDebugEnabled)
        } else {
            for (VnfResource vnfResource : vnfResourceList) {
                ModelInfo vnfResourceModelInfo = vnfResource.getModelInfo()
                def resourceInstanceType = vnfResource.getResourceType()
                def serviceResourceId = vnfResource.getResourceId()
                def resourceModuleName = vnfResource.getResourceType()
                def resouceModelInvariantId = vnfResourceModelInfo.getModelInvariantUuid()
                def resouceModelName = vnfResourceModelInfo.getModelName()
                def resouceModelVersion = vnfResourceModelInfo.getModelVersion()
                def resouceModelVersionId = vnfResourceModelInfo.getModelUuid()
                def resouceModelType = vnfResourceModelInfo.getModelType()

                // TODO Add Existing Licenses to demand
                //"existingLicenses": {
                //"entitlementPoolUUID": ["87257b49-9602-4ca1-9817-094e52bc873b",
                // "43257b49-9602-4fe5-9337-094e52bc9435"],
                //"licenseKeyGroupUUID": ["87257b49-9602-4ca1-9817-094e52bc873b",
                // "43257b49-9602-4fe5-9337-094e52bc9435"]
                //}

                    String licenseDemand =
                        "{\n" +
                        "\"resourceModuleName\": \"${resourceModuleName}\",\n" +
                        "\"serviceResourceId\": \"${serviceResourceId}\",\n" +
                        "\"resourceInstanceType\": \"${resourceInstanceType}\",\n" +
                        "\"resourceModelInfo\": {\n" +
                        "  \"modelInvariantId\": \"${resouceModelInvariantId}\",\n" +
                        "  \"modelVersionId\": \"${resouceModelVersionId}\",\n" +
                        "  \"modelName\": \"${resouceModelName}\",\n" +
                        "  \"modelType\": \"${resouceModelType}\",\n" +
                        "  \"modelVersion\": \"${resouceModelVersion}\",\n" +
                        "  \"modelCustomizationName\": \"\"\n" +
                        "  }\n"
                        "},"

                licenseDemands = sb.append(licenseDemand)
            }
            licenseDemands = licenseDemands.substring(0, licenseDemands.length() - 1)
        }*/

        String request =
                "{\n" +
                "  \"requestInfo\": {\n" +
                "    \"transactionId\": \"${transactionId}\",\n" +
                "    \"requestId\": \"${requestId}\",\n" +
                "    \"callbackUrl\": \"${callbackUrl}\",\n" +
                "    \"sourceId\": \"so\",\n" +
                "    \"requestType\": \"${requestType}\"," +
                "    \"numSolutions\": 1,\n" +
                "    \"optimizers\": [\"placement\"],\n" +
                "    \"timeout\": 600\n" +
                "    },\n" +
                "  \"placementInfo\": {\n" +
                "    \"requestParameters\": {\n" +
                "      \"customerLatitude\": \"${customerLocation.customerLatitude}\",\n" +
                "      \"customerLongitude\": \"${customerLocation.customerLongitude}\",\n" +
                "      \"customerName\": \"${customerLocation.customerName}\"\n" +
                "    }," +
                "    \"subscriberInfo\": { \n" +
                "      \"globalSubscriberId\": \"${subscriberId}\",\n" +
                "      \"subscriberName\": \"${subscriberName}\",\n" +
                "      \"subscriberCommonSiteId\": \"${commonSiteId}\"\n" +
                "    },\n" +
                "    \"placementDemands\": [\n" +
                "      ${placementDemands}\n" +
                "      ]\n" +
                "    },\n" +
                "  \"serviceInfo\": {\n" +
                "    \"serviceInstanceId\": \"${serviceInstanceId}\",\n" +
                "    \"serviceName\": \"${serviceInstanceName}\",\n" +
                "    \"modelInfo\": {\n" +
                "      \"modelType\": \"${modelType}\",\n" +
                "      \"modelInvariantId\": \"${modelInvariantId}\",\n" +
                "      \"modelVersionId\": \"${modelVersionId}\",\n" +
                "      \"modelName\": \"${modelName}\",\n" +
                "      \"modelVersion\": \"${modelVersion}\",\n" +
                "      \"modelCustomizationName\": \"\"\n" +
                "    }\n" +
                "  }\n" +
                "}"


        utils.log("DEBUG", "Completed Building OOF Request", isDebugEnabled)
        return request
    }

    /**
     * This method validates the callback response
     * from OOF. If the response contains an
     * exception the method will build and throw
     * a workflow exception.
     *
     * @param execution
     * @param response - the async callback response from oof
     */
    Void validateCallbackResponse(DelegateExecution execution, String response) {
        def isDebugEnabled = execution.getVariable("isDebugLogEnabled")
        String placements = ""
        if (isBlank(response)) {
            exceptionUtil.buildAndThrowWorkflowException(execution, 5000, "OOF Async Callback Response is Empty")
        } else {
            if (JsonUtils.jsonElementExist(response, "solutions.placementSolutions")) {
                placements = jsonUtil.getJsonValue(response, "solutions.placementSolutions")
                if (isBlank(placements) || placements.equalsIgnoreCase("[]")) {
                    String statusMessage = jsonUtil.getJsonValue(response, "statusMessage")
                    if (isBlank(statusMessage)) {
                        utils.log("DEBUG", "Error Occurred in Homing: OOF Async Callback Response does " +
                                "not contain placement solution.", isDebugEnabled)
                        exceptionUtil.buildAndThrowWorkflowException(execution, 400,
                                "OOF Async Callback Response does not contain placement solution.")
                    } else {
                        utils.log("DEBUG", "Error Occurred in Homing: " + statusMessage, isDebugEnabled)
                        exceptionUtil.buildAndThrowWorkflowException(execution, 400, statusMessage)
                    }
                } else {
                    return
                }
            } else if (response.contains("error") || response.contains("Error") ) {
                String errorMessage = ""
                if (response.contains("policyException")) {
                    String text = jsonUtil.getJsonValue(response, "requestError.policyException.text")
                    errorMessage = "OOF Async Callback Response contains a Request Error Policy Exception: " + text
                } else if (response.contains("Unable to find any candidate for demand")) {
                    errorMessage = "OOF Async Callback Response contains error: Unable to find any candidate for " +
                            "demand *** Response: " + response.toString()
                } else if (response.contains("serviceException")) {
                    String text = jsonUtil.getJsonValue(response, "requestError.serviceException.text")
                    errorMessage = "OOF Async Callback Response contains a Request Error Service Exception: " + text
                } else {
                    errorMessage = "OOF Async Callback Response contains a Request Error. Unable to determine the Request Error Exception."
                }
                utils.log("DEBUG", "Error Occurred in Homing: " + errorMessage, isDebugEnabled)
                exceptionUtil.buildAndThrowWorkflowException(execution, 400, errorMessage)

            } else {
                utils.log("DEBUG", "Error Occurred in Homing: Received an Unknown Async Callback Response from OOF.", isDebugEnabled)
                exceptionUtil.buildAndThrowWorkflowException(execution, 2500, "Received an Unknown Async Callback Response from OOF.")
            }
        }

    }

    /**
     * This method creates candidates json for placement Demands.
     *
     * @param execution
     * @param existingCandidates -
     * @param excludedCandidates -
     * @param requiredCandidates -
     *
     * @return candidatesJson - a JSON string with candidates
     */
    String createCandidateJson(ArrayList existingCandidates = null,
                               ArrayList excludedCandidates = null,
                               ArrayList requiredCandidates = null) {
        def candidatesJson = ""
        def type = ""
        if (existingCandidates != null && existingCandidates != {}) {
            sb = new StringBuilder()
            sb.append(",\n" +
                    "  \"existingCandidates\": [\n")
            def existingCandidateJson = ""
            existingCandidates.each { existingCandidate ->
                type = existingCandidate.get('identifierType')
                if (type == 'vimId') {
                    def cloudOwner = existingCandidate.get('cloudOwner')
                    def cloudRegionId = existingCandidate.get('identifiers')
                    existingCandidateJson = "{\n" +
                            "    \"identifierType\": \"vimId\",\n" +
                            "    \"cloudOwner\": \"${cloudOwner}\",\n" +
                            "    \"identifiers\": [\"${cloudRegionId}\"]\n" +
                            "    },"
                    sb.append(existingCandidateJson)
                }
                if (type == 'serviceInstanceId') {
                    def serviceInstanceId = existingCandidate.get('identifiers')
                    existingCandidateJson += "{\n" +
                            "    \"identifierType\": \"serviceInstanceId\",\n" +
                            "    \"identifiers\": [\"${serviceInstanceId}\"]\n" +
                            "    },"
                    sb.append(existingCandidateJson)
                }
            }
            if (existingCandidateJson != "") {
                sb.setLength(sb.length() - 1)
                candidatesJson = sb.append(",\n],")
            }
        }
        if (excludedCandidates != null && excludedCandidates != {}) {
            sb = new StringBuilder()
            sb.append(",\n" +
                    "  \"excludedCandidates\": [\n")
            def excludedCandidateJson = ""
            excludedCandidates.each { excludedCandidate ->
                type = excludedCandidate.get('identifierType')
                if (type == 'vimId') {
                    def cloudOwner = excludedCandidate.get('cloudOwner')
                    def cloudRegionId = excludedCandidate.get('identifiers')
                    excludedCandidateJson = "{\n" +
                            "    \"identifierType\": \"vimId\",\n" +
                            "    \"cloudOwner\": \"${cloudOwner}\",\n" +
                            "    \"identifiers\": [\"${cloudRegionId}\"]\n" +
                            "    },"
                    sb.append(excludedCandidateJson)
                }
                if (type == 'serviceInstanceId') {
                    def serviceInstanceId = excludedCandidate.get('identifiers')
                    excludedCandidateJson += "{\n" +
                            "    \"identifierType\": \"serviceInstanceId\",\n" +
                            "    \"identifiers\": [\"${serviceInstanceId}\"]\n" +
                            "    },"
                    sb.append(excludedCandidateJson)
                }
            }
            if (excludedCandidateJson != "") {
                sb.setLength(sb.length() - 1)
                candidatesJson = sb.append(",\n],")
            }
        }
        if (requiredCandidates != null && requiredCandidates != {}) {
            sb = new StringBuilder()
            sb.append(",\n" +
                    "  \"requiredCandidates\": [\n")
            def requiredCandidatesJson = ""
            requiredCandidates.each { requiredCandidate ->
                type = requiredCandidate.get('identifierType')
                if (type == 'vimId') {
                    def cloudOwner = requiredCandidate.get('cloudOwner')
                    def cloudRegionId = requiredCandidate.get('identifiers')
                    requiredCandidatesJson = "{\n" +
                            "    \"identifierType\": \"vimId\",\n" +
                            "    \"cloudOwner\": \"${cloudOwner}\",\n" +
                            "    \"identifiers\": [\"${cloudRegionId}\"]\n" +
                            "    },"
                    sb.append(requiredCandidatesJson)
                }
                if (type == 'serviceInstanceId') {
                    def serviceInstanceId = requiredCandidate.get('identifiers')
                    requiredCandidatesJson += "{\n" +
                            "    \"identifierType\": \"serviceInstanceId\",\n" +
                            "    \"identifiers\": [\"${serviceInstanceId}\"]\n" +
                            "    },"
                    sb.append(requiredCandidatesJson)
                }
            }
            if (requiredCandidatesJson != "") {
                sb.setLength(sb.length() - 1)
                candidatesJson = sb.append(",\n],")
            }
        }
        if (candidatesJson != "") {candidatesJson = candidatesJson.substring(0, candidatesJson.length() - 1)}
        return candidatesJson
    }
}