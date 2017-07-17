package com.github.sunnysuperman.serverpub.loadbalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.slb.model.v20140515.AddBackendServersRequest;
import com.aliyuncs.slb.model.v20140515.AddBackendServersResponse;
import com.aliyuncs.slb.model.v20140515.DescribeHealthStatusRequest;
import com.aliyuncs.slb.model.v20140515.DescribeHealthStatusResponse;
import com.aliyuncs.slb.model.v20140515.DescribeLoadBalancerAttributeRequest;
import com.aliyuncs.slb.model.v20140515.DescribeLoadBalancerAttributeResponse;
import com.aliyuncs.slb.model.v20140515.RemoveBackendServersRequest;
import com.aliyuncs.slb.model.v20140515.RemoveBackendServersResponse;
import com.github.sunnysuperman.commons.utils.CollectionUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.serverpub.L;

public class LoadBalanceServiceAliImp implements LoadBalanceService {
    private IClientProfile profile;

    public LoadBalanceServiceAliImp(Map<?, ?> config) {
        profile = DefaultProfile.getProfile("cn-qingdao", config.get("key").toString(),
                config.get("secret").toString());
    }

    private IAcsClient getClient() {
        IAcsClient client = new DefaultAcsClient(profile);
        return client;
    }

    @Override
    public void addBackendServer(String loadBalancerId, String serverId) throws Exception {
        AddBackendServersRequest request = new AddBackendServersRequest();
        // request.setRegionId(regionId);
        request.setLoadBalancerId(loadBalancerId);

        Map<String, Object> server = CollectionUtil.arrayAsMap("ServerId", serverId);
        List<Map<String, Object>> servers = Collections.singletonList(server);
        request.setBackendServers(JSONUtil.toJSONString(servers));

        AddBackendServersResponse response = getClient().getAcsResponse(request);
        L.info("addBackendServer successfully: " + response.getRequestId().toString() + ", left servers: "
                + JSONUtil.toJSONString(response.getBackendServers()));
    }

    @Override
    public void removeBackendServer(String loadBalancerId, String serverId) throws Exception {
        RemoveBackendServersRequest request = new RemoveBackendServersRequest();
        request.setLoadBalancerId(loadBalancerId);

        request.setBackendServers(JSONUtil.toJSONString(Collections.singletonList(serverId)));

        RemoveBackendServersResponse response = getClient().getAcsResponse(request);
        L.info("removeBackendServer successfully: " + response.getRequestId().toString() + ", left servers: "
                + JSONUtil.toJSONString(response.getBackendServers()));
    }

    @Override
    public List<String> getBackendServers(String loadBalancerId) throws Exception {
        DescribeLoadBalancerAttributeRequest request = new DescribeLoadBalancerAttributeRequest();
        request.setLoadBalancerId(loadBalancerId);
        DescribeLoadBalancerAttributeResponse response = getClient().getAcsResponse(request);
        List<DescribeLoadBalancerAttributeResponse.BackendServer> servers = response.getBackendServers();
        if (CollectionUtil.isEmpty(servers)) {
            return new ArrayList<>(0);
        }
        List<String> serverIds = new ArrayList<>(servers.size());
        for (DescribeLoadBalancerAttributeResponse.BackendServer server : servers) {
            serverIds.add(server.getServerId());
        }
        return serverIds;
    }

    @Override
    public Set<String> getHealthyBackendServers(String loadBalancerId) throws Exception {
        DescribeHealthStatusRequest request = new DescribeHealthStatusRequest();
        request.setLoadBalancerId(loadBalancerId);
        DescribeHealthStatusResponse response = getClient().getAcsResponse(request);
        List<DescribeHealthStatusResponse.BackendServer> servers = response.getBackendServers();
        if (CollectionUtil.isEmpty(servers)) {
            return new HashSet<>(0);
        }
        Set<String> serverIds = new HashSet<String>();
        for (DescribeHealthStatusResponse.BackendServer server : servers) {
            if ("normal".equals(server.getServerHealthStatus())) {
                serverIds.add(server.getServerId());
            }
        }
        return serverIds;
    }
}
