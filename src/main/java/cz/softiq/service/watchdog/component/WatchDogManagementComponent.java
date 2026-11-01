package cz.softiq.service.watchdog.component;

import static cz.softiq.service.watchdog.model.enums.MembershipEnum.BASIC;
import static cz.softiq.service.watchdog.model.enums.MembershipEnum.PROFESSIONAL;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.DAILY;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.FrequencyEnum.OFTEN;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.clients.CoreClientRepository;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.security.RoleEnum;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogPersistenceService;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.TaxReport;
import cz.softiq.service.realiq.repository.ClientEstateRepository;
import cz.softiq.service.realiq.repository.TaxReportRepository;
import cz.softiq.service.softiq.service.email.IEmailService;
import cz.softiq.service.watchdog.entity.*;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.repository.*;
import cz.softiq.service.watchdog.service.gopay.IGoPayService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class WatchDogManagementComponent
    implements IWatchDogAccountService,
        IWatchDogPersistenceService,
        IEmailService,
        IGoPayService,
        IBeanProvider,
        ICoreSoftiqService,
        IBaseService {
  private static final Set<Integer> PROTECTED_WATCHDOG_IDS = Set.of(666);
  private static final String REGISTRATION_ORIGINS = "svemar04.local:5000,softiq.cz";

  private final WatchDogRepository watchDogRepository;
  private final CoreClientRepository coreClientRepository;
  private final WatchDogEstatesRepository watchDogEstatesRepository;
  private final WatchDogMembershipRepository watchDogMembershipRepository;
  private final WatchDogOldClientRepository watchDogOldClientRepository;
  private final PaymentRepository paymentRepository;
  private final ReportingRepository reportingRepository;
  private final ClientEstateRepository clientEstateRepository;
  private final TaxReportRepository taxReportRepository;

  public int getMaximumSizeForMembership(CoreClientEntity client) {
    return watchDogMembershipRepository.findByIsActiveAndClient(true, client).stream()
        .findFirst()
        .map(this::getMaximumSizeForMembership)
        .orElse(Integer.valueOf(0));
  }

  private int getMaximumSizeForMembership(WatchDogMembershipEntity membership) {
    PromoCodeEnum promoCode = membership.getPromoCode();
    if (promoCode != null
        && promoCode.appliesTo(membership.getMembership())
        && promoCode.getMaxWatchdogs() != null) {
      return promoCode.getMaxWatchdogs();
    }
    return membership.getMembership().getMaxCountOfWatchdogs();
  }

  public Map<String, Object> getPromoCodeInfo(String value, String email) {
    PromoCodeEnum promoCode = PromoCodeEnum.fromString(value);
    Map<String, Object> info = new HashMap<>();
    info.put("valid", promoCode != null);
    if (promoCode == null) {
      info.put("reason", "INVALID");
      return info;
    }

    CoreClientEntity client = getCoreClient(email);
    boolean alreadyRedeemed =
        watchDogMembershipRepository.existsByPromoCodeAndClient(promoCode, client);
    boolean redemptionLimitReached =
        promoCode.getMaxRedemptions() != null
            && !alreadyRedeemed
            && watchDogMembershipRepository.countRedeemersByPromoCode(promoCode)
                >= promoCode.getMaxRedemptions();
    if (redemptionLimitReached) {
      info.put("valid", false);
      info.put("reason", "MAX_REDEMPTIONS");
      return info;
    }

    MembershipEnum membership = promoCode.getMembership();
    Integer maxWatchdogs = promoCode.getMaxWatchdogs();
    if (maxWatchdogs == null && membership != null) {
      maxWatchdogs = membership.getMaxCountOfWatchdogs();
    }

    info.put("membership", membership == null ? null : membership.getName());
    info.put("maxWatchDogs", maxWatchdogs);
    info.put("free", promoCode.getMultiplier().compareTo(BigDecimal.ZERO) == 0);
    info.put("lifetime", promoCode.isLifetime());
    info.put(
        "discountPercent",
        BigDecimal.ONE
            .subtract(promoCode.getMultiplier())
            .multiply(BigDecimal.valueOf(100))
            .stripTrailingZeros());
    return info;
  }

  public CoreClientEntity getClient() {
    return getCoreClient(SecurityContextHolder.getContext().getAuthentication().getName());
  }

  public WatchDogMembershipEntity getWatchDogMembership(String email) {
    return getWatchDogMembership(getCoreClient(email));
  }

  public WatchDogMembershipEntity getWatchDogMembership(CoreClientEntity coreClient) {
    List<WatchDogMembershipEntity> all =
        watchDogMembershipRepository.findByIsActiveAndClient(true, coreClient);

    if (all.isEmpty()) {
      return null;
    } else {
      return all.getFirst();
    }
  }

  public WatchDogEntity getWatchDog(Integer watchDogId) {
    return watchDogRepository.findById(watchDogId).orElse(null);
  }

  public List<WatchDogEntity> getAllWatchDogsByRealiqId(Integer realiqId) {
    return watchDogRepository.findAllByRealiqId(realiqId);
  }

  public WatchDogMembershipEntity getNewWatchDogClient(CoreClientEntity coreClient) {
    var memberships = watchDogMembershipRepository.findByIsActiveAndClient(false, coreClient);
    var newMembership = memberships.get(memberships.size() - 1);
    newMembership.setIsActive(Boolean.valueOf(true));
    watchDogMembershipRepository.save(newMembership);
    return watchDogMembershipRepository.findByIsActiveAndClient(true, coreClient).getFirst();
  }

  public void setWatchDogFields(WatchDogEntity watchdog) {
    if (watchdog.getClient() == null) {
      watchdog.setClient(getClient());
    }
    for (Field field : WatchDogEntity.class.getDeclaredFields()) {
      field.setAccessible(true);
      try {
        if (field.getType().equals(String.class)) {
          String value = (String) field.get(watchdog);
          if (!StringUtils.isEmpty(value)) {
            if (!field.getName().equals("inRange") && !field.getName().equals("exactAddress")) {
              value = value.replace(",", "; ");
            }
            field.set(watchdog, value);
          } else {
            field.set(watchdog, null);
          }
        } else if (field.getType().equals(Boolean.class)) {
          Boolean hasValue = (Boolean) field.get(watchdog);
          if (hasValue == null) {
            hasValue = Boolean.FALSE;
          }
          field.set(watchdog, hasValue);
        }
      } catch (IllegalAccessException e) {
        log.error("Error while getting field value", e);
      }
    }
    watchdog.setSearchAddress(watchdog.getExactAddress());
    watchDogRepository.save(watchdog);
  }

  @Transactional
  public void deleteWatchDog(WatchDogEntity watchDog) {
    if (isProtectedWatchDog(watchDog)) {
      throw new SoftiqException(
          "Watchdog " + watchDog.getId() + " is protected and cannot be deleted");
    }
    watchDogEstatesRepository.deleteAllByWatchdogEntity(watchDog);
    watchDogRepository.delete(watchDog);
  }

  public CoreClientEntity createNewClient(String email, String password, Boolean doUpdate) {
    CoreClientEntity coreClient;
    if (!doUpdate) {
      coreClient = new CoreClientEntity();
      coreClient.setUsername(email);
      coreClient.setEmail(email);
      coreClient.setClientName(email);
      coreClient.setRole(RoleEnum.USER);
      coreClient.setOrigin(REGISTRATION_ORIGINS);
    } else {
      coreClient = getCoreClient(email);
      coreClient.setOrigin(REGISTRATION_ORIGINS);
    }

    encodePassword(password, coreClient);
    coreClientRepository.save(coreClient);
    return coreClient;
  }

  public void softChangeMembershipProcess(
      String email, MembershipEnum membership, BigDecimal amount, PromoCodeEnum promoCode) {
    var coreClient = getCoreClient(email);
    var watchDogClient =
        generateNewMembershipPlan(
            membership, coreClient, Boolean.TRUE, amount, Boolean.FALSE, promoCode);
    watchDogMembershipRepository.save(watchDogClient);
  }

  @Transactional
  public void changeMembership(
      String email, MembershipEnum membership, BigDecimal amount, Boolean stopPreviousPayment) {
    var coreClient = getCoreClient(email);
    WatchDogMembershipEntity currentWatchDogClient = getWatchDogMembershipEntity(coreClient);
    var isNotNull = currentWatchDogClient != null;
    var watchDogMembershipEntity =
        generateNewMembershipPlan(
            membership,
            coreClient,
            Boolean.FALSE,
            amount,
            stopPreviousPayment,
            isNotNull ? currentWatchDogClient.getPromoCode() : null);
    if (isNotNull) {
      int currentMembershipLvl = getMaximumSizeForMembership(currentWatchDogClient);
      int newMembershipLvl = getMaximumSizeForMembership(watchDogMembershipEntity);

      currentWatchDogClient.setIsActive(Boolean.FALSE);
      watchDogMembershipRepository.save(currentWatchDogClient);
      if (currentMembershipLvl > newMembershipLvl) {
        downgrade(coreClient, newMembershipLvl);
      } else {
        upgrade(coreClient, newMembershipLvl, membership);
      }
      BeanUtils.copyProperties(
          currentWatchDogClient,
          watchDogMembershipEntity,
          "id",
          "isActive",
          "pendingMembership",
          "agreedPrice",
          "membership",
          "membershipInfo",
          "validTo",
          "validToFormatted",
          "promoCode");
    }

    watchDogMembershipRepository.save(watchDogMembershipEntity);
    sendVerificationEmail(watchDogMembershipEntity.getClient().getEmail());
  }

  public WatchDogMembershipEntity getWatchDogMembershipEntity(CoreClientEntity coreClient) {
    WatchDogMembershipEntity currentWatchDogClient =
        watchDogMembershipRepository.findByIsActiveAndClient(true, coreClient).getFirst();
    return currentWatchDogClient;
  }

  public void createOrEditNewChildren(WatchDogEntity watchDog, Boolean isNew) {
    try {
      if (watchDog.getOfferType().equals("Prodej")) {
        log.info("Creating child before reko");
        WatchDogEntity watchDogChild1;
        if (!isNew) {
          var watchDogs = getWatchDogChildrenByParentId(watchDog.getId());
          watchDogChild1 = watchDogs.isEmpty() ? new WatchDogEntity() : watchDogs.getFirst();
        } else {
          watchDogChild1 = new WatchDogEntity();
        }

        Integer existingId = watchDogChild1.getId();

        BeanUtils.copyProperties(watchDog, watchDogChild1);

        if (!isNew) {
          watchDogChild1.setId(existingId);
        } else {
          watchDogChild1.setId(null);
        }
        watchDogChild1.setOfferType("Pronájem");
        watchDogChild1.setEmailReportFrequency(SettingsEnums.FrequencyEnum.WEEKLY);
        watchDogChild1.setDoEmailReport(Boolean.FALSE);
        watchDogChild1.setSendOfferAs(SettingsEnums.OfferSettingEnum.ALL);
        watchDogChild1.setIsVisible(Boolean.FALSE);
        watchDogChild1.setParentWatchDogId(watchDog.getId());
        watchDogChild1.setIsFirstSyncCompleted(Boolean.FALSE);
        saveNewChildWatchDog(watchDogChild1);
        log.info("Creating child after reko");
        WatchDogEntity watchDogChild2;
        if (!isNew) {
          var watchDogs = getWatchDogChildrenByParentId(watchDog.getId());
          watchDogChild2 = watchDogs.isEmpty() ? new WatchDogEntity() : watchDogs.get(1);
        } else {
          watchDogChild2 = new WatchDogEntity();
        }

        Integer existingId2 = watchDogChild2.getId();

        BeanUtils.copyProperties(watchDogChild1, watchDogChild2);

        if (!isNew) {
          watchDogChild2.setId(existingId2);
        } else {
          watchDogChild2.setId(null);
        }
        watchDogChild2.setEstateCondition("Novostavba; Po rekonstrukci; Velmi dobrý");
        saveNewChildWatchDog(watchDogChild2);

        log.info("Creating child Prodej");
        WatchDogEntity watchDogChild3;
        if (!isNew) {
          var watchDogs = getWatchDogChildrenByParentId(watchDog.getId());
          watchDogChild3 = watchDogs.size() < 3 ? new WatchDogEntity() : watchDogs.get(2);
        } else {
          watchDogChild3 = new WatchDogEntity();
        }

        Integer existingId3 = watchDogChild3.getId();

        BeanUtils.copyProperties(watchDog, watchDogChild3);

        if (!isNew) {
          watchDogChild3.setId(existingId3);
        } else {
          watchDogChild3.setId(null);
        }
        watchDogChild3.setOfferType("Prodej");
        watchDogChild3.setEmailReportFrequency(SettingsEnums.FrequencyEnum.WEEKLY);
        watchDogChild3.setDoEmailReport(Boolean.FALSE);
        watchDogChild3.setSendOfferAs(SettingsEnums.OfferSettingEnum.ALL);
        watchDogChild3.setIsVisible(Boolean.FALSE);
        watchDogChild3.setParentWatchDogId(watchDog.getId());
        watchDogChild3.setIsFirstSyncCompleted(Boolean.FALSE);
        watchDogChild3.setEstateCondition("Novostavba; Po rekonstrukci; Velmi dobrý");
        saveNewChildWatchDog(watchDogChild3);
      }
    } catch (Throwable e) {
      CoreLogEntity logEntity =
          new CoreLogEntity(
              null,
              LocalDate.now(),
              LocalDateTime.now(),
              "Watchdog children creating failed: " + watchDog.getId());
      saveLog(logEntity);
    }
  }

  public WatchDogMembershipEntity changeUserProfileInfo(
      String email, WatchDogMembershipEntity watchDogMembershipEntity) {
    var coreClient = getCoreClient(email);
    var currentWatchDogClient = getWatchDogMembershipByCoreClient(coreClient);

    Field[] fields = WatchDogMembershipEntity.class.getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      try {
        Object value = field.get(watchDogMembershipEntity);
        if (value != null) {
          field.set(currentWatchDogClient, value);
        }
      } catch (IllegalAccessException e) {
        throw new SoftiqException(e.getMessage());
      }
    }
    watchDogMembershipRepository.save(currentWatchDogClient);
    return currentWatchDogClient;
  }

  public void enableAccount(String email) {
    var coreEntity = coreClientRepository.getCoreClientEntityByClientName(email);
    coreEntity.setEnabled(true);
    coreClientRepository.save(coreEntity);
  }

  public CoreClientEntity getCoreClient(String email) {
    return coreClientRepository.findByUsername(email);
  }

  public Boolean changePassword(String email, String token, String password) {
    var watchDogClient = watchDogMembershipRepository.findByUniqueHash(token);
    if (watchDogClient != null) {
      String userEmailFromDb = watchDogClient.getClient().getEmail();
      if (userEmailFromDb.equals(email)) {
        var coreClient = getCoreClient(userEmailFromDb);
        encodePassword(password, coreClient);
        coreClientRepository.save(coreClient);
        watchDogClient.setUniqueHash(null);
        watchDogMembershipRepository.save(watchDogClient);
        return Boolean.TRUE;
      }
      return Boolean.FALSE;
    }
    return Boolean.FALSE;
  }

  public void sendEmailToResetPw(String email) {
    var coreClient = getCoreClient(email);
    if (coreClient != null) {
      var watchDogClient = getWatchDogMembership(coreClient);
      watchDogClient.setUniqueHash(generateUniqueHash());
      watchDogMembershipRepository.save(watchDogClient);
      resetPasswordEmail(
          "https://softiq.cz/zapomenute-heslo?token=" + watchDogClient.getUniqueHash(),
          coreClient.getEmail());
    }
  }

  @Transactional
  public void deleteAccount(CoreClientEntity coreClient) {
    var watchDogClientAll = watchDogMembershipRepository.findAllByClient(coreClient);
    var watchdogs = watchDogRepository.getAllByClientId(coreClient.getId());
    watchdogs.stream()
        .filter(this::isProtectedWatchDog)
        .findFirst()
        .ifPresent(
            watchdog -> {
              throw new SoftiqException(
                  "Account cannot be deleted because it owns protected watchdog "
                      + watchdog.getId());
            });

    String state = stopRecurringPayment(coreClient, Boolean.TRUE);

    var oldClient =
        new WatchDogOldClientEntity(
            coreClient.getUsername(), watchDogClientAll.getFirst().getIdNumber());

    for (WatchDogEntity watchdogEntity : watchdogs) {
      deleteWatchDog(watchdogEntity);
    }

    reportingRepository.deleteAllByEmailTo(coreClient.getEmail());

    var estates = getClientEstates(coreClient);

    for (ClientEstate estate : estates) {
      deleteClientEstate(estate);
    }

    List<PaymentEntity> payments = paymentRepository.findByClient(coreClient);
    var defaultClient = getCoreClient("default@softiq.cz");
    if (!payments.isEmpty()) {
      for (PaymentEntity payment : payments) {
        payment.setNote(payment.getNote() + " " + state + " " + coreClient.getUsername());
        payment.setClient(defaultClient);
        paymentRepository.save(payment);
      }
    }
    for (WatchDogMembershipEntity watchDogMembershipEntity : watchDogClientAll) {
      watchDogMembershipEntity.setClient(defaultClient);
      watchDogMembershipEntity.setIsActive(Boolean.FALSE);
      watchDogMembershipEntity.setNote(coreClient.getUsername());
      watchDogMembershipRepository.save(watchDogMembershipEntity);
    }
    coreClientRepository.delete(coreClient);
    watchDogOldClientRepository.save(oldClient);
    stopRecurringPayment(coreClient, Boolean.TRUE);
    deactivateAccount(coreClient.getEmail());
  }

  private boolean isProtectedWatchDog(WatchDogEntity watchDog) {
    return watchDog != null
        && watchDog.getId() != null
        && PROTECTED_WATCHDOG_IDS.contains(watchDog.getId());
  }

  public boolean getIfIsNewEmail(String email) {
    var oldAccounts = watchDogOldClientRepository.findByEmail(email);
    return oldAccounts.isEmpty();
  }

  public void sendVerificationEmail(String email) {
    var newClient = coreClientRepository.getCoreClientEntityByClientName(email);
    var watchDogClient = getWatchDogMembership(newClient);
    MembershipEnum membership = watchDogClient.getMembership();
    BigDecimal price = getPrice(membership, watchDogClient, null);
    var helperOldPrice = getOldPrice(newClient);
    String oldPrice = helperOldPrice == null ? null : String.valueOf(helperOldPrice);
    changeMembership(email, membership, String.valueOf(price), oldPrice, watchDogClient);
  }

  public BigDecimal getOldPrice(CoreClientEntity coreClient) {
    var memberships = watchDogMembershipRepository.findByIsActiveAndClient(false, coreClient);
    if (memberships.size() < 2) {
      return null;
    }
    var oldMembership = memberships.get(memberships.size() - 1);
    return oldMembership.getAgreedPrice();
  }

  public BigDecimal getPrice(
      MembershipEnum membership,
      WatchDogMembershipEntity watchDogMembershipEntity,
      PromoCodeEnum promoCodeNew) {

    BigDecimal price = membership.getPrice();

    PromoCodeEnum promoToApply = null;
    if (watchDogMembershipEntity != null && watchDogMembershipEntity.getPromoCode() != null) {
      promoToApply = watchDogMembershipEntity.getPromoCode();
    } else if (promoCodeNew != null) {
      promoToApply = promoCodeNew;
    }

    if (promoToApply != null && promoToApply.appliesTo(membership)) {
      price = price.multiply(promoToApply.getMultiplier());
    }
    BigDecimal roundedPrice = price.setScale(0, RoundingMode.DOWN);
    return roundedPrice.setScale(2, RoundingMode.DOWN);
  }

  public void preSaveMembership(String email, MembershipEnum membership, PromoCodeEnum promoCode) {
    var coreClient = getCoreClient(email);
    var watchDogClient = getWatchDogMembership(coreClient);
    if (watchDogClient != null) {
      watchDogClient.setPromoCode(promoCode);
      watchDogClient.setPendingMembership(membership);
      watchDogMembershipRepository.save(watchDogClient);
    }
  }

  public void disableWatchdogs(WatchDogMembershipEntity watchDogMembershipEntity) {
    var watchdogs =
        watchDogRepository.getAllByClientIdAndIsVisible(
            watchDogMembershipEntity.getClient().getId(), Boolean.TRUE);
    for (WatchDogEntity watchdogEntity : watchdogs) {
      watchdogEntity.setIsEnabled(Boolean.FALSE);
      watchDogRepository.save(watchdogEntity);
    }
  }

  public void deleteOldResults(WatchDogEntity watchDogEntity) {
    LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
    var watchDogEstates =
        watchDogEstatesRepository.findByWatchdogEntityAndCreatedAtBefore(
            watchDogEntity, threeMonthsAgo);
    watchDogEstatesRepository.deleteAll(watchDogEstates);
  }

  public String joinStringList(Object obj) {
    if (obj == null) return null;

    if (obj instanceof String) {
      return (String) obj;
    } else if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      return list.stream().map(Object::toString).collect(Collectors.joining(","));
    } else {
      throw new SoftiqException("Unsupported type: " + obj.getClass());
    }
  }

  public BigDecimal toBigDecimal(Object value, int numberOfDecimals) {
    if (value == null) return null;

    BigDecimal result;

    if (value instanceof BigDecimal) {
      result = (BigDecimal) value;
    } else {
      result = new BigDecimal(value.toString());
    }

    return result.setScale(numberOfDecimals, RoundingMode.HALF_UP);
  }

  public List<WatchDogEntity> getAllWatchDogsWithFrequency(
      SettingsEnums.FrequencyEnum frequencyEnum) {
    return watchDogRepository.findAllByEmailReportFrequency(frequencyEnum);
  }

  public List<WatchDogEntity> getAllWatchDogChildren() {
    return watchDogRepository.findAllByParentWatchDogIdIsNotNull();
  }

  public List<WatchDogEstatesEntity> getNewWatchDogEstateEntities(WatchDogEntity watchDog) {
    return watchDogEstatesRepository.findNewWatchDogEstates(watchDog);
  }

  private static void encodePassword(String password, CoreClientEntity coreClient) {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    String encryptedPassword = passwordEncoder.encode(password);
    coreClient.setPassword(encryptedPassword);
  }

  private void upgrade(
      CoreClientEntity coreClient, int newMembershipLvl, MembershipEnum membership) {
    var allWatchDogs =
        watchDogRepository.getAllByClientIdAndIsVisible(coreClient.getId(), Boolean.TRUE);
    allWatchDogs.sort(Comparator.comparing(WatchDogEntity::getId));
    for (int i = 0; i < allWatchDogs.size(); i++) {
      if (i == newMembershipLvl) {
        break;
      }
      WatchDogEntity watchDog = allWatchDogs.get(i);
      watchDog.setIsEnabled(Boolean.TRUE);
      watchDogRepository.save(watchDog);
    }

    if (membership.equals(PROFESSIONAL) || membership.equals(BASIC))
      for (WatchDogEntity watchDog : allWatchDogs) {
        createOrEditNewChildren(watchDog, Boolean.TRUE);
      }
  }

  private void downgrade(CoreClientEntity coreClient, int newMembershipLvl) {
    var allWatchDogs =
        watchDogRepository.getAllByClientIdAndIsVisible(coreClient.getId(), Boolean.TRUE);
    allWatchDogs.sort(Comparator.comparing(WatchDogEntity::getId));
    for (int i = newMembershipLvl; i < allWatchDogs.size(); i++) {
      WatchDogEntity watchDog = allWatchDogs.get(i);
      deleteWatchDog(watchDog);
    }

    var topTierDogs =
        watchDogRepository.getAllByClientIdAndIsVisible(coreClient.getId(), Boolean.FALSE);
    for (WatchDogEntity watchDog : topTierDogs) {
      deleteWatchDog(watchDog);
    }

    var theRestOfWatchDogs =
        watchDogRepository.getAllByClientIdAndIsVisible(coreClient.getId(), Boolean.TRUE);
    var newFrequency = newMembershipLvl == 2 ? DAILY : OFTEN;
    List<SettingsEnums.FrequencyEnum> allowedFrequency =
        newMembershipLvl == 2
            ? List.of(SettingsEnums.FrequencyEnum.DAILY)
            : List.of(SettingsEnums.FrequencyEnum.DAILY, SettingsEnums.FrequencyEnum.OFTEN);

    for (WatchDogEntity watchDog : theRestOfWatchDogs) {
      var oldFrequency = watchDog.getEmailReportFrequency();
      if (!allowedFrequency.contains(oldFrequency) && oldFrequency != newFrequency) {
        watchDog.setEmailReportFrequency(newFrequency);
        watchDogRepository.save(watchDog);
      }
    }
  }

  private void saveNewChildWatchDog(WatchDogEntity watchDog) {
    setWatchDogFields(watchDog);
  }

  private WatchDogMembershipEntity generateNewMembershipPlan(
      MembershipEnum membershipEnum,
      CoreClientEntity coreClient,
      Boolean isPendingMembership,
      BigDecimal amount,
      Boolean stopPreviousPayment,
      PromoCodeEnum promoCode) {
    if (stopPreviousPayment) {
      stopRecurringPayment(coreClient, Boolean.FALSE);
    }
    var watchDogClient = new WatchDogMembershipEntity();
    LocalDateTime now = LocalDateTime.now();
    PromoCodeEnum appliedPromo =
        promoCode != null && promoCode.appliesTo(membershipEnum) ? promoCode : null;
    LocalDateTime validTo =
        appliedPromo != null && appliedPromo.isLifetime()
            ? LocalDateTime.of(9999, 12, 31, 23, 59)
            : BASIC.equals(membershipEnum) ? now.plusDays(14) : now.plusMonths(1);
    watchDogClient.setMembership(isPendingMembership ? MembershipEnum.PENDING : membershipEnum);
    watchDogClient.setPromoCode(appliedPromo);
    watchDogClient.setAgreedPrice(amount);
    watchDogClient.setPendingMembership(isPendingMembership ? membershipEnum : null);
    watchDogClient.setIsActive(Boolean.valueOf(!isPendingMembership));
    watchDogClient.setValidFrom(LocalDateTime.now());
    watchDogClient.setValidTo(validTo);
    watchDogClient.setClient(coreClient);
    watchDogClient.setValidToFormatted(
        validTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    watchDogClient.setMembershipInfo(
        appliedPromo != null && appliedPromo.getMaxWatchdogs() != null
            ? appliedPromo.getMaxWatchdogs() + " hlídacích psů"
            : membershipEnum.getMaxCountOfWatchdogsText());
    return watchDogClient;
  }

  private String generateUniqueHash() {
    return UUID.randomUUID().toString();
  }

  public List<WatchDogMembershipEntity> getAllPastPayments(LocalDateTime date) {
    return new ArrayList<>(
        watchDogMembershipRepository.getAllByIsActiveAndValidToBeforeAndMembershipIsNot(
            true, date, BASIC));
  }

  public BigDecimal getFixedPrice(CoreClientEntity client) {
    return watchDogMembershipRepository.findAllByClient(client).getFirst().getAgreedPrice();
  }

  private List<ClientEstate> getClientEstates(CoreClientEntity client) {
    return clientEstateRepository.findAllByClient(client);
  }

  private void deleteClientEstate(ClientEstate estate) {
    List<TaxReport> taxReports = taxReportRepository.findTaxReportByEstate(estate);
    taxReportRepository.deleteAll(taxReports);
    clientEstateRepository.delete(estate);
  }
}
