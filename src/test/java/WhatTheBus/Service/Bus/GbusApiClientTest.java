package WhatTheBus.Service.Bus;

import WhatTheBus.DTO.Bus.GbusResponse;
import WhatTheBus.Bus.GbusApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class GbusApiClientTest {

    @Test
    void testGetStationId(){

        // given
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

        GbusApiClient client = new GbusApiClient(mockRestTemplate);

        ReflectionTestUtils.setField(client, "apiBaseUrl",
                "https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalListv");
        ReflectionTestUtils.setField(client, "serviceKey", "TEST_KEY");

        String stationId = "228000331";

        // expected URL
        String expectedUrl = "https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalListv2"
                + "?serviceKey=TEST_KEY&stationId=228000331&format=json";

        // stub 응답
        GbusResponse mockResponse = new GbusResponse();
        when(mockRestTemplate.getForObject(anyString(),eq(GbusResponse.class)))
                .thenReturn(mockResponse);

        // when
        GbusResponse result = client.getStationId(stationId);

        assertThat(result).isNotNull();
    }
}
