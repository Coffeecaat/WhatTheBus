package WhatTheBus.Controller;

import WhatTheBus.DTO.Response.ErrorResponse;
import WhatTheBus.DTO.SendLocData;
import WhatTheBus.Service.ShuttleLocationBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name= "셔틀 위치 API", description = "셔틀 위치 업데이트 및 조회 API")
@RestController
@RequestMapping("/api/shuttle")
public class                                                                                   ShuttleController {

    private final ShuttleLocationBusinessService businessService;

    public ShuttleController(ShuttleLocationBusinessService businessService) {
        this.businessService = businessService;
    }

    @Operation(summary = "특정 셔틀 최신 위치 조회", description = "특정 셔틀ID의 가장 최근 위치 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 위치 정보를 반환",
                    content = @Content(schema = @Schema(implementation = SendLocData.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 셔틀 ID", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 셔틀의 위치가 없음", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    @GetMapping("/{shuttleId}/location")
    public ResponseEntity<SendLocData> getShuttleLocation(
            @Parameter(description = "조회할 셔틀의 고유 ID", example = "SHUTTLE_01")
            @PathVariable String shuttleId) {
        try {
            SendLocData location = businessService.getShuttleLocationForClient(shuttleId);
            return ResponseEntity.ok(location);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "모든 셔틀 최신 위치 목록", description = "현재 운행 중인 모든 셔틀의 최신 위치를 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 운행중인 셔틀 위치 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SendLocData.class)))),
            @ApiResponse(responseCode = "204", description = "운행 중인 셔틀 없음", content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/locations")
    public ResponseEntity<?> getAllShuttleLocations() {
        try {
            List<SendLocData> locations = businessService.getAllActiveShuttleLocations();

            if (locations.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Result-Message", "현재 운행 중인 셔틀이 없습니다.");
                return ResponseEntity.status(HttpStatus.NO_CONTENT).headers(headers).build();
            }

            return ResponseEntity.ok(locations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "셔틀 위치 정보를 조회하지 못했습니다."));
        }
    }
}
