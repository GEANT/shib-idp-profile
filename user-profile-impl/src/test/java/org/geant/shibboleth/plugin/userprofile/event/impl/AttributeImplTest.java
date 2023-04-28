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
 * Unit tests for {@link AttributeImpl}.
 */
public class AttributeImplTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        AttributeImpl attribute = AttributeImpl.parse(
                " {\"id\":\"foo\",\"name\":\"fooName\",\"description\":\"fooDesc\",\"values\":[\"foo\",\"bar\"]} ");
        attribute = AttributeImpl.parse(attribute.serialize());
        Assert.assertEquals(attribute.getId(), "foo");
        Assert.assertEquals(attribute.getName(), "fooName");
        Assert.assertEquals(attribute.getDescription(), "fooDesc");
        Assert.assertTrue(attribute.getValues().contains("foo"));
    }

    @Test
    public void testNoValues() throws JsonMappingException, JsonProcessingException {
        AttributeImpl attribute = AttributeImpl
                .parse(" {\"id\":\"foo\",\"name\":\"fooName\",\"description\":\"fooDesc\"} ");
        attribute = AttributeImpl.parse(attribute.serialize());
        Assert.assertNull(attribute.getValues());
    }
}
