package com.github.sunnysuperman.serverpub.loadbalance;

import java.util.List;
import java.util.Set;

public interface LoadBalanceService {

    void addBackendServer(String loadBalancerId, String serverId) throws Exception;

    void removeBackendServer(String loadBalancerId, String serverId) throws Exception;

    List<String> getBackendServers(String loadBalancerId) throws Exception;

    Set<String> getHealthyBackendServers(String loadBalancerId) throws Exception;
}
