package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.BusResponse;
import WhatTheBus.DTO.Bus.BusStopData;
import WhatTheBus.DTO.Bus.GbusResponse;
import WhatTheBus.DTO.Bus.ReceiveBusData;
import WhatTheBus.bus.CampusStopCode;
import WhatTheBus.bus.CampusStopMapping;
import WhatTheBus.bus.GbusApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusArrivalService {

    private final GbusApiClient gbusApiClient;
    private final RedisTemplate<String, BusStopData> busStopDataRedisTemplate;

    @Value("${BUS_KEY_PREFIX}")
    private String busKeyPrefix;

    private static final List<String> TARGET_ROUTES = List.of("24","720-3");

    private String getKeyForStop(CampusStopCode stopCode){
        return busKeyPrefix + stopCode.name();
    }

    @Scheduled(fixedDelay = 40000L)
    public void stopsRefresh(){

        for(CampusStopCode stopCode : CampusStopCode.values()){
            String stationId = CampusStopMapping.STATION_ID_MAP.get(stopCode);

            GbusResponse gbusResponse = gbusApiClient.getStationId(stationId);

            List<GbusResponse.BusInfo> items =
                    gbusResponse != null &&
                            gbusResponse.getResponse() != null &&
                            gbusResponse.getResponse().getMsgBody() != null
                    ? gbusResponse.getResponse().getMsgBody().getBusArrivalList()
                    : List.of();

            List<ReceiveBusData> buses = items.stream()
                    .filter(item -> TARGET_ROUTES.contains(item.getRouteName()))
                    .map(item -> {
                        ReceiveBusData dto = new ReceiveBusData();
                        dto.setRouteName(item.getRouteName());
                        dto.setMinutesLeft(item.getPredictTime1());
                        dto.setRemainingSeats(item.getRemainSeatCnt1());
                        return dto;
                    })
                    .toList();

            BusStopData stopData = new BusStopData();
            stopData.setStopCode(stopCode);
            stopData.setStopName(CampusStopMapping.STATION_NAME_MAP.get(stopCode));
            stopData.setBuses(buses);

            String key = getKeyForStop(stopCode);
            busStopDataRedisTemplate.opsForValue().set(key, stopData);
        }
    }

    public BusResponse getArrivals(){
        List<BusStopData> stops = new ArrayList<>();

        for(CampusStopCode stopCode : CampusStopCode.values()){
            String key = getKeyForStop(stopCode);
            BusStopData stopData = busStopDataRedisTemplate.opsForValue().get(key);
            if(stopData != null){
                stops.add(stopData);
            }
        }

        BusResponse busResponse = new BusResponse();
        busResponse.setUpdatedAt(LocalDateTime.now());
        busResponse.setStops(stops);
        return busResponse;
    }

}
