package cz.softiq.service.softiq.service.reporting;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.softiq.core.email.CoreLogRepository;
import cz.softiq.core.email.CoreReportingRepository;
import cz.softiq.service.softiq.model.projection.ReportEmailProjection;
import cz.softiq.service.softiq.repository.TestReportRepository;
import cz.softiq.service.watchdog.repository.ReportingRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.testng.annotations.Test;

public class ReportingPersistenceComponentTest {
  private ReportingPersistenceComponent component(
      ReportingRepository reads, CoreReportingRepository writes) {
    return new ReportingPersistenceComponent(
        writes, reads, mock(TestReportRepository.class), mock(CoreLogRepository.class));
  }

  @Test
  public void reportListUsesMetadataProjectionAndDoesNotLoadEntitiesOrBodies() throws Exception {
    ReportingRepository reads = mock(ReportingRepository.class);
    CoreReportingRepository writes = mock(CoreReportingRepository.class);
    ReportEmailProjection report =
        new SpelAwareProxyProjectionFactory()
            .createProjection(
                ReportEmailProjection.class,
                Map.of(
                    "id",
                    7,
                    "emailTo",
                    "recipient@example.com",
                    "emailFrom",
                    "sender@example.com",
                    "subject",
                    "Test",
                    "body",
                    "large HTML",
                    "createdDate",
                    "unused"));
    when(reads.findAllProjectedByOrderByCreatedDateTimeDescIdDesc()).thenReturn(List.of(report));
    Iterable<ReportEmailProjection> result = component(reads, writes).getAllSentReports();
    Map<?, ?> serialized =
        new ObjectMapper()
            .readValue(new ObjectMapper().writeValueAsString(result.iterator().next()), Map.class);
    assertEquals(
        serialized.keySet(), Set.of("id", "createdDateTime", "emailTo", "emailFrom", "subject"));
    assertEquals(serialized.get("id"), 7);
    verify(reads).findAllProjectedByOrderByCreatedDateTimeDescIdDesc();
    verifyNoMoreInteractions(reads);
    verifyNoInteractions(writes);
  }

  @Test
  public void previewLoadsOnlyTheSelectedBody() {
    ReportingRepository reads = mock(ReportingRepository.class);
    CoreReportingRepository writes = mock(CoreReportingRepository.class);
    when(reads.findBodyById(7)).thenReturn(Optional.of("<html>Selected report</html>"));
    assertEquals(
        component(reads, writes).getReportBody(Map.of("data", Map.of("id", 7))),
        "<html>Selected report</html>");
    verify(reads).findBodyById(7);
    verifyNoMoreInteractions(reads);
    verifyNoInteractions(writes);
  }

  @Test
  public void invalidIdsAreRejectedBeforeQuerying() {
    ReportingRepository reads = mock(ReportingRepository.class);
    ReportingPersistenceComponent component = component(reads, mock(CoreReportingRepository.class));
    for (Object id : List.of(0, -1, 1.5, "7", 2147483648L)) {
      expectThrows(
          IllegalArgumentException.class,
          () -> component.getReportBody(Map.of("data", Map.of("id", id))));
    }
    expectThrows(IllegalArgumentException.class, () -> component.getReportBody(Map.of()));
    verifyNoInteractions(reads);
  }

  @Test
  public void deletedReportProducesAnError() {
    ReportingRepository reads = mock(ReportingRepository.class);
    when(reads.findBodyById(7)).thenReturn(Optional.empty());
    expectThrows(
        IllegalArgumentException.class,
        () ->
            component(reads, mock(CoreReportingRepository.class))
                .getReportBody(Map.of("data", Map.of("id", 7))));
  }
}
