/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2017 Huawei Technologies Co., Ltd. All rights reserved.
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

package org.openecomp.mso.adapters.nwrest;



import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.openecomp.mso.logger.MsoLogger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Everything that is common between all Network Requests.
 */
public abstract class NetworkRequestCommon {
    private static final MsoLogger LOGGER = MsoLogger.getMsoLogger (MsoLogger.Catalog.RA);
	private Boolean skipAAI = false;
	private String messageId;
	private String notificationUrl;
	@JsonProperty
	private boolean synchronous;
	public Boolean getSkipAAI() {
		return skipAAI;
	}

	public void setSkipAAI(Boolean skipAAI) {
		this.skipAAI = skipAAI;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getNotificationUrl() {
		return notificationUrl;
	}

	public void setNotificationUrl(String notificationUrl) {
		this.notificationUrl = notificationUrl;
	}

	public boolean isSynchronous() {
		return notificationUrl == null || (notificationUrl.isEmpty());
	}
	
	@JsonIgnore
	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public String toJsonString() {
		String jsonString = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
			jsonString = mapper.writeValueAsString(this);
		} catch (Exception e) {
		    LOGGER.debug("Exception:", e);
		}
		return jsonString;
	}

	public String toXmlString() {
		try {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			JAXBContext context = JAXBContext.newInstance(this.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //pretty print XML
			marshaller.marshal(this, bs);
			return bs.toString();
		} catch (Exception e) {
		    LOGGER.debug("Exception:", e);
			return "";
		}
	}
}
