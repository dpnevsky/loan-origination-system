package gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Tag(name = "Gateway controller", description = "Application gateway for routing requests to microservices")
@Slf4j
@RestController
@RequiredArgsConstructor
public class GatewayController {

    private final RestClient restClient;

    @Value("${url.deal}")
    private String urlDealService;

    @Value("${url.statement}")
    private String urlStatementService;

    @Operation(summary = "Route requests to Deal Service", description = "Routes any request starting with /deal to the Deal Service")
    @RequestMapping("/deal/**")
    public ResponseEntity<String> routeToDealService(HttpServletRequest request, @RequestParam(required = false) Map<String, String> queryParams,
                                                     @RequestBody(required = false) String body) {
        return routeRequest(request, queryParams, urlDealService, "deal", body);
    }

    @Operation(summary = "Route requests to Statement Service", description = "Routes any request starting with /statement to the Statement Service")
    @RequestMapping("/statement/**")
    public ResponseEntity<String> routeToStatementService(HttpServletRequest request, @RequestParam(required = false) Map<String, String> queryParams,
                                                          @RequestBody(required = false) String body) {
        return routeRequest(request, queryParams, urlStatementService, "statement", body);
    }

    private ResponseEntity<String> routeRequest(HttpServletRequest request, Map<String, String> queryParams, String serviceUrl, String serviceName, String body) {
        String requestMethod = request.getMethod();
        String requestPath = request.getRequestURI();
        String basePath = "/" + serviceName;

        if (requestPath.startsWith(basePath)) {
            requestPath = requestPath.substring(basePath.length());
        } else {
            log.error("Request path '{}' does not start with the expected base path '{}'", requestPath, basePath);
            throw new IllegalArgumentException("Invalid request path for " + serviceName);
        }

        StringBuilder urlBuilder = new StringBuilder(serviceUrl).append(requestPath);

        if (!queryParams.isEmpty()) {
            urlBuilder.append("?");
            queryParams.forEach((key, value) -> urlBuilder.append(key).append("=").append(value).append("&"));
            urlBuilder.setLength(urlBuilder.length() - 1);
        }

        String fullUrl = urlBuilder.toString();
        log.info("Routing {} request to {}: {}", requestMethod, serviceName, fullUrl);

        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(HttpMethod.valueOf(requestMethod))
                    .uri(fullUrl);

            if (body != null && !body.isEmpty()) {
                requestSpec.header("Content-Type", "application/json");
                requestSpec.body(body);
            }

            String response = requestSpec
                    .retrieve()
                    .body(String.class);

            log.info("Response received from {}", serviceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error while routing {} request to {}. Error type: {}", requestMethod, serviceName, e.getClass().getSimpleName());
            throw new RuntimeException("Failed to route " + requestMethod + " request to " + serviceName, e);
        }
    }
}
