package WhatTheBus.Config;

import WhatTheBus.Service.DtlsServerService;
import WhatTheBus.Service.Shuttle.ShuttleLocationBusinessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.PskSecretResult;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedMultiPskStore;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.eclipse.californium.scandium.util.ServerNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.Base64;

@Configuration
public class DtlsConfig {

    private final Environment env;

    public DtlsConfig(Environment env) {
        this.env = env;
    }

    @Value("${app.dtls.port:5684}")
    private int dtlsPort;

    @Value("${app.dtls.enabled:true}")
    private boolean dtlsEnabled;

    @Bean
    public DTLSConnector dtlsConnector(AdvancedMultiPskStore pskStore) {
        org.eclipse.californium.elements.config.Configuration config =
                org.eclipse.californium.elements.config.Configuration.getStandard();

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(config)
                .setAddress(new InetSocketAddress(dtlsPort))
                .setAdvancedPskStore(pskStore);

        return new DTLSConnector(builder.build());
    }

    @Bean
    public AdvancedMultiPskStore pskStore() {
        return new AdvancedMultiPskStore() {
            @Override
            public boolean hasEcdhePskSupported() {
                return true;
            }

            @Override
            public PskSecretResult requestPskSecretResult(ConnectionId cid, ServerNames serverName,
                    PskPublicInformation identity, String hmacAlgorithm, SecretKey otherSecret, byte[] seed,
                    boolean useExtendedMasterSecret) {

                // Extract Pi device ID from PSK identity
                String deviceId = identity.getPublicInfoAsString();

                // Get PSK for this device ID
                byte[] psk = getPskForDevice(deviceId);

                if (psk != null) {
                    SecretKey secretKey = SecretUtil.create(psk, "PSK");
                    return new PskSecretResult(cid, identity, secretKey);
                }

                return new PskSecretResult(cid, identity, null);
            }

            @Override
            public PskPublicInformation getIdentity(InetSocketAddress peerAddress, ServerNames virtualHost) {
                return new PskPublicInformation("SERVER");
            }
        };
    }

    @Bean
    public DtlsServerService dtlsServerService(DTLSConnector dtlsConnector,
                                               ShuttleLocationBusinessService businessService,
                                               ObjectMapper objectMapper) {
        if (!dtlsEnabled) {
            return null;
        }
        return new DtlsServerService(dtlsConnector, businessService, objectMapper);
    }

    private byte[] getPskForDevice(String deviceId) {
        String propertyKey = "dtls.psk." + deviceId;
        String base64 = env.getProperty(propertyKey);
        if (base64 == null || base64.isBlank()) {
            return null; // 등록되지 않은 디바이스 → 연결 거부
        }
        return Base64.getDecoder().decode(base64);
    }

}
