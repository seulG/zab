package com.justin.distribute.election.zab.message.nodemgrmsg;

import com.justin.distribute.election.zab.message.AbstractMessage;
import com.justin.distribute.election.zab.message.MessageType;


public class JoinGroupMessage extends AbstractMessage<JoinGroupMessage> {
    private int nodeId;
    private String host;
    private int port;
    private int nodeMgrPort;

    private Boolean success;

    private JoinGroupMessage() {}

    public static JoinGroupMessage getInstance() {
        return new JoinGroupMessage();
    }

    @Override
    public int getMessageType() {
        return MessageType.JOIN_GROUP;
    }

    @Override
    public String toString() {
        return "JoinGroupMessage: [" +
                " nodeId=" + nodeId +
                " host=" + host +
                " port=" + port +
                " nodeMgrPort=" + nodeMgrPort +
                "]";
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNodeMgrPort() {
        return nodeMgrPort;
    }

    public void setNodeMgrPort(int nodeMgrPort) {
        this.nodeMgrPort = nodeMgrPort;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
