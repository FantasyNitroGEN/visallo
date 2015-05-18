package org.visallo.core.model.audit;

import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.RowKeyHelper;
import org.visallo.web.clientapi.model.UserType;
import org.json.JSONObject;
import com.v5analytics.simpleorm.Entity;
import com.v5analytics.simpleorm.EntitySubTypes;
import com.v5analytics.simpleorm.Field;
import com.v5analytics.simpleorm.Id;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Entity(tableName = "audit")
@EntitySubTypes(
        discriminatorColumnName = "type",
        types = {
                @EntitySubTypes.Type(value = AuditProperty.class, name = OntologyRepository.TYPE_PROPERTY),
                @EntitySubTypes.Type(value = AuditRelationship.class, name = OntologyRepository.TYPE_RELATIONSHIP),
                @EntitySubTypes.Type(value = AuditEntity.class, name = OntologyRepository.ENTITY_CONCEPT_IRI)
        }
)
public abstract class Audit {
    @Id
    private String id;

    @Field
    private UserType actorType;

    @Field
    private String userId;

    @Field
    private String userName;

    @Field
    private String displayName;

    @Field
    private AuditAction action;

    @Field
    private String comment;

    @Field
    private String process;

    @Field
    private String type;

    protected Audit() {

    }

    protected Audit(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public UserType getActorType() {
        return actorType;
    }

    public Audit setActorType(UserType actorType) {
        this.actorType = actorType;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public Audit setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public Audit setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Audit setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public AuditAction getAction() {
        return action;
    }

    public Audit setAction(AuditAction action) {
        this.action = action;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public Audit setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getProcess() {
        return process;
    }

    public Audit setProcess(String process) {
        this.process = process;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public Audit setUser(User user) {
        setUserId(user.getUserId());
        setUserName(user.getUsername());
        setDisplayName(user.getDisplayName());
        setActorType(user.getUserType());
        return this;
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            String[] rowKey = RowKeyHelper.splitOnMinorFieldSeparator(getId());
            json.put("graphVertexID", rowKey[0]);
            json.put("dateTime", getDateFormat().parse(rowKey[1]).getTime());

            JSONObject dataJson = new JSONObject();
            dataJson.put("actorType", getActorType());
            dataJson.put("userId", getUserId());
            dataJson.put("userName", getUserName());
            dataJson.put("displayName", getDisplayName());
            dataJson.put("action", getAction());
            dataJson.put("type", getType());
            dataJson.put("comment", getComment());
            dataJson.put("process", getProcess());
            json.put("data", dataJson);

            return json;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SimpleDateFormat getDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }
}
