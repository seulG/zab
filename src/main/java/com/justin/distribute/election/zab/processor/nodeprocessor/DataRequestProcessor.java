package com.justin.distribute.election.zab.processor.nodeprocessor;

import com.justin.distribute.election.zab.Node;
import com.justin.distribute.election.zab.NodeStatus;
import com.justin.distribute.election.zab.data.Data;
import com.justin.distribute.election.zab.data.ZxId;
import com.justin.distribute.election.zab.message.nodemsg.DataMessage;
import com.justin.net.remoting.common.Pair;
import com.justin.net.remoting.netty.NettyRequestProcessor;
import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;


public class DataRequestProcessor implements NettyRequestProcessor {
    private static final Logger logger = LogManager.getLogger(DataRequestProcessor.class.getSimpleName());

    private final Node node;

    public DataRequestProcessor(final Node node) {
        this.node = node;
    }

    /**
     * 数据同步消息处理器
     */
    @Override
    public RemotingMessage processRequest(ChannelHandlerContext ctx, RemotingMessage request) throws Exception {
        try {
            if (node.getDataLock().tryLock(3000, TimeUnit.MILLISECONDS)) {
                DataMessage dataMsg = DataMessage.getInstance().parseMessage(request);
                // 心跳消息
                if (dataMsg.getType() == DataMessage.Type.SYNC) {
                    logger.info("Receive heartbeat message: " + dataMsg);
                    node.getNodeConfig().setPreElectionTime(System.currentTimeMillis());
                    node.getNodeConfig().setPreHeartbeatTime(System.currentTimeMillis());
                    node.setStatus(NodeStatus.FOLLOWING);
                    node.setLeaderId(dataMsg.getNodeId());

                    Data lastData = node.getDataManager().readLastData();
                    if (lastData.getZxId().getEpoch() == 0) {
                        lastData.getZxId().setEpoch(node.getMyVote().getEpoch());
                    }
                    Data peerLastData = dataMsg.getData();

                    Data resData = new Data();
                    resData.setKv(new Pair<>("", ""));

                    // 日志同步
                    if (lastData.getZxId().compareTo(peerLastData.getZxId()) > 0) {
                        node.getDataManager().removeFromIndex(peerLastData.getZxId().getCounter() + 1);
                        resData.setZxId(peerLastData.getZxId());
                    } else if (lastData.getZxId().compareTo(peerLastData.getZxId()) < 0) {
                        long lastCounter = lastData.getZxId().getCounter();
                        lastCounter += 1;
                        if (lastCounter == peerLastData.getZxId().getCounter()) {
                            boolean flag = node.getDataManager().write(peerLastData);
                            if (flag) {
                                node.getDataManager().put(peerLastData.getKv().getObject1(), peerLastData.getKv().getObject2());
                            }
                        }
                        lastData.getZxId().setCounter(lastCounter);
                        resData.setZxId(lastData.getZxId());
                    } else if (lastData.getZxId().compareTo(peerLastData.getZxId()) == 0) {
                        resData.setZxId(lastData.getZxId());
                    }
                    dataMsg.setNodeId(node.getNodeConfig().getNodeId());
                    dataMsg.setData(resData);
                    dataMsg.setSuccess(true);
                    return dataMsg.response(request);
                    // 消息进入提议状态, 只写入缓存，不持久化，只有收到commit后，才会持久化
                } else if (dataMsg.getType() == DataMessage.Type.SNAPSHOT) {
                    Data snapshot = dataMsg.getData();
                    if (snapshot != null) {
                        boolean flag = node.getDataManager().put(snapshot.getKv().getObject1(), snapshot.getKv().getObject2());
                        dataMsg.setNodeId(node.getNodeConfig().getNodeId());
                        dataMsg.setSuccess(flag);
                        return dataMsg.response(request);
                    }
                    // 将缓存的消息持久化，并清除缓存
                } else if (dataMsg.getType() == DataMessage.Type.COMMIT) {
                    Data data = dataMsg.getData();
                    if (data != null) {
                        long lastIndex = node.getDataManager().getLastIndex();
                        if (lastIndex + 1 == data.getZxId().getCounter()) {
                            boolean flag = node.getDataManager().write(data);
                            if (flag) {
                                String value = node.getDataManager().get(data.getKv().getObject1());
                                if (value == null || value.equals("")) {
                                    node.getDataManager().put(data.getKv().getObject1(), data.getKv().getObject2());
                                }
                            }
                        }
                    }
                }
            }
            return null;
        } finally {
            node.getDataLock().unlock();
        }
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
