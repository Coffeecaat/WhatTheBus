package WhatTheBus.bus;

import WhatTheBus.DTO.Bus.GbusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class GbusApiClient {

    private final RestTemplate restTemplate;

    @Value("${bus.api.service-key}")
    private String serviceKey;

    @Value("${bus.api.base-url}")
    private String apiBaseUrl;

    public GbusApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GbusResponse getStationId(String stationId) {
        String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .queryParam("serviceKey",serviceKey)
                .queryParam("stationId",stationId)
                .queryParam("format","json")
                .toUriString();

        try{
            log.debug("[GbusApiClient] GET {}", url);
            GbusResponse response = restTemplate.getForObject(url, GbusResponse.class);
            return response;
        } catch(Exception e){
            log.error("[GbusApiClient] 정류장 stationId={} 호출 실패. url={}", stationId, url, e);
            return null;
        }
    }
}
