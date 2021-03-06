/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2018 Huawei Technologies Co., Ltd. All rights reserved.
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
package org.openecomp.mso.openstack.beans;

import org.junit.Test;

public class RouteTargetTest {

    RouteTarget routeTarget = new RouteTarget();

    @Test
    public void getRouteTarget() throws Exception {
        routeTarget.getRouteTarget();
    }

    @Test
    public void setRouteTarget() throws Exception {
        routeTarget.setRouteTarget("1.1.1.1");
    }

    @Test
    public void getRouteTargetRole() throws Exception {
        routeTarget.getRouteTargetRole();
    }

    @Test
    public void setRole() throws Exception {
        routeTarget.setRole("test");
    }

}