package com.github.sunnysuperman.serverpub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.sunnysuperman.commons.bean.Bean;
import com.github.sunnysuperman.commons.utils.FormatUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.serverpub.ansible.Ansible;
import com.github.sunnysuperman.serverpub.loadbalance.BackendServer;
import com.github.sunnysuperman.serverpub.loadbalance.LoadBalanceService;
import com.github.sunnysuperman.serverpub.loadbalance.LoadBalanceServiceFactory;

public class ServerPublish {

    private static int publishServer(BackendServer server, Map<String, String> vars) throws Exception {
        Map<String, String> cfg = new HashMap<>(vars);
        cfg.put("host", FormatUtil.parseString(server.getIp()));
        return Ansible.execute(cfg);
    }

    public static int publishServers(Map<String, String> vars) throws Exception {
        L.info("PublishServers vars: " + JSONUtil.stringify(vars));

        String lbType = vars.get("loadbalancer_type").toString();
        Map<String, Object> lbConfig = JSONUtil
                .parseJSONObject(FormatUtil.parseString(vars.get("loadbalancer_config")));
        List<BackendServer> updateServers = Bean.fromJson(vars.get("loadbalancer_servers").toString(), BackendServer.class);
        LoadBalanceService lbService = null;
        if (!lbType.equals("none")) {
            lbService = LoadBalanceServiceFactory.getInstance(lbType, lbConfig);
        }
        int ret;
        if (lbService == null) {
            for (BackendServer server : updateServers) {
                ret = publishServer(server, vars);
                if (ret != 0) {
                    return ret;
                }
            }
        } else {
            List<?> ids = (List<?>) lbConfig.get("ids");
            String[] loadbalancers = ids.toArray(new String[ids.size()]);
            LinkedList<BackendServer> safeUpdateServers = new LinkedList<BackendServer>();
            Set<String> updateServerIds = new HashSet<>(updateServers.size());
            {
                Set<String> healthyServers = lbService.getHealthyBackendServers(loadbalancers[0]);
                for (BackendServer server : updateServers) {
                    updateServerIds.add(server.getId());
                    // 把不健康(或者新加入)的实例放到前面更新
                    if (!healthyServers.contains(server.getId())) {
                        safeUpdateServers.addFirst(server);
                    } else {
                        safeUpdateServers.addLast(server);
                    }
                }
            }
            for (BackendServer server : safeUpdateServers) {
                // remove backend server
                for (String loadbalancer : loadbalancers) {
                    while (true) {
                        try {
                            L.info("RemoveBackendServer " + server.getId() + " from loadbalancer " + loadbalancer);
                            lbService.removeBackendServer(loadbalancer, server.getId());
                            break;
                        } catch (Exception e) {
                            L.error(e);
                            U.sleep(10);
                        }
                    }
                }

                // publish
                ret = publishServer(server, vars);
                if (ret != 0) {
                    return ret;
                }

                // add backend server
                for (String loadbalancer : loadbalancers) {
                    while (true) {
                        try {
                            L.info("AddBackendServer " + server.getId() + " to loadbalancer " + loadbalancer);
                            lbService.addBackendServer(loadbalancer, server.getId());
                            while (!lbService.getHealthyBackendServers(loadbalancer).contains(server.getId())) {
                                L.info("Wait for server {" + server.toString() + "} restarts in loadbalancer "
                                        + loadbalancer);
                                U.sleep(1000);
                            }
                            break;
                        } catch (Exception e) {
                            L.error(e);
                        }
                    }
                }
            }

            // remove legacy servers
            for (String loadbalancer : loadbalancers) {
                List<String> backendServerIds;
                while (true) {
                    try {
                        backendServerIds = lbService.getBackendServers(loadbalancer);
                        break;
                    } catch (Exception e) {
                        L.error(e);
                        U.sleep(10);
                    }
                }
                for (String serverId : backendServerIds) {
                    if (updateServerIds.contains(serverId)) {
                        continue;
                    }
                    while (true) {
                        try {
                            L.info("Remove Legacy BackendServer " + serverId + " from loadbalancer " + loadbalancer);
                            lbService.removeBackendServer(loadbalancer, serverId);
                            break;
                        } catch (Exception e) {
                            L.error(e);
                            U.sleep(10);
                        }
                    }
                }
            }
        }

        return 0;
    }

}
