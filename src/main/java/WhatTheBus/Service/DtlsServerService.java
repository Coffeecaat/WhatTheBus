package WhatTheBus.Service;

import WhatTheBus.DTO.Shuttle.ReceiveLocData;
import WhatTheBus.Service.Shuttle.ShuttleLocationBusinessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.scandium.DTLSConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DtlsServerService implements SmartLifecycle, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DtlsServerService.class);

    private final DTLSConnector dtlsConnector;
    private final ShuttleLocationBusinessService businessService;
    private final ObjectMapper objectMapper;

    private volatile boolean running = false;

    public DtlsServerService(DTLSConnector dtlsConnector,
                             ShuttleLocationBusinessService businessService,
                             ObjectMapper objectMapper) {
        this.dtlsConnector = dtlsConnector;
        this.businessService = businessService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        dtlsConnector.setRawDataReceiver(this::handleIncomingPacket);
        try {
            dtlsConnector.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        running = true;
        log.info("DTLS connector started on {}", dtlsConnector.getAddress());
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        dtlsConnector.stop();
        running = false;
        log.info("DTLS connector stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private void handleIncomingPacket(RawData rawData) {
        try {
            ReceiveLocData payload = objectMapper.readValue(rawData.getBytes(), ReceiveLocData.class);
            businessService.processLocationUpdate(payload);
            log.debug("Processed location update from {}", payload.getShuttleId());
        } catch (IOException e) {
            log.warn("Failed to deserialize DTLS payload from {}", rawData.getInetSocketAddress(), e);
        } catch (Exception e) {
            log.error("Failed to handle DTLS payload from {}", rawData.getInetSocketAddress(), e);
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
