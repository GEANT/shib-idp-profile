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
        AttributeImpl attribute = AttributeImpl.parse(
                " {\"id\":\"foo\",\"name\":\"fooName\",\"description\":\"fooDesc\"} ");
        attribute = AttributeImpl.parse(attribute.serialize());
        Assert.assertNull(attribute.getValues());
    }
}
