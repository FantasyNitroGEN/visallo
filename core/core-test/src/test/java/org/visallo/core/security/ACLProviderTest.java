package org.visallo.core.security;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.visallo.core.model.properties.VisalloProperties.COMMENT;

@RunWith(MockitoJUnitRunner.class)
public class ACLProviderTest {
    private static final String REGULAR_PROP_NAME = "regularPropName";
    private static final String REGULAR_PROP_KEY = "regularPropKey";
    private static final String COMMENT_PROP_KEY = "commentPropKey";
    private static final String COMMENT_PROP_NAME = COMMENT.getPropertyName();
    private static final Visibility VISIBILITY = Visibility.EMPTY;
    private static final VisibilityJson VISIBILITY_JSON = new VisibilityJson();

    @Mock private Graph graph;
    @Mock private OntologyRepository ontologyRepository;
    @Mock private UserRepository userRepository;
    @Mock private OntologyProperty ontologyProperty1;
    @Mock private OntologyProperty ontologyProperty2;
    @Mock private OntologyProperty ontologyProperty3;
    @Mock private OntologyProperty ontologyProperty4;
    @Mock private Concept vertexConcept;
    @Mock private Concept parentConcept;
    @Mock private Vertex vertex;
    @Mock private Edge edge;
    @Mock private Relationship edgeRelationship;
    @Mock private Property elementProperty1;
    @Mock private Property elementProperty2a;
    @Mock private Property elementProperty2b;
    @Mock private Property elementProperty3;
    @Mock private Property user1RegularProperty;
    @Mock private Property user1CommentProperty;
    @Mock private User user1;
    @Mock private User user2;
    @Mock private User userWithCommentEditAny;
    @Mock private User userWithCommentDeleteAny;

    private ACLProvider aclProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        aclProvider = spy(new MockAclProvider(graph, userRepository, ontologyRepository));

        when(user1.getUserId()).thenReturn("USER_1");
        when(user1.getPrivileges()).thenReturn(ImmutableSet.of(Privilege.EDIT, Privilege.COMMENT));

        when(user2.getUserId()).thenReturn("USER_2");
        when(user2.getPrivileges()).thenReturn(ImmutableSet.of(Privilege.EDIT, Privilege.COMMENT));

        when(userWithCommentEditAny.getUserId()).thenReturn("USER_WITH_COMMENT_EDIT_ANY");
        when(userWithCommentEditAny.getPrivileges())
                .thenReturn(ImmutableSet.of(Privilege.EDIT, Privilege.COMMENT_EDIT_ANY));

        when(userWithCommentDeleteAny.getUserId()).thenReturn("USER_WITH_COMMENT_DELETE_ANY");
        when(userWithCommentDeleteAny.getPrivileges())
                .thenReturn(ImmutableSet.of(Privilege.EDIT, Privilege.COMMENT_DELETE_ANY));

        when(ontologyRepository.getConceptByIRI("vertex")).thenReturn(vertexConcept);
        when(ontologyRepository.getConceptByIRI("parent")).thenReturn(parentConcept);
        when(ontologyRepository.getRelationshipByIRI("edge")).thenReturn(edgeRelationship);

        when(vertexConcept.getParentConceptIRI()).thenReturn("parent");
        when(vertexConcept.getProperties()).thenReturn(
                ImmutableList.of(ontologyProperty1, ontologyProperty2, ontologyProperty4));

        when(parentConcept.getParentConceptIRI()).thenReturn(null);
        when(parentConcept.getProperties()).thenReturn(ImmutableList.of(ontologyProperty3));

        when(edgeRelationship.getProperties()).thenReturn(
                ImmutableList.of(ontologyProperty1, ontologyProperty2, ontologyProperty3, ontologyProperty4));

        when(ontologyProperty1.getTitle()).thenReturn("prop1");
        when(ontologyProperty2.getTitle()).thenReturn("prop2");
        when(ontologyProperty3.getTitle()).thenReturn("prop3");
        when(ontologyProperty4.getTitle()).thenReturn("prop4");

        List<Property> allProperties = ImmutableList.of(
                elementProperty1, elementProperty2a, elementProperty2b, elementProperty3);

        when(vertex.getId()).thenReturn("VERTEX_1");
        when(vertex.getPropertyValue(VisalloProperties.CONCEPT_TYPE.getPropertyName())).thenReturn("vertex");
        when(vertex.getProperties("prop1")).thenReturn(ImmutableList.of(elementProperty1));
        when(vertex.getProperties("prop2")).thenReturn(ImmutableList.of(elementProperty2a, elementProperty2b));
        when(vertex.getProperties("prop3")).thenReturn(ImmutableList.of(elementProperty3));
        when(vertex.getProperties("prop4")).thenReturn(Collections.emptyList());
        when(vertex.getProperties()).thenReturn(Lists.newArrayList(allProperties));

        when(edge.getId()).thenReturn("EDGE_1");
        when(edge.getLabel()).thenReturn("edge");
        when(edge.getProperties("prop1")).thenReturn(ImmutableList.of(elementProperty1));
        when(edge.getProperties("prop2")).thenReturn(ImmutableList.of(elementProperty2a, elementProperty2b));
        when(edge.getProperties("prop3")).thenReturn(ImmutableList.of(elementProperty3));
        when(edge.getProperties("prop4")).thenReturn(Collections.emptyList());
        when(edge.getProperties()).thenReturn(Lists.newArrayList(allProperties));

        when(graph.getVertex(eq("VERTEX_1"), any(Authorizations.class))).thenReturn(vertex);
        when(graph.getEdge(eq("EDGE_1"), any(Authorizations.class))).thenReturn(edge);

        when(elementProperty1.getName()).thenReturn("prop1");
        when(elementProperty1.getKey()).thenReturn("keyA");

        when(elementProperty2a.getName()).thenReturn("prop2");
        when(elementProperty2a.getKey()).thenReturn("keyA");

        when(elementProperty2b.getName()).thenReturn("prop2");
        when(elementProperty2b.getKey()).thenReturn("keyB");

        when(elementProperty3.getName()).thenReturn("prop3");
        when(elementProperty3.getKey()).thenReturn("keyA");

        for (Property property : allProperties) {
            when(property.getMetadata()).thenReturn(new Metadata());
        }
    }

    @Test
    public void appendAclOnVertexShouldPopulateClientApiElementAcl() {
        appendAclShouldPopulateClientApiElementAcl(vertex);
    }

    @Test
    public void appendAclOnEdgeShouldPopulateClientApiElementAcl() {
        appendAclShouldPopulateClientApiElementAcl(edge);
    }

    @Test
    public void checkCanAddOrUpdatePropertyShouldNotThrowWhenUserUpdatesAccessibleRegularProperty() {
        setupForRegularPropertyTests();

        aclProvider.checkCanAddOrUpdateProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user1);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void checkCanAddOrUpdatePropertyShouldThrowWhenUserUpdatesInaccessibleRegularProperty() {
        setupForRegularPropertyTests();

        aclProvider.checkCanAddOrUpdateProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user2);
    }

    @Test
    public void checkCanDeletePropertyShouldNotThrowWhenUserDeletesAccessibleRegularProperty() {
        setupForRegularPropertyTests();

        aclProvider.checkCanDeleteProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user1);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void checkCanDeletePropertyShouldThrowWhenUserDeletesInaccessibleRegularProperty() {
        setupForRegularPropertyTests();

        aclProvider.checkCanDeleteProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user2);
    }

    @Test
    public void checkCanAddOrUpdatePropertyShouldNotThrowWhenUserUpdatesOwnComment() {
        setupForCommentPropertyTests();

        aclProvider.checkCanAddOrUpdateProperty(vertex, COMMENT_PROP_KEY, COMMENT.getPropertyName(), user1);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void checkCanAddOrUpdatePropertyShouldThrowWhenUserUpdatesAnotherUsersComment() {
        setupForCommentPropertyTests();

        aclProvider.checkCanAddOrUpdateProperty(vertex, COMMENT_PROP_KEY, COMMENT.getPropertyName(), user2);
    }

    @Test
    public void checkCanAddOrUpdatePropertyShouldNotThrowWhenPrivilegedUserUpdatesAnotherUsersComment() {
        setupForCommentPropertyTests();

        aclProvider.checkCanAddOrUpdateProperty(
                vertex, COMMENT_PROP_KEY, COMMENT.getPropertyName(), userWithCommentEditAny);
    }

    @Test
    public void checkCanDeletePropertyShouldNotThrowWhenUserDeletesOwnComment() {
        setupForCommentPropertyTests();

        aclProvider.checkCanDeleteProperty(vertex, COMMENT_PROP_KEY, COMMENT.getPropertyName(), user1);
    }

    @Test(expected = VisalloAccessDeniedException.class)
    public void checkCanDeletePropertyShouldThrowWhenUserDeletesAnotherUsersComment() {
        setupForCommentPropertyTests();

        aclProvider.checkCanDeleteProperty(vertex, COMMENT_PROP_KEY, COMMENT.getPropertyName(), user2);
    }

    @Test
    public void checkCanDeletePropertyShouldNotThrowWhenPrivilegedUserDeletesAnotherUsersComment() {
        setupForCommentPropertyTests();

        aclProvider.checkCanDeleteProperty(
                vertex, COMMENT_PROP_KEY, COMMENT.getPropertyName(), userWithCommentDeleteAny);
    }

    private void setupForCommentPropertyTests() {
        // user1 and user2 can both add/update/delete the comment property

        Metadata user1CommentMetadata = new PropertyMetadata(user1, VISIBILITY_JSON, VISIBILITY).createMetadata();
        when(user1CommentProperty.getMetadata()).thenReturn(user1CommentMetadata);
        when(vertex.getProperty(COMMENT_PROP_KEY, COMMENT.getPropertyName())).thenReturn(user1CommentProperty);

        when(aclProvider.canUpdateElement(eq(vertex), any(User.class)))
                .thenReturn(true);
        when(aclProvider.canUpdateProperty(eq(vertex), eq(COMMENT_PROP_KEY), eq(COMMENT_PROP_NAME), any(User.class)))
                .thenReturn(true);
        when(aclProvider.canAddProperty(eq(vertex), eq(COMMENT_PROP_KEY), eq(COMMENT_PROP_NAME), any(User.class)))
                .thenReturn(true);
        when(aclProvider.canDeleteProperty(eq(vertex), eq(COMMENT_PROP_KEY), eq(COMMENT_PROP_NAME), any(User.class)))
                .thenReturn(true);
    }

    private void setupForRegularPropertyTests() {
        // only user1 can add/update/delete the regular property

        Metadata user1PropertyMetadata = new PropertyMetadata(user1, VISIBILITY_JSON, VISIBILITY).createMetadata();
        when(user1RegularProperty.getMetadata()).thenReturn(user1PropertyMetadata);
        when(vertex.getProperty(REGULAR_PROP_KEY, REGULAR_PROP_NAME)).thenReturn(user1RegularProperty);

        when(aclProvider.canUpdateElement(vertex, user1)).thenReturn(true);
        when(aclProvider.canUpdateProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user1)).thenReturn(true);
        when(aclProvider.canAddProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user1)).thenReturn(true);
        when(aclProvider.canDeleteProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user1)).thenReturn(true);

        when(aclProvider.canUpdateElement(vertex, user2)).thenReturn(true);
        when(aclProvider.canUpdateProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user2)).thenReturn(false);
        when(aclProvider.canAddProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user2)).thenReturn(false);
        when(aclProvider.canDeleteProperty(vertex, REGULAR_PROP_KEY, REGULAR_PROP_NAME, user2)).thenReturn(false);
    }

    private void appendAclShouldPopulateClientApiElementAcl(Element element) {
        when(aclProvider.canUpdateElement(element, user1)).thenReturn(true);
        when(aclProvider.canDeleteElement(element, user1)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop1", user1)).thenReturn(true);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop1", user1)).thenReturn(false);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop1", user1)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop2", user1)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop2", user1)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop2", user1)).thenReturn(false);

        when(aclProvider.canAddProperty(element, "keyB", "prop2", user1)).thenReturn(true);
        when(aclProvider.canUpdateProperty(element, "keyB", "prop2", user1)).thenReturn(false);
        when(aclProvider.canDeleteProperty(element, "keyB", "prop2", user1)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop3", user1)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop3", user1)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop3", user1)).thenReturn(false);

        when(aclProvider.canAddProperty(element, null, "prop4", user1)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, null, "prop4", user1)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, null, "prop4", user1)).thenReturn(true);

        ClientApiElement apiElement = null;
        if (element instanceof Vertex) {
            apiElement = ClientApiConverter.toClientApiVertex((Vertex) element, null, null);
        } else if (element instanceof Edge) {
            apiElement = ClientApiConverter.toClientApiEdge((Edge) element, null);
        }

        apiElement = (ClientApiElement) aclProvider.appendACL(apiElement, user1);

        ClientApiElementAcl elementAcl = apiElement.getAcl();

        assertThat(elementAcl.isAddable(), equalTo(true));
        assertThat(elementAcl.isUpdateable(), equalTo(true));
        assertThat(elementAcl.isDeleteable(), equalTo(true));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        assertThat(propertyAcls.size(), equalTo(5));

        ClientApiPropertyAcl propertyAcl = findSinglePropertyAcl(propertyAcls, "prop1");
        assertThat(propertyAcl.getName(), equalTo("prop1"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(true));
        assertThat(propertyAcl.isUpdateable(), equalTo(false));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));

        propertyAcl = findMultiplePropertyAcls(propertyAcls, "prop2").get(0);
        assertThat(propertyAcl.getName(), equalTo("prop2"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(false));

        propertyAcl = findMultiplePropertyAcls(propertyAcls, "prop2").get(1);
        assertThat(propertyAcl.getName(), equalTo("prop2"));
        assertThat(propertyAcl.getKey(), equalTo("keyB"));
        assertThat(propertyAcl.isAddable(), equalTo(true));
        assertThat(propertyAcl.isUpdateable(), equalTo(false));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));

        propertyAcl = findSinglePropertyAcl(propertyAcls, "prop3");
        assertThat(propertyAcl.getName(), equalTo("prop3"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(false));

        propertyAcl = findSinglePropertyAcl(propertyAcls, "prop4");
        assertThat(propertyAcl.getName(), equalTo("prop4"));
        assertThat(propertyAcl.getKey(), nullValue());
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));
    }

    private List<ClientApiPropertyAcl> findMultiplePropertyAcls(
            List<ClientApiPropertyAcl> propertyAcls, String propertyName) {
        return propertyAcls.stream().filter(pa -> pa.getName().equals(propertyName)).collect(Collectors.toList());
    }

    private ClientApiPropertyAcl findSinglePropertyAcl(List<ClientApiPropertyAcl> propertyAcls, String propertyName) {
        List<ClientApiPropertyAcl> matches = findMultiplePropertyAcls(propertyAcls, propertyName);
        assertThat(matches.size(), equalTo(1));
        return matches.get(0);
    }

    private static class MockAclProvider extends ACLProvider {

        protected MockAclProvider(Graph graph, UserRepository userRepository, OntologyRepository ontologyRepository) {
            super(graph, userRepository, ontologyRepository);
        }

        @Override
        public boolean canDeleteElement(Element element, User user) {
            return false;
        }

        @Override
        public boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
            return false;
        }

        @Override
        public boolean canUpdateElement(Element element, User user) {
            return false;
        }

        @Override
        public boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
            return false;
        }

        @Override
        public boolean canAddProperty(Element element, String propertyKey, String propertyName, User user) {
            return false;
        }
    }
}
