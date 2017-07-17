package com.github.sunnysuperman.serverpub.loadbalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.github.sunnysuperman.commons.utils.CollectionUtil;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.serverpub.L;

public class LoadBalanceServiceAwsImp implements LoadBalanceService {
    private BasicAWSCredentials credential;

    public LoadBalanceServiceAwsImp(Map<?, ?> config) {
        credential = new BasicAWSCredentials(config.get("key").toString(), config.get("secret").toString());
    }

    public AmazonElasticLoadBalancingClient getClient(RegionAwareLoadBalancer loadBalancer) {
        AmazonElasticLoadBalancingClient client = new AmazonElasticLoadBalancingClient(credential);
        client.setRegion(loadBalancer.getRegion());
        return client;
    }

    private static class RegionAwareLoadBalancer {
        private Region region;
        private String name;

        public Region getRegion() {
            return region;
        }

        public void setRegion(Region region) {
            this.region = region;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    private RegionAwareLoadBalancer parseLoadBalancer(String id) {
        List<String> result = StringUtil.split(id, "/");
        RegionAwareLoadBalancer balancer = new RegionAwareLoadBalancer();
        balancer.setRegion(Region.getRegion(Regions.valueOf(result.get(0).toUpperCase().replaceAll("-", "_"))));
        balancer.setName(result.get(1));
        return balancer;
    }

    @Override
    public void addBackendServer(String loadBalancerId, String serverId) throws Exception {
        RegionAwareLoadBalancer loadBalancer = parseLoadBalancer(loadBalancerId);
        AmazonElasticLoadBalancingClient client = getClient(loadBalancer);

        RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest();
        request.setLoadBalancerName(loadBalancer.getName());
        request.setInstances(Collections.singletonList(new Instance(serverId)));

        client.registerInstancesWithLoadBalancer(request);
        L.info("addBackendServer successfully: " + serverId);
    }

    @Override
    public void removeBackendServer(String loadBalancerId, String serverId) throws Exception {
        RegionAwareLoadBalancer loadBalancer = parseLoadBalancer(loadBalancerId);
        AmazonElasticLoadBalancingClient client = getClient(loadBalancer);

        DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest();
        request.setLoadBalancerName(loadBalancer.getName());
        request.setInstances(Collections.singletonList(new Instance(serverId)));

        client.deregisterInstancesFromLoadBalancer(request);
        L.info("removeBackendServer successfully: " + serverId);
    }

    @Override
    public List<String> getBackendServers(String loadBalancerId) throws Exception {
        RegionAwareLoadBalancer loadBalancer = parseLoadBalancer(loadBalancerId);
        AmazonElasticLoadBalancingClient client = getClient(loadBalancer);

        DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest();
        request.setLoadBalancerName(loadBalancer.getName());

        DescribeInstanceHealthResult result = client.describeInstanceHealth(request);
        List<InstanceState> states = result.getInstanceStates();
        if (CollectionUtil.isEmpty(states)) {
            return new ArrayList<>(0);
        }
        List<String> servers = new ArrayList<String>(states.size());
        for (InstanceState state : states) {
            servers.add(state.getInstanceId());
        }
        return servers;
    }

    @Override
    public Set<String> getHealthyBackendServers(String loadBalancerId) throws Exception {
        RegionAwareLoadBalancer loadBalancer = parseLoadBalancer(loadBalancerId);
        AmazonElasticLoadBalancingClient client = getClient(loadBalancer);

        DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest();
        request.setLoadBalancerName(loadBalancer.getName());

        DescribeInstanceHealthResult result = client.describeInstanceHealth(request);
        List<InstanceState> states = result.getInstanceStates();
        if (CollectionUtil.isEmpty(states)) {
            return new HashSet<>(0);
        }
        Set<String> healthyServers = new HashSet<String>(states.size());
        for (InstanceState state : states) {
            if ("InService".equals(state.getState())) {
                healthyServers.add(state.getInstanceId());
            }
        }
        return healthyServers;
    }

}
