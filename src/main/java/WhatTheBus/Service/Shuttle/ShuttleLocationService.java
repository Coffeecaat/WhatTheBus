package WhatTheBus.Service.Shuttle;

import WhatTheBus.DTO.Shuttle.ReceiveLocData;
import WhatTheBus.Entity.LocationHistory;
import WhatTheBus.Repository.LocationHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ShuttleLocationService {

    private final RedisTemplate<String, ReceiveLocData> redisTemplate;
    private final LocationHistoryRepository locationHistoryRepository;

    @Value("${SHUTTLE_LOCATION_KEY_PREFIX}")
    private String shuttleLocationKey;

    public ShuttleLocationService(RedisTemplate<String, ReceiveLocData> redisTemplate, LocationHistoryRepository locationHistoryRepository){
        this.redisTemplate = redisTemplate;
        this.locationHistoryRepository = locationHistoryRepository;
    }

    @Transactional
    public void processLocationUpdate(ReceiveLocData locationData){

        String key = shuttleLocationKey + locationData.getShuttleId();
        redisTemplate.opsForValue().set(key,locationData);

        LocationHistory history = LocationHistory.from(locationData);
        locationHistoryRepository.save(history);
    }

    @Transactional
    public ReceiveLocData getLatestLocation(String shuttleId){
        String key = shuttleLocationKey + shuttleId;
        return redisTemplate.opsForValue().get(key);
    }

    public List<ReceiveLocData> getAllActiveLocations() {
        String pattern = shuttleLocationKey + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<ReceiveLocData> locations = new ArrayList<>();
        for (String key : keys) {
            ReceiveLocData location = redisTemplate.opsForValue().get(key);
            if (location != null) {
                locations.add(location);
            }
        }

        return locations;
    }
}
