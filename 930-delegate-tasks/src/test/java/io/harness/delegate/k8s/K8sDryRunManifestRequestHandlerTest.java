/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.PRATYUSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sDryRunManifestRequest;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.exception.KubernetesCliTaskRuntimeException;
import io.harness.helpers.k8s.releasehistory.K8sReleaseHandler;
import io.harness.k8s.KubernetesReleaseDetails;
import io.harness.k8s.ProcessResponse;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.releasehistory.IK8sReleaseHistory;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.util.List;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class K8sDryRunManifestRequestHandlerTest extends CategoryTest {
  @Mock K8sTaskHelperBase taskHelperBase;
  @Mock ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Mock K8sReleaseHandler legacyReleaseHandler;
  @Mock K8sReleaseHandler declarativeReleaseHandler;
  @Mock IK8sReleaseHistory releaseHistory;

  @InjectMocks @Spy K8sDryRunManifestRequestHandler dryRunRequestHandler;
  @InjectMocks @Spy K8sRollingBaseHandler baseHandler;

  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @Mock LogCallback logCallback;

  @Captor ArgumentCaptor<List<KubernetesResourceId>> captor;
  final CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    doReturn(logCallback)
        .when(taskHelperBase)
        .getLogCallback(eq(logStreamingTaskClient), anyString(), anyBoolean(), eq(commandUnitsProgress));

    doReturn(KubernetesConfig.builder().namespace("default").build())
        .when(containerDeploymentDelegateBaseHelper)
        .createKubernetesConfig(any(K8sInfraDelegateConfig.class), anyString(), any(LogCallback.class));

    doReturn(legacyReleaseHandler).when(taskHelperBase).getReleaseHandler(false);
    doReturn(declarativeReleaseHandler).when(taskHelperBase).getReleaseHandler(true);
    doReturn(releaseHistory).when(legacyReleaseHandler).getReleaseHistory(any(KubernetesConfig.class), anyString());
    doReturn(releaseHistory)
        .when(declarativeReleaseHandler)
        .getReleaseHistory(any(KubernetesConfig.class), anyString());
    doReturn(false).when(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testExecuteTask() throws Exception {
    K8sDryRunManifestRequest dryRunManifestRequest =
        K8sDryRunManifestRequest.builder()
            .releaseName("releaseName")
            .k8sInfraDelegateConfig(mock(K8sInfraDelegateConfig.class))
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().kustomizeDirPath("dir").build())
            .build();

    testExecuteTask(dryRunManifestRequest);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testExecuteTaskFailedScenario() throws Exception {
    K8sDryRunManifestRequest dryRunManifestRequest =
        K8sDryRunManifestRequest.builder()
            .releaseName("releaseName")
            .k8sInfraDelegateConfig(mock(K8sInfraDelegateConfig.class))
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().kustomizeDirPath("dir").build())
            .build();

    ProcessResponse response =
        ProcessResponse.builder()
            .processResult(new ProcessResult(1, new ProcessOutput("Something went wrong".getBytes())))
            .build();
    doReturn(response).when(taskHelperBase).runK8sExecutable(any(), any(), any());
    Kubectl client = Kubectl.client("kubectl", "config-path");

    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    doReturn(client).when(taskHelperBase).getOverriddenClient(any(), any(), eq(k8sDelegateTaskParams));
    assertThatThrownBy(()
                           -> dryRunRequestHandler.executeTask(dryRunManifestRequest, k8sDelegateTaskParams,
                               logStreamingTaskClient, commandUnitsProgress))
        .matches(throwable -> {
          KubernetesCliTaskRuntimeException taskException = (KubernetesCliTaskRuntimeException) throwable;
          assertThat(taskException.getProcessResponse().getProcessResult().outputUTF8())
              .contains("Something went wrong");
          assertThat(taskException.getProcessResponse().getProcessResult().getExitValue()).isEqualTo(1);
          return true;
        });
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testExecuteTaskUsingReleaseHistoryLegacyRollback() {
    final K8sDryRunManifestRequest dryRunManifestRequest =
        K8sDryRunManifestRequest.builder()
            .releaseName("releaseName")
            .k8sInfraDelegateConfig(mock(K8sInfraDelegateConfig.class))
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().kustomizeDirPath("dir").build())
            .useDeclarativeRollback(false)
            .build();
    final KubernetesReleaseDetails expectedReleaseDetails = KubernetesReleaseDetails.builder().releaseNumber(1).build();

    doReturn(1).when(releaseHistory).getNextReleaseNumber(false);

    testExecuteTask(dryRunManifestRequest);

    verify(legacyReleaseHandler).getReleaseHistory(any(KubernetesConfig.class), eq("releaseName"));
    verify(dryRunRequestHandler).getManifestOverrideFlies(dryRunManifestRequest, expectedReleaseDetails.toContextMap());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  @SneakyThrows
  public void testExecuteTaskUsingReleaseHistoryDeclarativeRollback() {
    final K8sDryRunManifestRequest dryRunManifestRequest =
        K8sDryRunManifestRequest.builder()
            .releaseName("releaseName")
            .k8sInfraDelegateConfig(mock(K8sInfraDelegateConfig.class))
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().kustomizeDirPath("dir").build())
            .useDeclarativeRollback(true)
            .build();
    final KubernetesReleaseDetails expectedReleaseDetails = KubernetesReleaseDetails.builder().releaseNumber(3).build();

    doReturn(3).when(releaseHistory).getNextReleaseNumber(false);

    testExecuteTask(dryRunManifestRequest);

    verify(declarativeReleaseHandler).getReleaseHistory(any(KubernetesConfig.class), eq("releaseName"));
    verify(dryRunRequestHandler).getManifestOverrideFlies(dryRunManifestRequest, expectedReleaseDetails.toContextMap());
  }

  @SneakyThrows
  private void testExecuteTask(K8sDryRunManifestRequest dryRunManifestRequest) {
    ProcessResponse response =
        ProcessResponse.builder().processResult(new ProcessResult(0, new ProcessOutput("abc".getBytes()))).build();
    doReturn(response).when(taskHelperBase).runK8sExecutable(any(), any(), any());
    Kubectl client = Kubectl.client("kubectl", "config-path");

    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    doReturn(client).when(taskHelperBase).getOverriddenClient(any(), any(), eq(k8sDelegateTaskParams));
    K8sDeployResponse k8sDeployResponse = dryRunRequestHandler.executeTask(
        dryRunManifestRequest, k8sDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);
    assertThat(k8sDeployResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(k8sDeployResponse.getK8sNGTaskResponse()).isNotNull();
  }
}
