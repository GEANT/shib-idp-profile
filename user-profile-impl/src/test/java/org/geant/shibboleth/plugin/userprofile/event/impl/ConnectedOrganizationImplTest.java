/*
 * Copyright (c) 2022-2023, GÉANT
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link ConnectedServiceImpl}.
 */
public class ConnectedOrganizationImplTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        ConnectedServiceImpl connectedOrganization = ConnectedServiceImpl.parse(
                " {\"id\":\"foo\",\"name\":\"fooName\",\"times\":2,\"lastAttributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id2\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}]} ");
        connectedOrganization = ConnectedServiceImpl.parse(connectedOrganization.serialize());
        Assert.assertEquals(connectedOrganization.getId(), "foo");
        Assert.assertEquals(connectedOrganization.getTimes(), 2);
        Assert.assertEquals(connectedOrganization.getLastAttributes().size(), 2);
    }
}
