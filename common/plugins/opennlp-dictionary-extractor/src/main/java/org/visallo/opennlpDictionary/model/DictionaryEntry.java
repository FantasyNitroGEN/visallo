package org.visallo.opennlpDictionary.model;

import org.visallo.core.util.RowKeyHelper;
import org.json.JSONObject;
import com.v5analytics.simpleorm.Entity;
import com.v5analytics.simpleorm.Field;
import com.v5analytics.simpleorm.Id;

@Entity(tableName = "dictionaryEntry")
public class DictionaryEntry {
    @Id
    private String id;

    @Field
    private String tokens;

    @Field
    private String concept;

    @Field
    private String resolvedName;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected DictionaryEntry() {

    }

    public DictionaryEntry(
            String tokens,
            String concept,
            String resolvedName
    ) {
        this.id = RowKeyHelper.build(tokens, concept);
        this.tokens = tokens;
        this.concept = concept;
        this.resolvedName = resolvedName;
    }

    public String getId() {
        return id;
    }

    public String getTokens() {
        return tokens;
    }

    public String getConcept() {
        return concept;
    }

    public String getResolvedName() {
        return resolvedName;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("rowKey", getId());
        json.put("concept", getConcept());
        json.put("tokens", getTokens());
        if (getResolvedName() != null) {
            json.put("resolvedName", getResolvedName());
        }
        return json;
    }

}
