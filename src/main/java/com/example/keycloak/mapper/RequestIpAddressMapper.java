package com.example.keycloak.mapper;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * Adds the IP address of the HTTP request that issued the token as a claim.
 *
 * <p>This is deliberately the <em>request-time</em> address, not the address recorded on the
 * user session at login. See {@link #setClaim} for why that distinction matters.
 *
 * <p><strong>SPI note:</strong> this provider is registered in
 * {@code META-INF/services/org.keycloak.protocol.ProtocolMapper}. That interface lives in
 * {@code keycloak-server-spi-private}, i.e. it is not part of Keycloak's public, stability-
 * guaranteed SPI surface. There is no public alternative for protocol mappers, so the
 * dependency is unavoidable; it is the reason the {@code keycloak.version} property must
 * track the server version.
 *
 * <p>Everything else used here ({@code KeycloakSession}, {@code KeycloakContext},
 * {@code ClientConnection}) is public SPI.
 */
public class RequestIpAddressMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper,
        TokenIntrospectionTokenMapper {

    public static final String PROVIDER_ID = "oidc-request-ip-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        // Claim name ("Token Claim Name" in the admin console) ...
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
        // ... and the "Add to ID token" / "Add to access token" / "Add to userinfo" /
        // "Add to token introspection" toggles. Which toggles are rendered is derived from
        // the marker interfaces this class implements, hence passing our own type.
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES,
                RequestIpAddressMapper.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Request IP Address";
    }

    @Override
    public String getDisplayCategory() {
        // Inherited constant from AbstractOIDCProtocolMapper ("Token mapper"), which is
        // where Keycloak 26.x defines it — it is not on ProtocolMapperUtils.
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds the IP address of the HTTP request that issued this token as a claim. "
                + "Unlike the session's login IP, this is re-evaluated on every grant, "
                + "including refresh_token. Requires the server to be configured to trust "
                + "the reverse proxy's X-Forwarded-For header when running behind one.";
    }

    /**
     * Reads the remote address of the request currently being served and maps it to the
     * configured claim.
     *
     * <p>The address comes from {@code session.getContext().getConnection()} and
     * <strong>not</strong> from {@link UserSessionModel#getIpAddress()}. The latter is frozen
     * at login time, so on a {@code refresh_token} grant — which does not involve the browser
     * at all and may come from a completely different network — it would report a stale
     * address. {@code KeycloakContext#getConnection()} is bound to the in-flight HTTP request,
     * so it is correct for every grant type.
     *
     * <p>Keycloak 26.x dispatches to this five-argument overload; the legacy three-argument
     * {@code setClaim(IDToken, ProtocolMapperModel, UserSessionModel)} is not overridden
     * because it does not receive the {@link KeycloakSession} and therefore cannot reach the
     * current request context.
     *
     * <p>Note that {@code getRemoteAddr()} returns the peer address of the connection unless
     * the server is configured with {@code KC_PROXY_HEADERS=xforwarded} and a matching
     * {@code KC_PROXY_TRUSTED_ADDRESSES}; behind an OpenShift Route without those, the claim
     * would carry the router pod's IP. See the README.
     */
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
            UserSessionModel userSession, KeycloakSession session,
            ClientSessionContext clientSessionCtx) {

        ClientConnection connection = session.getContext().getConnection();
        if (connection == null) {
            // No HTTP request in scope (e.g. a token minted from an internal/admin code
            // path). Omit the claim rather than failing the token issuance.
            return;
        }

        String remoteAddress = connection.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isEmpty()) {
            return;
        }

        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, remoteAddress);
    }
}
