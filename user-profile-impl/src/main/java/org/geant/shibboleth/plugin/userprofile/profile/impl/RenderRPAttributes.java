package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opensaml.profile.context.ProfileRequestContext;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONObject;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Action transcodes attributes per relying party. The purpose is to verify
 * which of the resolved and filtered attributes have transcoding result and
 * thus reach the user.
 * 
 */
public class RenderRPAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderRPAttributes.class);

    /**
     * Strategy used to locate or create the {@link RelyingPartyContext} to
     * populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextCreationStrategy;

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Transcoder registry service object. */
    @NonnullAfterInit
    private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /** Relying Party Id we resolve attributes for. */
    private String rpId;

    /** Context for User Profile . */
    private UserProfileContext userProfileContext;

    /** Relying party context we manipulate for attribute resolving/filtering. */
    private RelyingPartyContext rpContext;

    /** Constructor. */
    RenderRPAttributes() {
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
    }

    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        relyingPartyContextCreationStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set strategy used to locate or create the {@link UserProfileContext} to
     * populate.
     * 
     * @param strategy Strategy used to locate or create the
     *                 {@link UserProfileContext} to populate.
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /**
     * Sets the registry of transcoding rules to apply to supply attribute display
     * metadata.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nullable final ReloadableService<AttributeTranscoderRegistry> registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        transcoderRegistry = registry;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (transcoderRegistry == null) {
            throw new ComponentInitializationException("transcoderRegistry cannot be null");
        }
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.error("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        rpId = rpContext.getRelyingPartyId();
        if (rpId == null || rpId.isBlank()) {
            log.error("{} Relying party id missing", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        if (userProfileContext.getRPAttributeContext() == null
                || userProfileContext.getRPAttributeContext().get(rpId) == null) {
            // Nothing to do, no attributes resolved
            // Cleanup relying party in this last action of attribute resolving/filtering.
            rpContext.setRelyingPartyId(null);
            return false;
        }
        return true;

    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        ServiceableComponent<AttributeTranscoderRegistry> component = null;
        try {
            component = transcoderRegistry.getServiceableComponent();
            if (component == null) {
                log.error("Attribute transoding service unavailable");
                throw new AttributeEncodingException("Attribute transoding service unavailable");
            }

            for (final IdPAttribute attribute : userProfileContext.getRPAttributeContext().get(rpId).getIdPAttributes()
                    .values()) {
                if (attribute != null && !attribute.getValues().isEmpty()) {
                    final Collection<TranscodingRule> transcodingRules = component.getComponent()
                            .getTranscodingRules(attribute, JSONObject.class);
                    if (transcodingRules.isEmpty()) {
                        log.debug("{} Attribute {} does not have any transcoding rules, nothing to do", getLogPrefix(),
                                attribute.getId());
                        continue;
                    }
                    for (final TranscodingRule rule : transcodingRules) {
                        final AttributeTranscoder<JSONObject> transcoder = TranscoderSupport
                                .<JSONObject>getTranscoder(rule);
                        final JSONObject encodedAttribute = transcoder.encode(profileRequestContext, attribute,
                                JSONObject.class, rule);
                        if (encodedAttribute != null)
                            userProfileContext.setEncodedJSONAttribute(rpId, attribute);
                        log.debug("{} Transcoded attribute {} as {}", getLogPrefix(), attribute.getId(),
                                encodedAttribute.toString());
                    }
                }
            }
        } catch (final AttributeEncodingException e) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_ENCODE_ATTRIBUTE);
            return;
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
        // Cleanup relying party in this last action of attribute resolving/filtering.
        rpContext.setRelyingPartyId(null);
    }
}
