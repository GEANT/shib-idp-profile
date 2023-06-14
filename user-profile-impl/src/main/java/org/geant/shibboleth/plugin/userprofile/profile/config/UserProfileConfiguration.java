/*
 * Copyright (c) 2022-2023, GÉANT
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geant.shibboleth.plugin.userprofile.profile.config;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.config.AbstractProfileConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.navigate.ProxyAwareDefaultAuthenticationMethodsLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Profile configuration for the user profile. Copy of shibboleth sso profile.
 */
public class UserProfileConfiguration extends AbstractProfileConfiguration
        implements ProfileConfiguration, AuthenticationProfileConfiguration {

    @Nonnull
    @NotEmpty
    public static final String PROFILE_ID = "http://geant.org/ns/profiles/userprofile/sso/browser";

    /** Lookup function to supply default authentication methods. */
    @Nonnull
    private Function<ProfileRequestContext, Collection<AuthnContextClassRefPrincipal>> defaultAuthenticationContextsLookupStrategy;

    /** Lookup function to supply authentication flows. */
    @Nonnull
    private Function<ProfileRequestContext, Set<String>> authenticationFlowsLookupStrategy;

    /** Lookup function to supply post authentication flows. */
    @Nonnull
    private Function<ProfileRequestContext, Collection<String>> postAuthenticationFlowsLookupStrategy;

    /** Whether to mandate forced authentication for the request. */
    @Nonnull
    private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /** Lookup function to supply proxyCount property. */
    @Nonnull
    private Function<ProfileRequestContext, Integer> proxyCountLookupStrategy;

    /**
     * Constructor.
     */
    public UserProfileConfiguration() {
        this(PROFILE_ID);
    }

    protected UserProfileConfiguration(String id) {
        super(id);
        defaultAuthenticationContextsLookupStrategy = new ProxyAwareDefaultAuthenticationMethodsLookupFunction();
        authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        postAuthenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        forceAuthnPredicate = Predicates.alwaysFalse();
        proxyCountLookupStrategy = FunctionSupport.constant(null);
    }

    /** {@inheritDoc} */
    @Nonnull
    @NonnullElements
    @NotLive
    @Unmodifiable
    public List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<AuthnContextClassRefPrincipal> methods = defaultAuthenticationContextsLookupStrategy
                .apply(profileRequestContext);
        if (methods != null) {
            return List.copyOf(methods);
        }
        return Collections.emptyList();
    }

    /**
     * Set the default authentication contexts to use, expressed as custom
     * principals.
     * 
     * @param contexts default authentication contexts to use
     */
    public void setDefaultAuthenticationMethods(
            @Nullable @NonnullElements final Collection<AuthnContextClassRefPrincipal> contexts) {
        if (contexts != null) {
            defaultAuthenticationContextsLookupStrategy = FunctionSupport.constant(List.copyOf(contexts));
        } else {
            defaultAuthenticationContextsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the authentication contexts to use, expressed as
     * custom principals.
     *
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setDefaultAuthenticationMethodsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, Collection<AuthnContextClassRefPrincipal>> strategy) {
        defaultAuthenticationContextsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull
    @NonnullElements
    @NotLive
    @Unmodifiable
    public Set<String> getAuthenticationFlows(@Nullable final ProfileRequestContext profileRequestContext) {
        final Set<String> flows = authenticationFlowsLookupStrategy.apply(profileRequestContext);
        if (flows != null) {
            return Set.copyOf(flows);
        }
        return Collections.emptySet();
    }

    /**
     * Set the authentication flows to use.
     * 
     * @param flows flow identifiers to use
     */
    public void setAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            authenticationFlowsLookupStrategy = FunctionSupport
                    .constant(Set.copyOf(StringSupport.normalizeStringCollection(flows)));
        } else {
            authenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the authentication flows to use.
     *
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setAuthenticationFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, Set<String>> strategy) {
        authenticationFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull
    @NonnullElements
    @NotLive
    @Unmodifiable
    public List<String> getPostAuthenticationFlows(@Nullable final ProfileRequestContext profileRequestContext) {
        final Collection<String> flows = postAuthenticationFlowsLookupStrategy.apply(profileRequestContext);
        if (flows != null) {
            return List.copyOf(flows);
        }
        return Collections.emptyList();
    }

    /**
     * Set the ordered collection of post-authentication interceptor flows to
     * enable.
     * 
     * @param flows flow identifiers to enable
     */
    public void setPostAuthenticationFlows(@Nullable @NonnullElements final Collection<String> flows) {
        if (flows != null) {
            postAuthenticationFlowsLookupStrategy = FunctionSupport
                    .constant(List.copyOf(StringSupport.normalizeStringCollection(flows)));
        } else {
            postAuthenticationFlowsLookupStrategy = FunctionSupport.constant(null);
        }
    }

    /**
     * Set a lookup strategy for the post-authentication interceptor flows to
     * enable.
     *
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setPostAuthenticationFlowsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, Collection<String>> strategy) {
        postAuthenticationFlowsLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    public boolean isForceAuthn(@Nullable final ProfileRequestContext profileRequestContext) {
        return forceAuthnPredicate.test(profileRequestContext);
    }

    /**
     * Set whether a fresh user presence proof should be required for this request.
     * 
     * @param flag flag to set
     */
    public void setForceAuthn(final boolean flag) {
        forceAuthnPredicate = flag ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }

    /**
     * Set a condition to determine whether a fresh user presence proof should be
     * required for this request.
     * 
     * @param condition condition to set
     */
    public void setForceAuthnPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        forceAuthnPredicate = Constraint.isNotNull(condition, "Forced authentication predicate cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable
    public Integer getProxyCount(@Nullable final ProfileRequestContext profileRequestContext) {
        final Integer count = proxyCountLookupStrategy.apply(profileRequestContext);
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        return count;
    }

    /**
     * Sets the maximum number of times an assertion may be proxied outbound and/or
     * the maximum number of hops between the relying party and a proxied
     * authentication authority inbound.
     * 
     * @param count proxy count
     * 
     * @since 4.0.0
     */
    public void setProxyCount(@Nullable @NonNegative final Integer count) {
        if (count != null) {
            Constraint.isGreaterThanOrEqual(0, count, "Proxy count must be greater than or equal to 0");
        }
        proxyCountLookupStrategy = FunctionSupport.constant(count);
    }

    /**
     * Set a lookup strategy for the maximum number of times an assertion may be
     * proxied outbound and/or the maximum number of hops between the relying party
     * and a proxied authentication authority inbound.
     *
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext, Integer> strategy) {
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }

}