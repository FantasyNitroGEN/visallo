package org.visallo.core.security;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElementAcl;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiPropertyAcl;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ACLProviderTest {

    @Mock private OntologyRepository ontologyRepository;
    @Mock private OntologyProperty ontologyProperty1;
    @Mock private OntologyProperty ontologyProperty2;
    @Mock private OntologyProperty ontologyProperty3;
    @Mock private Concept elementConcept;
    @Mock private Concept parentConcept;
    @Mock private Element element;
    @Mock private Property elementProperty1;
    @Mock private Property elementProperty2a;
    @Mock private Property elementProperty2b;
    @Mock private Property elementProperty3;
    @Mock private User user;

    private ACLProvider aclProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        // mock ACLProvider abstract methods, but call implemented methods
        aclProvider = mock(ACLProvider.class);
        when(aclProvider.elementACL(any(Element.class), any(User.class), any(OntologyRepository.class)))
                .thenCallRealMethod();
        when(aclProvider.appendACL(any(ClientApiObject.class), any(User.class))).thenCallRealMethod();
        doCallRealMethod().when(aclProvider).appendACL(any(Collection.class), any(User.class));

        when(ontologyRepository.getConceptByIRI("element")).thenReturn(elementConcept);
        when(ontologyRepository.getConceptByIRI("parent")).thenReturn(parentConcept);

        when(elementConcept.getParentConceptIRI()).thenReturn("parent");
        when(elementConcept.getProperties()).thenReturn(ImmutableList.of(ontologyProperty1, ontologyProperty2));

        when(parentConcept.getParentConceptIRI()).thenReturn(null);
        when(parentConcept.getProperties()).thenReturn(ImmutableList.of(ontologyProperty3));

        when(ontologyProperty1.getTitle()).thenReturn("prop1");
        when(ontologyProperty2.getTitle()).thenReturn("prop2");
        when(ontologyProperty3.getTitle()).thenReturn("prop3");

        when(element.getPropertyValue(VisalloProperties.CONCEPT_TYPE.getPropertyName())).thenReturn("element");
        when(element.getProperties("prop1")).thenReturn(ImmutableList.of(elementProperty1));
        when(element.getProperties("prop2")).thenReturn(ImmutableList.of(elementProperty2a, elementProperty2b));
        when(element.getProperties("prop3")).thenReturn(ImmutableList.of(elementProperty3));

        when(elementProperty1.getName()).thenReturn("prop1");
        when(elementProperty1.getKey()).thenReturn("keyA");

        when(elementProperty2a.getName()).thenReturn("prop2");
        when(elementProperty2a.getKey()).thenReturn("keyA");

        when(elementProperty2b.getName()).thenReturn("prop2");
        when(elementProperty2b.getKey()).thenReturn("keyB");

        when(elementProperty3.getName()).thenReturn("prop3");
        when(elementProperty3.getKey()).thenReturn("keyA");
    }

    @Test
    public void elementAclShouldPopulateClientApiElementAcl() {
        when(aclProvider.canUpdateElement(element, user)).thenReturn(true);
        when(aclProvider.canDeleteElement(element, user)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop1", user)).thenReturn(true);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop1", user)).thenReturn(false);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop1", user)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop2", user)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop2", user)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop2", user)).thenReturn(false);

        when(aclProvider.canAddProperty(element, "keyB", "prop2", user)).thenReturn(true);
        when(aclProvider.canUpdateProperty(element, "keyB", "prop2", user)).thenReturn(false);
        when(aclProvider.canDeleteProperty(element, "keyB", "prop2", user)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop3", user)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop3", user)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop3", user)).thenReturn(false);

        ClientApiElementAcl elementAcl = aclProvider.elementACL(element, user, ontologyRepository);

        assertThat(elementAcl.isAddable(), equalTo(true));
        assertThat(elementAcl.isUpdateable(), equalTo(true));
        assertThat(elementAcl.isDeleteable(), equalTo(true));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        assertThat(propertyAcls.size(), equalTo(4));

        ClientApiPropertyAcl propertyAcl = propertyAcls.get(0);
        assertThat(propertyAcl.getName(), equalTo("prop1"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(true));
        assertThat(propertyAcl.isUpdateable(), equalTo(false));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));

        propertyAcl = propertyAcls.get(1);
        assertThat(propertyAcl.getName(), equalTo("prop2"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(false));

        propertyAcl = propertyAcls.get(2);
        assertThat(propertyAcl.getName(), equalTo("prop2"));
        assertThat(propertyAcl.getKey(), equalTo("keyB"));
        assertThat(propertyAcl.isAddable(), equalTo(true));
        assertThat(propertyAcl.isUpdateable(), equalTo(false));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));

        propertyAcl = propertyAcls.get(3);
        assertThat(propertyAcl.getName(), equalTo("prop3"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(false));
    }
}
