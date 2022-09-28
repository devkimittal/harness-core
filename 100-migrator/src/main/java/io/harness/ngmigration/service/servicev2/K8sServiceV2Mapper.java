/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.servicev2;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.KubernetesServiceSpec.KubernetesServiceSpecBuilder;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.service.artifactstream.ArtifactStreamFactory;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class K8sServiceV2Mapper implements ServiceV2Mapper {
  @Override
  public ServiceDefinition getServiceDefinition(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, Service service, Map<CgEntityId, NGYamlFile> migratedEntities,
      List<ManifestConfigWrapper> manifests) {
    List<ArtifactStream> artifactStreams = getArtifactStream(entities, graph, service);
    PrimaryArtifact primaryArtifact = null;
    if (EmptyPredicate.isNotEmpty(artifactStreams)) {
      ArtifactStream artifactStream =
          artifactStreams.stream().findFirst().orElseThrow(() -> new IllegalStateException(""));
      primaryArtifact = ArtifactStreamFactory.getArtifactStreamMapper(artifactStream)
                            .getArtifactDetails(inputDTO, entities, graph, artifactStream, migratedEntities);
    }
    KubernetesServiceSpecBuilder kubernetesServiceSpec = KubernetesServiceSpec.builder();
    if (primaryArtifact != null) {
      kubernetesServiceSpec.artifacts(ArtifactListConfig.builder().primary(primaryArtifact).build());
    }
    kubernetesServiceSpec.manifests(manifests);
    return ServiceDefinition.builder()
        .type(ServiceDefinitionType.KUBERNETES)
        .serviceSpec(kubernetesServiceSpec.build())
        .build();
  }
}