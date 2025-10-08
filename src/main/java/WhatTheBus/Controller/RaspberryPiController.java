package WhatTheBus.Controller;

import WhatTheBus.DTO.ReceiveLocData;
import WhatTheBus.Service.ShuttleLocationBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "라즈베리파이 API", description = "라즈베리파이 전용 셔틀 위치 업데이트 API")
@RestController
@RequestMapping("/api/pi")
public class RaspberryPiController {

    private final ShuttleLocationBusinessService businessService;

    public RaspberryPiController(ShuttleLocationBusinessService businessService) {
        this.businessService = businessService;
    }

    @Operation(summary = "셔틀 위치 업데이트", description = "라즈베리파이에서 셔틀의 현재 위치를 서버에 업데이트")
    @PostMapping("/location")
    public ResponseEntity<Void> updateShuttleLocation(@RequestBody ReceiveLocData locationData) {
        try {
            businessService.processLocationUpdate(locationData);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "라즈베리파이 상태 확인", description = "라즈베리파이 디바이스 상태 확인")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Pi connection OK");
    }
}