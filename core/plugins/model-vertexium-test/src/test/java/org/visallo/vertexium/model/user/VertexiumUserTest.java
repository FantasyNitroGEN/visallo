package org.visallo.vertexium.model.user;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.model.user.UserVisalloProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumUserTest {
    @Mock
    private Vertex userVertex;

    @Test
    public void testGetCustomPropertiesFilterBuiltInProperties() {
        List<Property> properties = new ArrayList<>();

        Property builtInProp = mock(Property.class);
        when(builtInProp.getName()).thenReturn(UserVisalloProperties.PASSWORD_HASH.getPropertyName());
        when(builtInProp.getValue()).thenReturn("secret");
        properties.add(builtInProp);

        Property notBuiltInProp = mock(Property.class);
        when(notBuiltInProp.getName()).thenReturn("otherProp");
        when(notBuiltInProp.getValue()).thenReturn("open");
        properties.add(notBuiltInProp);

        when(userVertex.getProperties()).thenReturn(properties);

        VertexiumUser user = new VertexiumUser(userVertex);
        Map<String, Object> customProperties = user.getCustomProperties();
        assertFalse(customProperties.containsKey(UserVisalloProperties.PASSWORD_HASH.getPropertyName()));
        assertTrue(customProperties.containsKey("otherProp"));
        assertEquals("open", customProperties.get("otherProp"));
    }
}