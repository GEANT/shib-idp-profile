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
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.xml.SAMLConstants;
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
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Actions merges SAML2 and OIDC based relying parties to single list. For each
 * party Relying Party UI Context is created.
 */
public class RenderUserProfileContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderUserProfileContext.class);

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
     * @param language What to set.
     */
    public void setDefaultLanguage(@Nonnull final String language) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        defaultLanguage = Constraint.isNotEmpty(language, "The default language cannot be empty");
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

        if (userProfileContext.getOidcClientInformation() != null)
            userProfileContext.getOidcClientInformation().forEach(client -> {
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
                        // TODO: should it be configurable?
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

        // Render SAML2 clients
        if (userProfileContext.getEntityDescriptors() != null)
            userProfileContext.getEntityDescriptors().forEach(client -> {
                SPSSODescriptor spSSODescriptor = client.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
                if (spSSODescriptor != null) {
                    RelyingPartyUIContext relyingPartyUIContext = new RelyingPartyUIContext();
                    if (fallbackLanguages != null) {
                        relyingPartyUIContext.setFallbackLanguages(fallbackLanguages);
                    }
                    // TODO: Implementation assumes all clients are of type SAML2. Fix it.
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
    }

}
