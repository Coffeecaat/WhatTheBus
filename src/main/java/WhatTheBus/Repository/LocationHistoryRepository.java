package WhatTheBus.Repository;

import WhatTheBus.Entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {

}
