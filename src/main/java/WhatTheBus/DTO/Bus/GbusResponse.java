package WhatTheBus.DTO.Bus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Data
public class GbusResponse {

    @JsonProperty("response")
    private ResponseBody response;

    @Data
    public static class ResponseBody {

        @JsonProperty("msgBody")
        private MsgBody msgBody;
    }

    @Data
    public static class MsgBody {
        @JsonProperty("busArrivalList")
        private List<BusInfo> busArrivalList;
    }

    @Data
    public static class BusInfo {

        @JsonProperty("routeId")
        private String routeId;

        @JsonProperty("routeName")
        private String routeName;

        @JsonProperty("stationId")
        private String stationId;

        @JsonProperty("staOrder")
        private Integer staOrder;

        @JsonProperty("predictTime1")
        private Integer predictTime1;

        @JsonProperty("remainSeatCnt1")
        private Integer remainSeatCnt1;
    }
}
