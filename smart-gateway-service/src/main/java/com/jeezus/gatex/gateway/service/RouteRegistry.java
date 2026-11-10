package com.jeezus.gatex.gateway.service;

import com.jeezus.gatex.gateway.model.CompiledRoute;
import com.jeezus.gatex.gateway.dto.GatewayRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class RouteRegistry {
    private final AtomicReference <List<CompiledRoute>> routes =
            new AtomicReference<>(Collections.emptyList());

    private final PathPatternParser parser;

    public void replaceAll(List<GatewayRoute> gatewayRoutes) {
        List<CompiledRoute> compiledRoutes = new ArrayList<>();

        if (gatewayRoutes != null) {
            for (GatewayRoute route : gatewayRoutes) {
                PathPattern pattern = parser.parse(route.getPathPattern());
                compiledRoutes.add(new CompiledRoute(route, pattern, sourcePrefix(route.getPathPattern())));
            }
        }
        compiledRoutes.sort(new Comparator<CompiledRoute>() {
            @Override
            public int compare(CompiledRoute o1, CompiledRoute o2) {
                return PathPattern.SPECIFICITY_COMPARATOR.compare(
                        o1.getPathPattern(), o2.getPathPattern()
                );
            }
        });
        routes.set(Collections.unmodifiableList(compiledRoutes));
    }

    public Optional<CompiledRoute> find(String requestPath) {
        // convert the path into PathContainer to compare with the list of path patterns
        PathContainer path = PathContainer.parsePath(requestPath);

        for (CompiledRoute route : routes.get()) { // .get() to get the atomic reference ie routes
            if (route.getPathPattern().matches(path)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    private String sourcePrefix(String pathPattern) {
        if (pathPattern.endsWith("/**")) {
            return pathPattern.substring(0,pathPattern.length() - 3);
        }
        return pathPattern;
    }


}
