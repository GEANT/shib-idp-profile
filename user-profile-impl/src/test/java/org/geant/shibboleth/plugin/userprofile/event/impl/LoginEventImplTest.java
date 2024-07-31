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
 * Unit tests for {@link LoginEventImpl}.
 */
public class LoginEventImplTest {

    @Test
    public void test090Format() throws JsonMappingException, JsonProcessingException {
        LoginEventImpl event = LoginEventImpl.parse(
                "{\"id\":\"id\",\"attributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}],\"time\":500}");
        event = LoginEventImpl.parse(event.serialize());
        Assert.assertEquals(event.getId(), "id");
        Assert.assertEquals(event.getAcr(), "n/a");
        Assert.assertEquals(event.getTime(), 500);
        Assert.assertTrue(event.getAttributes().size() == 2);
        Assert.assertEquals(event.getAttributes().get(0).getName(), "name");
    }

    @Test
    public void testFormat() throws JsonMappingException, JsonProcessingException {
        LoginEventImpl event = LoginEventImpl.parse(
                "{\"id\":\"id\",\"acr\":\"refedsMFA\",\"attributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}],\"time\":500}");
        event = LoginEventImpl.parse(event.serialize());
        Assert.assertEquals(event.getId(), "id");
        Assert.assertEquals(event.getAcr(), "refedsMFA");
        Assert.assertEquals(event.getTime(), 500);
        Assert.assertTrue(event.getAttributes().size() == 2);
        Assert.assertEquals(event.getAttributes().get(0).getName(), "name");
    }

}
