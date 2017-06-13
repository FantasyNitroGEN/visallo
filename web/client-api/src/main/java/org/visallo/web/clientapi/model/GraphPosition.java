package org.visallo.web.clientapi.model;

import org.json.JSONObject;

public class GraphPosition {
    private int x;
    private int y;

    public GraphPosition(JSONObject position) {
        this(position.optInt("x", 0), position.optInt("y", 0));
    }

    public GraphPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void add(GraphPosition graphPosition) {
        this.x = x + graphPosition.getX();
        this.y = y + graphPosition.getY();
    }

    public void subtract(GraphPosition graphPosition) {
        this.x = x - graphPosition.getX();
        this.y = y - graphPosition.getY();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GraphPosition that = (GraphPosition) o;

        if (x != that.x) {
            return false;
        }
        if (y != that.y) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "GraphPosition{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("x", x);
        json.put("y", y);
        return json;
    }
}
