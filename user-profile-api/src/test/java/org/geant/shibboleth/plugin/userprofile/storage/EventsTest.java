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

package org.geant.shibboleth.plugin.userprofile.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link Events}.
 */
public class EventsTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        Events events = Events
                .parse("{\"key1\" : {\"value\":\"foo1\",\"time\":100}, \"key2\" : {\"value\":\"foo2\",\"time\":100} }");
        events = Events.parse(events.serialize());
        Assert.assertEquals(events.getEvents().size(), 2);
        Assert.assertEquals(events.getEvents().get("key1").getValue(), "foo1");
    }
}
