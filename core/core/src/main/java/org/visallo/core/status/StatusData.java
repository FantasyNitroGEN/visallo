package org.visallo.core.status;

import org.visallo.core.exception.VisalloException;

import java.io.*;

public class StatusData implements Serializable {
    private static final long serialVersionUID = -8820411686759963863L;
    private final String url;
    private final String hostName;
    private final String hostAddress;

    public StatusData(String url, String hostName, String hostAddress) {
        this.url = url;
        this.hostName = hostName;
        this.hostAddress = hostAddress;
    }

    public StatusData(byte[] rawData) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(rawData));
            this.url = in.readUTF();
            this.hostName = in.readUTF();
            this.hostAddress = in.readUTF();
        } catch (IOException ex) {
            throw new VisalloException("Could not parse data", ex);
        }
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF(this.url);
            out.writeUTF(this.hostName);
            out.writeUTF(this.hostAddress);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new VisalloException("Could not write data", ex);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostAddress() {
        return hostAddress;
    }
}
