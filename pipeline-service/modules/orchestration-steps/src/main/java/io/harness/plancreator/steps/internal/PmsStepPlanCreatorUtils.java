/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps.internal;

import static io.harness.pms.yaml.YAMLFieldNameConstants.FAILURE_STRATEGIES;
import static io.harness.pms.yaml.YAMLFieldNameConstants.ROLLBACK_STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserRollbackParameters;
import io.harness.advisers.manualIntervention.ManualInterventionAdviserWithRollback;
import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.advisers.retry.RetryAdviserRollbackParameters;
import io.harness.advisers.retry.RetryAdviserWithRollback;
import io.harness.advisers.rollback.OnFailRollbackAdviser;
import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.advisers.rollback.OnFailRollbackParameters.OnFailRollbackParametersBuilder;
import io.harness.advisers.rollback.ProceedWithDefaultValueAdviser;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.govern.Switch;
import io.harness.plancreator.NGCommonUtilPlanCreationConstants;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.plancreator.steps.FailureStrategiesUtils;
import io.harness.plancreator.steps.GenericPlanCreatorUtils;
import io.harness.plancreator.strategy.StrategyUtils;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.ProceedWithDefaultAdviserParameters;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviser;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviserParameters;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviser;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviserParameters;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviser;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PmsStepPlanCreatorUtils {
  public List<AdviserObtainment> getAdviserObtainmentFromMetaData(
      KryoSerializer kryoSerializer, YamlField currentField) {
    boolean isStepInsideRollback = false;
    if (YamlUtils.findParentNode(currentField.getNode(), ROLLBACK_STEPS) != null) {
      isStepInsideRollback = true;
    }

    // Adding adviser obtainment list from the failure strategy.
    List<AdviserObtainment> adviserObtainmentList =
        new ArrayList<>(getAdviserObtainmentForFailureStrategy(kryoSerializer, currentField, isStepInsideRollback));

    /*
     * Adding OnSuccess adviser if step is inside rollback section else adding NextStep adviser for when condition to
     * work.
     */
    if (isStepInsideRollback) {
      AdviserObtainment onSuccessAdviserObtainment = getOnSuccessAdviserObtainment(kryoSerializer, currentField);
      if (onSuccessAdviserObtainment != null) {
        adviserObtainmentList.add(onSuccessAdviserObtainment);
      }
    } else {
      // Always add nextStep adviser at last, as its priority is less than, Do not change the order.
      AdviserObtainment nextStepAdviserObtainment = getNextStepAdviserObtainment(kryoSerializer, currentField);
      if (nextStepAdviserObtainment != null) {
        adviserObtainmentList.add(nextStepAdviserObtainment);
      }
    }

    return adviserObtainmentList;
  }

  @VisibleForTesting
  AdviserObtainment getNextStepAdviserObtainment(KryoSerializer kryoSerializer, YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (GenericPlanCreatorUtils.checkIfStepIsInParallelSection(currentField)
          || StrategyUtils.isWrappedUnderStrategy(currentField)) {
        return null;
      }
      YamlField siblingField = GenericPlanCreatorUtils.obtainNextSiblingField(currentField);
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        return AdviserObtainment.newBuilder()
            .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STEP.name()).build())
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                NextStepAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
            .build();
      }
    }
    return null;
  }

  @VisibleForTesting
  AdviserObtainment getOnSuccessAdviserObtainment(KryoSerializer kryoSerializer, YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (GenericPlanCreatorUtils.checkIfStepIsInParallelSection(currentField)
          || StrategyUtils.isWrappedUnderStrategy(currentField)) {
        return null;
      }
      YamlField siblingField = GenericPlanCreatorUtils.obtainNextSiblingField(currentField);
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        return AdviserObtainment.newBuilder()
            .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
            .build();
      }
    }
    return null;
  }

  @VisibleForTesting
  List<AdviserObtainment> getAdviserObtainmentForFailureStrategy(
      KryoSerializer kryoSerializer, YamlField currentField, boolean isStepInsideRollback) {
    List<AdviserObtainment> adviserObtainmentList = new ArrayList<>();
    List<FailureStrategyConfig> stageFailureStrategies =
        getFieldFailureStrategies(currentField, STAGE, isStepInsideRollback);
    List<FailureStrategyConfig> stepGroupFailureStrategies =
        getFieldFailureStrategies(currentField, STEP_GROUP, isStepInsideRollback);
    List<FailureStrategyConfig> stepFailureStrategies = getFailureStrategies(currentField.getNode());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMap;
    FailureStrategiesUtils.priorityMergeFailureStrategies(
        stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    actionMap = FailureStrategiesUtils.priorityMergeFailureStrategies(
        stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    for (Map.Entry<FailureStrategyActionConfig, Collection<FailureType>> entry : actionMap.entrySet()) {
      FailureStrategyActionConfig action = entry.getKey();
      Set<FailureType> failureTypes = new HashSet<>(entry.getValue());
      NGFailureActionType actionType = action.getType();

      String nextNodeUuid = null;
      YamlField siblingField = GenericPlanCreatorUtils.obtainNextSiblingField(currentField);
      // Check if step is in parallel section then dont have nextNodeUUid set.
      if (siblingField != null && !GenericPlanCreatorUtils.checkIfStepIsInParallelSection(currentField)
          && !StrategyUtils.isWrappedUnderStrategy(currentField)) {
        nextNodeUuid = siblingField.getNode().getUuid();
      }

      AdviserObtainment.Builder adviserObtainmentBuilder = AdviserObtainment.newBuilder();
      if (isStepInsideRollback) {
        if (actionType == NGFailureActionType.STAGE_ROLLBACK || actionType == NGFailureActionType.STEP_GROUP_ROLLBACK) {
          throw new InvalidRequestException("Step inside rollback section cannot have Rollback as failure strategy.");
        }
      }

      switch (actionType) {
        case IGNORE:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(IgnoreAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(IgnoreAdviserParameters.builder()
                                                                                .applicableFailureTypes(failureTypes)
                                                                                .nextNodeId(nextNodeUuid)
                                                                                .build())))
                  .build());
          break;
        case RETRY:
          RetryFailureActionConfig retryAction = (RetryFailureActionConfig) action;
          FailureStrategiesUtils.validateRetryFailureAction(retryAction);
          ParameterField<Integer> retryCount = retryAction.getSpecConfig().getRetryCount();
          FailureStrategyActionConfig actionUnderRetry = retryAction.getSpecConfig().getOnRetryFailure().getAction();
          adviserObtainmentList.add(getRetryAdviserObtainment(kryoSerializer, failureTypes, nextNodeUuid,
              adviserObtainmentBuilder, retryAction, retryCount, actionUnderRetry, currentField));
          break;
        case MARK_AS_SUCCESS:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(OnMarkSuccessAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(OnMarkSuccessAdviserParameters.builder()
                                                                                .applicableFailureTypes(failureTypes)
                                                                                .nextNodeId(nextNodeUuid)
                                                                                .build())))
                  .build());

          break;
        case ABORT:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(OnAbortAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      OnAbortAdviserParameters.builder().applicableFailureTypes(failureTypes).build())))
                  .build());
          break;
        case STAGE_ROLLBACK:
          OnFailRollbackParameters rollbackParameters =
              getRollbackParameters(currentField, failureTypes, RollbackStrategy.STAGE_ROLLBACK);
          adviserObtainmentList.add(adviserObtainmentBuilder.setType(OnFailRollbackAdviser.ADVISER_TYPE)
                                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(rollbackParameters)))
                                        .build());
          break;
        case MANUAL_INTERVENTION:
          ManualInterventionFailureActionConfig actionConfig = (ManualInterventionFailureActionConfig) action;
          FailureStrategiesUtils.validateManualInterventionFailureAction(actionConfig);
          FailureStrategyActionConfig actionUnderManualIntervention =
              actionConfig.getSpecConfig().getOnTimeout().getAction();
          adviserObtainmentList.add(getManualInterventionAdviserObtainment(
              kryoSerializer, failureTypes, adviserObtainmentBuilder, actionConfig, actionUnderManualIntervention));
          break;
        case PROCEED_WITH_DEFAULT_VALUE:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(ProceedWithDefaultValueAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      ProceedWithDefaultAdviserParameters.builder().applicableFailureTypes(failureTypes).build())))
                  .build());
          break;
        case PIPELINE_ROLLBACK:
          rollbackParameters = getRollbackParameters(currentField, failureTypes, RollbackStrategy.PIPELINE_ROLLBACK);
          adviserObtainmentList.add(adviserObtainmentBuilder.setType(OnFailRollbackAdviser.ADVISER_TYPE)
                                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(rollbackParameters)))
                                        .build());
          break;
        default:
          Switch.unhandled(actionType);
      }
    }
    return adviserObtainmentList;
  }

  public String getName(AbstractStepNode stepElement) {
    String nodeName;
    if (EmptyPredicate.isEmpty(stepElement.getName())) {
      nodeName = stepElement.getIdentifier();
    } else {
      nodeName = stepElement.getName();
    }
    return nodeName;
  }

  public OnFailRollbackParameters getRollbackParameters(
      YamlField currentField, Set<FailureType> failureTypes, RollbackStrategy rollbackStrategy) {
    OnFailRollbackParametersBuilder rollbackParametersBuilder = OnFailRollbackParameters.builder();
    rollbackParametersBuilder.applicableFailureTypes(failureTypes);
    rollbackParametersBuilder.strategy(rollbackStrategy);
    rollbackParametersBuilder.strategyToUuid(getRollbackStrategyMap(currentField));
    return rollbackParametersBuilder.build();
  }

  @VisibleForTesting
  Map<RollbackStrategy, String> getRollbackStrategyMap(YamlField currentField) {
    String stageNodeId = GenericPlanCreatorUtils.getStageNodeId(currentField);
    Map<RollbackStrategy, String> rollbackStrategyStringMap = new HashMap<>();
    rollbackStrategyStringMap.put(
        RollbackStrategy.STAGE_ROLLBACK, stageNodeId + NGCommonUtilPlanCreationConstants.COMBINED_ROLLBACK_ID_SUFFIX);
    rollbackStrategyStringMap.put(
        RollbackStrategy.STEP_GROUP_ROLLBACK, GenericPlanCreatorUtils.getStepGroupRollbackStepsNodeId(currentField));
    // todo: add next uuid for pipeline rollback strategy
    return rollbackStrategyStringMap;
  }

  @VisibleForTesting
  AdviserObtainment getRetryAdviserObtainment(KryoSerializer kryoSerializer, Set<FailureType> failureTypes,
      String nextNodeUuid, AdviserObtainment.Builder adviserObtainmentBuilder, RetryFailureActionConfig retryAction,
      ParameterField<Integer> retryCount, FailureStrategyActionConfig actionUnderRetry, YamlField currentField) {
    return adviserObtainmentBuilder.setType(RetryAdviserWithRollback.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
            RetryAdviserRollbackParameters.builder()
                .applicableFailureTypes(failureTypes)
                .nextNodeId(nextNodeUuid)
                .repairActionCodeAfterRetry(GenericPlanCreatorUtils.toRepairAction(actionUnderRetry))
                .retryCount(retryCount.getValue())
                .strategyToUuid(getRollbackStrategyMap(currentField))
                .waitIntervalList(retryAction.getSpecConfig()
                                      .getRetryIntervals()
                                      .getValue()
                                      .stream()
                                      .map(s -> (int) TimeoutUtils.getTimeoutInSeconds(s, 0))
                                      .collect(Collectors.toList()))
                .build())))
        .build();
  }

  @VisibleForTesting
  List<FailureStrategyConfig> getFieldFailureStrategies(
      YamlField currentField, String fieldName, boolean isStepInsideRollback) {
    YamlNode fieldNode = YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), fieldName);
    if (isStepInsideRollback && fieldNode != null) {
      // Check if found fieldNode is within rollbackSteps section
      YamlNode rollbackNode = YamlUtils.findParentNode(fieldNode, ROLLBACK_STEPS);
      if (rollbackNode == null) {
        return Collections.emptyList();
      }
    }
    if (fieldNode != null) {
      return getFailureStrategies(fieldNode);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  AdviserObtainment getManualInterventionAdviserObtainment(KryoSerializer kryoSerializer, Set<FailureType> failureTypes,
      AdviserObtainment.Builder adviserObtainmentBuilder, ManualInterventionFailureActionConfig actionConfig,
      FailureStrategyActionConfig actionUnderManualIntervention) {
    return adviserObtainmentBuilder.setType(ManualInterventionAdviserWithRollback.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
            ManualInterventionAdviserRollbackParameters.builder()
                .applicableFailureTypes(failureTypes)
                .timeoutAction(GenericPlanCreatorUtils.toRepairAction(actionUnderManualIntervention))
                .timeout((int) TimeoutUtils.getTimeoutInSeconds(actionConfig.getSpecConfig().getTimeout(), 0))
                .build())))
        .build();
  }

  @VisibleForTesting
  List<FailureStrategyConfig> getFailureStrategies(YamlNode node) {
    YamlField failureStrategy = node.getField(FAILURE_STRATEGIES);
    List<FailureStrategyConfig> failureStrategyConfigs = null;

    try {
      if (failureStrategy != null) {
        failureStrategyConfigs =
            YamlUtils.read(failureStrategy.getNode().toString(), new TypeReference<List<FailureStrategyConfig>>() {});
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
    return failureStrategyConfigs;
  }
}