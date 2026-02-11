package com.kvstore.common;

import org.json.JSONObject;

/**
 * Message wrapper for JSON communication between components
 */
public class Message {
    private final JSONObject json;

    public Message() {
        this.json = new JSONObject();
    }

    public Message(String jsonString) {
        this.json = new JSONObject(jsonString);
    }

    // Builder pattern for easy message creation
    public static Message ack(String message) {
        return new Message()
                .setReqType("ack")
                .setMessage(message);
    }

    public static Message data(String data) {
        return new Message()
                .setReqType("data")
                .setMessage(data);
    }

    public static Message request(String reqType, String key) {
        return new Message()
                .setReqType(reqType)
                .setKey(key);
    }

    public static Message request(String reqType, String key, String value) {
        return new Message()
                .setReqType(reqType)
                .setKey(key)
                .setValue(value);
    }

    // Setters (fluent API)
    public Message setReqType(String reqType) {
        json.put("req_type", reqType);
        return this;
    }

    public Message setKey(String key) {
        json.put("key", key);
        return this;
    }

    public Message setValue(String value) {
        json.put("value", value);
        return this;
    }

    public Message setMessage(String message) {
        json.put("message", message);
        return this;
    }

    public Message setId(String id) {
        json.put("id", id);
        return this;
    }

    public Message setTable(String table) {
        json.put("table", table);
        return this;
    }

    // Getters
    public String getReqType() {
        return json.optString("req_type", "");
    }

    public String getKey() {
        return json.optString("key", "");
    }

    public String getValue() {
        return json.optString("value", "");
    }

    public String getMessage() {
        return json.optString("message", "");
    }

    public String getId() {
        return json.optString("id", "");
    }

    public String getTable() {
        return json.optString("table", "");
    }

    @Override
    public String toString() {
        return json.toString();
    }
}
