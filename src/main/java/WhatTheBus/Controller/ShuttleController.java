package WhatTheBus.Controller;

import WhatTheBus.Service.RateLimitService;
import WhatTheBus.DTO.ReceiveLocData;
import WhatTheBus.DTO.SendLocData;
import WhatTheBus.Security.JwtService;
import WhatTheBus.Service.ShuttleLocationBusinessService;
import WhatTheBus.Service.ShuttleLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Tag(name= "셔틀 위치 API", description = "셔틀 위치 업데이트 및 조회 API")
@RestController
@RequestMapping("/api/shuttle")
public class ShuttleController {

    private final ShuttleLocationBusinessService businessService;
    private final RateLimitService rateLimitService;
    private final JwtService jwtService;

    public ShuttleController(ShuttleLocationBusinessService businessService,
                           RateLimitService rateLimitService,
                           JwtService jwtService
    ) {
        this.businessService = businessService;
        this.rateLimitService = rateLimitService;
        this.jwtService = jwtService;
    }


    @Operation(summary= "특정 셔틀 최신 위치 조회", description="특정 셔틀ID의 가장 최근 위치 조회")
    @GetMapping("/{shuttleId}/location")
    public ResponseEntity<SendLocData> getShuttleLocation(
            @Parameter(description= "조회할 셔틀의 고유 ID", example= "SHUTTLE_01")
            @PathVariable String shuttleId,
            HttpServletRequest request){

        // Extract JWT token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String jwtToken = authHeader.substring(7);

        // Validate JWT token
        if (!jwtService.isTokenValid(jwtToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract hashed client ID for rate limiting
        String hashedClientId = jwtService.extractHashedClientId(jwtToken);
        if (!rateLimitService.isAllowed(hashedClientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            SendLocData location = businessService.getShuttleLocationForClient(shuttleId);
            return ResponseEntity.ok().body(location);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}

