package io.harness.delegate.beans.ci.vm.steps;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.expression.Expression;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VmServiceDependency {
  @NotNull String identifier;
  String name;
  @NotNull String image;
  @NotNull String logKey;
  ConnectorDetails imageConnector;
  List<String> ports;
  String pullPolicy; // always, if-not-exists or never
  String runAsUser;
  boolean privileged;
  @Expression(ALLOW_SECRETS) private Map<String, String> envVariables;
  @Expression(ALLOW_SECRETS) private List<String> secrets;
}