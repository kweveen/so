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

package org.openecomp.mso.apihandlerinfra.tasksbeans;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
@JsonRootName(value = "variables")
@JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
public class Variables {

	protected Value source;
	protected Value responseValue;
    protected Value requestorId;
   
    /**
     * Gets the value of the source property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public Value getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSource(Value value) {
        this.source = value;
    }

	public Value getResponseValue() {
		return responseValue;
	}

	public void setResponseValue(Value responseValue) {
		this.responseValue = responseValue;
	}

	
	public Value getRequestorId() {
		return requestorId;
	}

	public void setRequestorId(Value requestorId) {
		this.requestorId = requestorId;
	}

}
