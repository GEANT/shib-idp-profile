package org.geant.shibboleth.plugin.userprofile.utils;

import java.time.Duration;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minidev.json.JSONObject;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.AccessTokenContext;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.OIDCAuthenticationResponseContext;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWT;

public class AccessToken {

    /** Class logger. */
    @Nonnull
    private Logger log = LoggerFactory.getLogger(AccessToken.class);

    private JWT jwt = null;
    private String opaque = null;
    private Duration lifeTime = null;

    public AccessToken(@Nonnull final ProfileRequestContext profileRequestContext) {
        Function<ProfileRequestContext, AccessTokenContext> accessTokenContextLookupStrategy = new ChildContextLookup<>(
                AccessTokenContext.class)
                .compose(new ChildContextLookup<>(OIDCAuthenticationResponseContext.class)
                        .compose(new OutboundMessageContextLookup()));
        AccessTokenContext ctx = accessTokenContextLookupStrategy.apply(profileRequestContext);
        if (ctx != null) {
            jwt = ctx.getJWT();
            opaque = ctx.getOpaque();
            lifeTime = ctx.getLifetime();
        }

    }

    public boolean exists() {
        return (getJwt() != null || getOpaque() != null);
    }

    public JWT getJwt() {
        return jwt;
    }

    public String getOpaque() {
        return opaque;
    }

    public Duration getLifeTime() {
        return lifeTime;
    }

    public JSONObject toJSONObject() {
        JSONObject token = new JSONObject();
        token.put("opaque", getOpaque());
        token.put("jwt", getJwt());
        token.put("expires_in", lifeTime != null ? lifeTime.toSeconds() : null);
        log.debug("Access Token as JSON {}", token.toJSONString());
        return token;
    }

}
