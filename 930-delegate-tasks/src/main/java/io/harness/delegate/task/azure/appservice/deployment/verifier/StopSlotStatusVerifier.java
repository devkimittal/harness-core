/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.deployment.verifier;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.delegate.task.azure.appservice.deployment.context.StatusVerifierContext;
import io.harness.logging.LogCallback;

import com.azure.core.http.rest.Response;
import reactor.core.publisher.Mono;

@OwnedBy(CDP)
public class StopSlotStatusVerifier extends SlotStatusVerifier {
  public StopSlotStatusVerifier(LogCallback logCallback, String slotName, AzureWebClient azureWebClient,
      AzureWebClientContext azureWebClientContext, Mono<Response<Void>> responseMono) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, responseMono);
  }

  public StopSlotStatusVerifier(StatusVerifierContext context) {
    super(context.getLogCallback(), context.getSlotName(), context.getAzureWebClient(),
        context.getAzureWebClientContext(), context.getResponseMono());
  }

  @Override
  public String getSteadyState() {
    return SlotStatus.STOPPED.name();
  }
}
