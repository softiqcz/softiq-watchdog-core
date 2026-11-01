package cz.softiq.service.watchdog.entity;

import cz.softiq.common.base.model.IBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "watchdog_old_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchDogOldClientEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column private String email;
  @Column private String idNumber;
  @Column private String note;

  public WatchDogOldClientEntity(String email, String idNumber) {
    this.email = email;
    this.idNumber = idNumber;
  }
}
