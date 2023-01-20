package io.harness.ci.enforcement;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Slf4j
public class HostedCreditsPerMonthRestriction implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject private TimeScaleDBService timeScaleDBService;
  String GET_TOTAL_CREDITS_USED = "SELECT SUM(buildMultiplier*cputime/60000.0) FROM public.stage_execution_summary_ci\n"
      + "where startts > %s and accountidentifier='%s'\n"
      + "and infratype='HostedVm'";
  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    double creditsUsed = 0;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      String query = String.format(GET_TOTAL_CREDITS_USED, getMonthStartTime(), accountIdentifier);
      ResultSet resultSet = statement.executeQuery(query);
      creditsUsed = 0d;
      while (resultSet.next()) {
        creditsUsed = resultSet.getDouble(1);
      }
    } catch (Exception e) {
      log.error("Exception while fetching the current hosted credit usage for this account.");
    }

    return (long) creditsUsed;
  }

  public static Long getMonthStartTime() {
    LocalDate today = LocalDate.now();
    LocalDate dayOne = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
    return dayOne.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
  }
}
