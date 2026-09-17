package com.example.keycloak.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lives in the same package as the mapper so the {@code protected setClaim} can be called
 * directly, without a test-only subclass.
 */
@ExtendWith(MockitoExtension.class)
class RequestIpAddressMapperTest {

    private static final String CLAIM_NAME = "ip_address";
    private static final String REQUEST_IP = "203.0.113.42";
    private static final String LOGIN_IP = "198.51.100.7";

    @Mock
    private KeycloakSession session;

    @Mock
    private KeycloakContext context;

    @Mock
    private ClientConnection connection;

    @Mock
    private UserSessionModel userSession;

    private RequestIpAddressMapper mapper;
    private ProtocolMapperModel mappingModel;
    private IDToken token;

    @BeforeEach
    void setUp() {
        mapper = new RequestIpAddressMapper();
        token = new IDToken();

        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, CLAIM_NAME);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");

        mappingModel = new ProtocolMapperModel();
        mappingModel.setName("request ip");
        mappingModel.setProtocolMapper(RequestIpAddressMapper.PROVIDER_ID);
        mappingModel.setConfig(config);

        when(session.getContext()).thenReturn(context);
    }

    @Test
    void setsClaimFromCurrentConnectionRemoteAddress() {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getRemoteAddr()).thenReturn(REQUEST_IP);

        mapper.setClaim(token, mappingModel, userSession, session, null);

        assertEquals(REQUEST_IP, token.getOtherClaims().get(CLAIM_NAME));
    }

    /**
     * The whole point of the mapper: on a refresh_token grant the user session still carries
     * the login-time IP, which may be stale. The claim must come from the current request.
     *
     * <p>The {@code userSession} stub below is deliberately never consumed — Mockito would
     * flag it as unnecessary, which is exactly the assertion being made.
     */
    @Test
    void prefersRequestAddressOverStaleLoginAddress() {
        lenient().when(userSession.getIpAddress()).thenReturn(LOGIN_IP);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getRemoteAddr()).thenReturn(REQUEST_IP);

        mapper.setClaim(token, mappingModel, userSession, session, null);

        assertEquals(REQUEST_IP, token.getOtherClaims().get(CLAIM_NAME));
    }

    @Test
    void omitsClaimWhenRemoteAddressIsNull() {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getRemoteAddr()).thenReturn(null);

        mapper.setClaim(token, mappingModel, userSession, session, null);

        assertFalse(token.getOtherClaims().containsKey(CLAIM_NAME));
    }

    @Test
    void omitsClaimWhenRemoteAddressIsEmpty() {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getRemoteAddr()).thenReturn("");

        mapper.setClaim(token, mappingModel, userSession, session, null);

        assertFalse(token.getOtherClaims().containsKey(CLAIM_NAME));
    }

    @Test
    void omitsClaimWhenThereIsNoConnection() {
        when(context.getConnection()).thenReturn(null);

        mapper.setClaim(token, mappingModel, userSession, session, null);

        assertFalse(token.getOtherClaims().containsKey(CLAIM_NAME));
    }
}
