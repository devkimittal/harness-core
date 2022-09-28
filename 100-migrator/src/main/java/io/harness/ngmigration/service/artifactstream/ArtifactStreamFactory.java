/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.artifactstream;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactStreamFactory {
  private static final ArtifactStreamMapper dockerMapper = new DockerArtifactStreamMapper();
  private static final ArtifactStreamMapper artifactoryMapper = new ArtifactoryArtifactStreamMapper();

  private static final Map<ArtifactStreamType, ArtifactStreamMapper> ARTIFACT_STREAM_MAPPER_MAP =
      ImmutableMap.<ArtifactStreamType, ArtifactStreamMapper>builder()
          .put(ArtifactStreamType.ARTIFACTORY, artifactoryMapper)
          .put(ArtifactStreamType.DOCKER, dockerMapper)
          .build();

  public static ArtifactStreamMapper getArtifactStreamMapper(ArtifactStream artifactStream) {
    ArtifactStreamType artifactStreamType = ArtifactStreamType.valueOf(artifactStream.getArtifactStreamType());
    if (ARTIFACT_STREAM_MAPPER_MAP.containsKey(artifactStreamType)) {
      return ARTIFACT_STREAM_MAPPER_MAP.get(artifactStreamType);
    }
    throw new InvalidRequestException(
        String.format("Unsupported artifact stream of type %s", artifactStream.getArtifactStreamType()));
  }
}