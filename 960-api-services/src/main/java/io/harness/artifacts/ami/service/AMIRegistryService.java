/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.ami.service;
import io.harness.ami.AMITagsResponse;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.aws.beans.AwsInternalConfig;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ARTIFACTS})
public interface AMIRegistryService {
  List<BuildDetails> listBuilds(AwsInternalConfig awsInternalConfig, String region, Map<String, List<String>> tags,
      Map<String, String> filters, String versionRegex);

  BuildDetails getLastSuccessfulBuild(AwsInternalConfig awsInternalConfig, String region,
      Map<String, List<String>> tags, Map<String, String> filters, String versionRegex);

  BuildDetails getBuild(AwsInternalConfig awsInternalConfig, String region, Map<String, List<String>> tags,
      Map<String, String> filters, String version);

  AMITagsResponse listTags(AwsInternalConfig awsInternalConfig, String region);
}
