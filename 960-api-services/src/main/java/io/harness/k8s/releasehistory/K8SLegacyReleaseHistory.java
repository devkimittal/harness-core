/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.releasehistory;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class K8SLegacyReleaseHistory implements IK8sReleaseHistory {
  ReleaseHistory releaseHistory;

  @Override
  public int getCurrentReleaseNumber() {
    return releaseHistory.getCurrentReleaseNumber();
  }

  @Override
  public IK8sRelease getLastSuccessfulRelease(int currentReleaseNumber) {
    return releaseHistory.getPreviousRollbackEligibleRelease(currentReleaseNumber);
  }

  @Override
  public IK8sRelease getLatestRelease() {
    if (isNotEmpty(releaseHistory.getReleases())) {
      return releaseHistory.getLatestRelease();
    }
    return null;
  }
}