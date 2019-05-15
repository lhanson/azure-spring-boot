/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import com.microsoft.aad.adal4j.ClientCredential;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

@RunWith(MockitoJUnitRunner.class)
public class AzureADGraphClientTest {

    private AzureADGraphClient adGraphClient;

    @Mock
    private ClientCredential credential;

    private AADAuthenticationProperties aadAuthProps;

    @Mock
    private ServiceEndpointsProperties endpointsProps;
    
    @Before
    public void setup() {
        final List<String> activeDirectoryGroups = new ArrayList<>();
        activeDirectoryGroups.add("Test_Group");
        aadAuthProps = new AADAuthenticationProperties();
        aadAuthProps.setActiveDirectoryGroups(activeDirectoryGroups);
        adGraphClient = new AzureADGraphClient(credential, aadAuthProps, endpointsProps);
    }

     @Test
     public void testConvertGroupToGrantedAuthorities() {

         final List<UserGroup> userGroups = Collections.singletonList(
                 new UserGroup("testId", "Test_Group"));

         final Set<GrantedAuthority> authorities = adGraphClient.convertGroupsToGrantedAuthorities(userGroups);
         assertThat(authorities).hasSize(1).extracting(GrantedAuthority::getAuthority)
                 .containsExactly("ROLE_Test_Group");
     }

    @Test
    public void testConvertGroupToGrantedAuthoritiesUsingAllowedGroups() {
        final List<UserGroup> userGroups = Arrays
                .asList(new UserGroup("testId", "Test_Group"),
                        new UserGroup("testId", "Another_Group"));
        aadAuthProps.getUserGroup().getAllowedGroups().add("Another_Group");
        final Set<GrantedAuthority> authorities = adGraphClient.convertGroupsToGrantedAuthorities(userGroups);
        assertThat(authorities).hasSize(2).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_Test_Group", "ROLE_Another_Group");
    }

    @Test
    public void testGetGrantedAuthoritiesWith403() {
        Set<GrantedAuthority> grantedAuthorities = null;
        final AzureADGraphClient adGraphClientSpy = Mockito.spy(adGraphClient);
        try {
            Mockito.doThrow(new IOException("Connection returned 403"))
                    .when(adGraphClientSpy).getGroups("graphApiToken");
            grantedAuthorities = adGraphClientSpy.getGrantedAuthorities("graphApiToken");
        } catch (IOException e) {
            fail("IOException should not be propagated when the aadMembershipRestUri returns a 403");
        }
        assertThat(grantedAuthorities).size().isEqualTo(1);
        assertThat(grantedAuthorities).contains(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
