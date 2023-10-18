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

package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.core.criterion.SatisfyAnyCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.InformationURL;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.PrivacyStatementURL;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.ext.saml2mdui.impl.DisplayNameBuilder;
import org.opensaml.saml.ext.saml2mdui.impl.InformationURLBuilder;
import org.opensaml.saml.ext.saml2mdui.impl.LogoBuilder;
import org.opensaml.saml.ext.saml2mdui.impl.PrivacyStatementURLBuilder;
import org.opensaml.saml.ext.saml2mdui.impl.UIInfoBuilder;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EmailAddress;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.impl.ContactPersonBuilder;
import org.opensaml.saml.saml2.metadata.impl.EmailAddressBuilder;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.ExtensionsBuilder;
import org.opensaml.saml.saml2.metadata.impl.SPSSODescriptorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.langtag.LangTag;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.oidc.metadata.ClientInformationResolver;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.component.ComponentSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Actions merges saml2 and oidc relying parties to single list. For each party
 * relying party ui context is created.
 * 
 * TODO: action supports now only ClientInformationResolver as oidc relying
 * party source.
 */
public class RenderUserProfileContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderUserProfileContext.class);

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @NonnullAfterInit
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** The default language when it has not been defined in the metadata. */
    private String defaultLanguage;

    /**
     * The system wide languages to inspect if there is no match between metadata
     * and browser.
     */
    @Nullable
    private List<String> fallbackLanguages;

    /**
     * OIDC json based client information resolver.
     */
    @NonnullAfterInit
    private ClientInformationResolver clientResolver;

    /**
     * XML based metadata resolver.
     */
    /** Resolver used to look up SAML metadata. */
    @NonnullAfterInit
    private MetadataResolver metadataResolver;

    /**
     * Set oidc json based client information resolver.
     * 
     * @param resolver oidc json based client information resolver
     */
    public void setClientInformationResolver(@Nonnull final ClientInformationResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        clientResolver = Constraint.isNotNull(resolver, "ClientInformationResolver cannot be null");
    }

    /**
     * Set the {@link MetadataResolver} to use.
     *
     * @param resolver the resolver to use
     */
    public void setMetadataResolver(@Nonnull final MetadataResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        metadataResolver = Constraint.isNotNull(resolver, "MetadataResolver cannot be null");
    }

    /** Constructor. */
    public RenderUserProfileContext() {
        super();
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
        defaultLanguage = "en";
    }

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /**
     * Set the system wide default languages.
     * 
     * @param langs a semi-colon separated string.
     */
    public void setFallbackLanguages(@Nonnull @NonnullElements final List<String> langs) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        fallbackLanguages = List.copyOf(StringSupport.normalizeStringCollection(langs));
    }

    /**
     * Set the default language when it has not been defined in the metadata.
     * 
     * @param language to set.
     */
    public void setDefaultLanguage(@Nonnull final String language) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        defaultLanguage = Constraint.isNotEmpty(language, "The default language cannot be empty");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (clientResolver == null) {
            throw new ComponentInitializationException("ClientInformationResolver cannot be null");
        }
        if (metadataResolver == null) {
            throw new ComponentInitializationException("RoleDescriptorResolver cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        log.debug("{} Relying party information rendering start", getLogPrefix());
        UserProfileContext userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        if (userProfileContext.getRelyingParties().size() > 0) {
            log.debug("{} Metadata has already been resolved. No action taken.", getLogPrefix());
            return;
        }

        try {
            // Resolve SAML2 RPs.
            Iterable<EntityDescriptor> entityDescriptors = metadataResolver.resolve(new CriteriaSet(
                    new SatisfyAnyCriterion(), new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
            if (log.isDebugEnabled()) {
                log.debug("{} Resolved SAML SP metadata of {} entities", getLogPrefix(),
                        entityDescriptors instanceof Collection ? ((Collection<?>) entityDescriptors).size() : 0);
            }
            if (entityDescriptors != null)
                entityDescriptors.forEach(client -> {
                    SPSSODescriptor spSSODescriptor = client.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
                    if (spSSODescriptor != null) {
                        RelyingPartyUIContext relyingPartyUIContext = new RelyingPartyUIContext();
                        if (fallbackLanguages != null) {
                            relyingPartyUIContext.setFallbackLanguages(fallbackLanguages);
                        }
                        relyingPartyUIContext.setRPEntityDescriptor(client);
                        relyingPartyUIContext.setRPSPSSODescriptor(spSSODescriptor);
                        final Extensions exts = spSSODescriptor.getExtensions();
                        if (exts != null) {
                            for (final XMLObject object : exts.getOrderedChildren()) {
                                if (object instanceof UIInfo) {
                                    relyingPartyUIContext.setRPUInfo((UIInfo) object);
                                }
                            }
                        }
                        userProfileContext.getRelyingParties().put(client.getEntityID(), relyingPartyUIContext);
                    }

                });
            // Resolve JSON OIDC RPs.
            Iterable<OIDCClientInformation> oidcClientInformation = clientResolver.resolve(new CriteriaSet());
            if (log.isDebugEnabled()) {
                log.debug("{} Resolved OIDC json RP metadata of {} entities", getLogPrefix(),
                        oidcClientInformation instanceof Collection ? ((Collection<?>) oidcClientInformation).size()
                                : 0);
            }
            if (oidcClientInformation != null)
                oidcClientInformation.forEach(client -> {
                    RelyingPartyUIContext relyingPartyUIContext = new RelyingPartyUIContext();
                    final EntityDescriptor entityDescriptor = new EntityDescriptorBuilder().buildObject();
                    final OIDCClientMetadata oidcMetadata = client.getOIDCMetadata();
                    entityDescriptor.setEntityID(client.getID().getValue());
                    final SPSSODescriptor spDescriptor = new SPSSODescriptorBuilder().buildObject();
                    final UIInfo uiInfo = new UIInfoBuilder().buildObject();
                    if (fallbackLanguages != null) {
                        relyingPartyUIContext.setFallbackLanguages(fallbackLanguages);
                    }

                    for (final LangTag tag : oidcMetadata.getLogoURIEntries().keySet()) {
                        final Logo logo = new LogoBuilder().buildObject();
                        logo.setXMLLang(tag == null ? defaultLanguage : tag.getLanguage());
                        final URI logoUri = oidcMetadata.getLogoURI(tag);
                        if (logoUri != null) {
                            logo.setURI(logoUri.toString());
                            uiInfo.getLogos().add(logo);
                        }
                    }
                    for (final LangTag tag : oidcMetadata.getPolicyURIEntries().keySet()) {
                        final PrivacyStatementURL url = new PrivacyStatementURLBuilder().buildObject();
                        url.setXMLLang(tag == null ? defaultLanguage : tag.getLanguage());
                        url.setURI(oidcMetadata.getPolicyURI(tag).toString());
                        uiInfo.getPrivacyStatementURLs().add(url);
                    }
                    for (final LangTag tag : oidcMetadata.getTermsOfServiceURIEntries().keySet()) {
                        final InformationURL url = new InformationURLBuilder().buildObject();
                        url.setXMLLang(tag == null ? defaultLanguage : tag.getLanguage());
                        url.setURI(oidcMetadata.getTermsOfServiceURI(tag).toString());
                        uiInfo.getInformationURLs().add(url);
                    }
                    final List<String> emails = oidcMetadata.getEmailContacts();
                    if (emails != null) {
                        for (final String email : emails) {
                            final ContactPerson contactPerson = new ContactPersonBuilder().buildObject();
                            contactPerson.setType(ContactPersonTypeEnumeration.SUPPORT);
                            final EmailAddress address = new EmailAddressBuilder().buildObject();
                            address.setURI(email.startsWith("mailto:") ? email : "mailto:" + email);
                            contactPerson.getEmailAddresses().add(address);
                            entityDescriptor.getContactPersons().add(contactPerson);
                        }
                    }
                    for (final LangTag tag : oidcMetadata.getNameEntries().keySet()) {
                        final DisplayName displayName = new DisplayNameBuilder().buildObject();
                        displayName.setXMLLang(tag == null ? defaultLanguage : tag.getLanguage());
                        displayName.setValue(oidcMetadata.getNameEntries().get(tag));
                        uiInfo.getDisplayNames().add(displayName);
                    }
                    final Extensions extensions = new ExtensionsBuilder().buildObject();
                    extensions.getUnknownXMLObjects().add(uiInfo);
                    spDescriptor.setExtensions(extensions);
                    relyingPartyUIContext.setRPEntityDescriptor(entityDescriptor);
                    relyingPartyUIContext.setRPSPSSODescriptor(spDescriptor);
                    relyingPartyUIContext.setRPUInfo((UIInfo) uiInfo);
                    userProfileContext.getRelyingParties().put(client.getID().getValue(), relyingPartyUIContext);
                });
        } catch (ResolverException e) {
            log.warn("{} Metadata resolving failed {}", getLogPrefix(), e);
        }
    }
}
