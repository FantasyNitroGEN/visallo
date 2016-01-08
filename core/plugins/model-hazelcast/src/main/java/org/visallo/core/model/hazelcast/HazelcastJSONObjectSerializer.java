package org.visallo.core.model.hazelcast;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import org.json.JSONObject;

import java.io.IOException;

public class HazelcastJSONObjectSerializer implements StreamSerializer<JSONObject> {
    @Override
    public void write(ObjectDataOutput out, JSONObject object) throws IOException {
        out.writeUTF(object.toString());
    }

    @Override
    public JSONObject read(ObjectDataInput in) throws IOException {
        String jsonString = in.readUTF();
        return new JSONObject(jsonString);
    }

    @Override
    public int getTypeId() {
        return 1;
    }

    @Override
    public void destroy() {

    }
}
