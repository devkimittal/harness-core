/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.utils;

import io.harness.beans.stages.IntegrationStageNode;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.ci.enforcement.HostedCreditsPerMonthRestriction;
import io.harness.ci.license.CILicenseService;
import io.harness.data.structure.CollectionUtils;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.repositories.CIAccountExecutionMetadataRepository;
import io.harness.steps.SdkCoreStepUtils;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIStagePlanCreationUtils {
  @Inject EnforcementClientService enforcementClientService;

  public static StageElementParametersBuilder getStageParameters(IntegrationStageNode stageNode) {
    TagUtils.removeUuidFromTags(stageNode.getTags());

    StageElementParametersBuilder stageBuilder = StageElementParameters.builder();
    stageBuilder.name(stageNode.getName());
    stageBuilder.identifier(stageNode.getIdentifier());
    stageBuilder.description(SdkCoreStepUtils.getParameterFieldHandleValueNull(stageNode.getDescription()));
    stageBuilder.failureStrategies(stageNode.getFailureStrategies());
    stageBuilder.skipCondition(stageNode.getSkipCondition());
    stageBuilder.when(stageNode.getWhen());
    stageBuilder.type(stageNode.getType());
    stageBuilder.uuid(stageNode.getUuid());
    stageBuilder.variables(
        ParameterField.createValueField(NGVariablesUtils.getMapOfVariables(stageNode.getVariables())));
    stageBuilder.tags(CollectionUtils.emptyIfNull(stageNode.getTags()));

    return stageBuilder;
  }

  public static boolean isHostedInfra(Infrastructure infrastructure) {
    return infrastructure.getType().equals(Infrastructure.Type.HOSTED_VM)
        || infrastructure.getType().equals(Infrastructure.Type.KUBERNETES_HOSTED);
  }

  @FeatureRestrictionCheck(FeatureRestrictionName.MAX_HOSTED_CREDITS_PER_MONTH)
  public void validateFreeAccountStageExecutionLimit(
      CIAccountExecutionMetadataRepository accountExecutionMetadataRepository, CILicenseService ciLicenseService,
      String accountId, Infrastructure infrastructure) {
    if (isHostedInfra(infrastructure)) {
      // fetch current usage and limits
    }
  }
}
