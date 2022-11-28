/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.buildstate;

import static io.harness.ci.commonconstants.CIExecutionConstants.MAXIMUM_EXPANSION_LIMIT;
import static io.harness.ci.commonconstants.CIExecutionConstants.MAXIMUM_EXPANSION_LIMIT_FREE_ACCOUNT;
import static io.harness.govern.Switch.unhandled;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.ci.integrationstage.DliteVmInitializeTaskParamsBuilder;
import io.harness.ci.integrationstage.DockerInitializeTaskParamsBuilder;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.ci.integrationstage.K8InitializeTaskParamsBuilder;
import io.harness.ci.integrationstage.VmInitializeTaskParamsBuilder;
import io.harness.ci.license.CILicenseService;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.steps.matrix.ExpandedExecutionWrapperInfo;
import io.harness.steps.matrix.StrategyExpansionData;
import io.harness.steps.matrix.StrategyHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class BuildSetupUtils {
  @Inject private K8InitializeTaskParamsBuilder k8InitializeTaskParamsBuilder;
  @Inject private VmInitializeTaskParamsBuilder vmInitializeTaskParamsBuilder;
  @Inject private DliteVmInitializeTaskParamsBuilder dliteVmInitializeTaskParamsBuilder;
  @Inject private DockerInitializeTaskParamsBuilder dockerInitializeTaskParamsBuilder;
  @Inject private CILicenseService ciLicenseService;
  @Inject private StrategyHelper strategyHelper;

  public CIInitializeTaskParams getBuildSetupTaskParams(
      InitializeStepInfo initializeStepInfo, Ambiance ambiance, String logPrefix) {
    initializeStepInfo = populateStrategyExpansion(initializeStepInfo, ambiance);

    switch (initializeStepInfo.getInfrastructure().getType()) {
      case KUBERNETES_DIRECT:
      case KUBERNETES_HOSTED:
        return k8InitializeTaskParamsBuilder.getK8InitializeTaskParams(initializeStepInfo, ambiance, logPrefix);
      case VM:
        return vmInitializeTaskParamsBuilder.getDirectVmInitializeTaskParams(initializeStepInfo, ambiance);
      case DOCKER:
        return dockerInitializeTaskParamsBuilder.getDockerInitializeTaskParams(initializeStepInfo, ambiance);
      case HOSTED_VM:
        return dliteVmInitializeTaskParamsBuilder.getDliteVmInitializeTaskParams(initializeStepInfo, ambiance);
      default:
        unhandled(initializeStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }

  private InitializeStepInfo populateStrategyExpansion(InitializeStepInfo initializeStepInfo, Ambiance ambiance) {
    ExecutionElementConfig executionElement = initializeStepInfo.getExecutionElementConfig();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    List<ExecutionWrapperConfig> expandedExecutionElement = new ArrayList<>();
    Map<String, StrategyExpansionData> strategyExpansionMap = new HashMap<>();

    LicensesWithSummaryDTO licensesWithSummaryDTO = ciLicenseService.getLicenseSummary(accountId);
    Optional<Integer> maxExpansionLimit = Optional.of(Integer.valueOf(MAXIMUM_EXPANSION_LIMIT));
    if (licensesWithSummaryDTO != null && licensesWithSummaryDTO.getEdition() == Edition.FREE) {
      maxExpansionLimit = Optional.of(Integer.valueOf(MAXIMUM_EXPANSION_LIMIT_FREE_ACCOUNT));
    }

    for (ExecutionWrapperConfig config : executionElement.getSteps()) {
      // Inject the envVariables before calling strategy expansion
      IntegrationStageUtils.injectLoopEnvVariables(config);
      ExpandedExecutionWrapperInfo expandedExecutionWrapperInfo =
          strategyHelper.expandExecutionWrapperConfig(config, maxExpansionLimit);
      expandedExecutionElement.addAll(expandedExecutionWrapperInfo.getExpandedExecutionConfigs());
      strategyExpansionMap.putAll(expandedExecutionWrapperInfo.getUuidToStrategyExpansionData());
    }

    initializeStepInfo.setExecutionElementConfig(
        ExecutionElementConfig.builder().steps(expandedExecutionElement).build());
    initializeStepInfo.setStrategyExpansionMap(strategyExpansionMap);
    return initializeStepInfo;
  }
}
