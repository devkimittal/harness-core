/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.docker;

import static io.harness.delegate.beans.connector.docker.DockerAuthType.USER_PASSWORD;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDC)
public class DockerDTOToEntityTest extends CategoryTest {
  @InjectMocks DockerDTOToEntity dockerDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toConnectorEntityTest() {
    String dockerRegistryUrl = "url";
    String dockerUserName = "dockerUserName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        DockerUserNamePasswordDTO.builder().username(dockerUserName).passwordRef(passwordSecretRef).build();

    DockerAuthenticationDTO dockerAuthenticationDTO =
        DockerAuthenticationDTO.builder().authType(USER_PASSWORD).credentials(dockerUserNamePasswordDTO).build();
    DockerConnectorDTO dockerConnectorDTO =
        DockerConnectorDTO.builder().dockerRegistryUrl(dockerRegistryUrl).auth(dockerAuthenticationDTO).build();
    DockerConnector dockerConectorEntity = dockerDTOToEntity.toConnectorEntity(dockerConnectorDTO);
    assertThat(dockerConectorEntity).isNotNull();
    assertThat(dockerConectorEntity.getUrl()).isEqualTo(dockerRegistryUrl);
    assertThat(((DockerUserNamePasswordAuthentication) (dockerConectorEntity.getDockerAuthentication())).getUsername())
        .isEqualTo(dockerUserName);
    assertThat(
        ((DockerUserNamePasswordAuthentication) (dockerConectorEntity.getDockerAuthentication())).getPasswordRef())
        .isEqualTo(passwordSecretRef.toSecretRefStringValue());
    assertThat(dockerConectorEntity.getAuthType()).isEqualTo(USER_PASSWORD);
  }
}
