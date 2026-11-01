package cz.softiq.service.softiq.model.projection;

import java.time.LocalDateTime;

public interface ReportEmailProjection {
  Integer getId();

  LocalDateTime getCreatedDateTime();

  String getEmailTo();

  String getEmailFrom();

  String getSubject();
}
