/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PL)
public class InvalidEntityException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public InvalidEntityException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_REQUEST, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }
}
