/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngmigration.beans.ImportMechanism;
import io.harness.ngmigration.beans.InputDefaults;

import software.wings.ngmigration.NGMigrationEntityType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SECRET")
public class SecretFilter extends Filter {
  @Parameter(
      description =
          "ALL: To migrate all secrets. ID: TO migrate only specific connectors. Specific type is current not supported")
  @NotNull
  private ImportMechanism mechanism;

  @Parameter(description = "To be provided if mechanism is ID") private List<String> ids;

  @Parameter(description = "The defaults for every entity. By default every entity is scoped to project.")
  private Map<NGMigrationEntityType, InputDefaults> defaults;
}