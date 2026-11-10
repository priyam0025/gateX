package com.jeezus.gatex.gateway.service;

import com.jeezus.gatex.gateway.client.ManagementApiClient;
import com.jeezus.gatex.gateway.dto.GatewayRoute;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.RouteMatcher;

import java.util.List;

@Component
public class GatewayConfigurationLoader implements ApplicationRunner {

    private final ManagementApiClient managementApiClient;
    private final RouteRegistry routeRegistry;

    public GatewayConfigurationLoader(ManagementApiClient managementApiClient, RouteRegistry routeRegistry) {
        this.managementApiClient = managementApiClient;
        this.routeRegistry = routeRegistry;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        refresh();
    }

    public void refresh() {
        List<GatewayRoute> routes = managementApiClient.fetchActiveRoutes();
        routeRegistry.replaceAll(routes);


//        //demo
//        for(GatewayRoute route : routes) {
//            System.out.println("Route : " + route.getTargetPathPrefix());
//        }
    }
}
