package WhatTheBus.Controller;


import WhatTheBus.DTO.Bus.BusResponse;
import WhatTheBus.Service.Bus.BusArrivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bus")
@Tag(name = "시내버스 도착정보", description = "GBUS API 기반 정류장별 도착 정보 조회 API")
public class BusController {
    private final BusArrivalService busArrivalService;

    public BusController(BusArrivalService busArrivalService) {
        this.busArrivalService = busArrivalService;
    }

    @Operation(
            summary = "정류장별 시내버스 도착정보 조회",
            description = """
                    단국대 주변 4개 정류장(죽전역, 치과병원, 인문관, 정문)에 대해  
                    24번 / 720-3번 버스의 도착예정시간, 잔여좌석 등을 반환합니다.  
                    Redis 캐시를 사용하여 1분 간격으로 업데이트합니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "정상적으로 조회됨",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BusResponse.class)
            )
    )
    @GetMapping("/arrivals")
    public ResponseEntity<BusResponse> arrivals() {
        BusResponse busResponse = busArrivalService.getArrivals();
        return ResponseEntity.ok(busResponse);
    }
}
