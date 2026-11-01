package cz.softiq.service.watchdog.service.invoice;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.softiq.service.jwt.IJwtService;
import cz.softiq.service.softiq.service.user.IUserService;
import cz.softiq.service.watchdog.entity.InvoiceEntity;
import cz.softiq.service.watchdog.service.gopay.IGoPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService
    implements IBaseService,
        IUserService,
        IJwtService,
        IWatchDogAccountService,
        IGoPayService,
        IBeanProvider,
        ICoreSoftiqService {
  private final InvoiceComponent invoiceComponent;

  Integer getLastInvoiceNumber() {
    return invoiceComponent.getLastInvoiceNumber();
  }

  Integer getNewInvoiceNumber() {
    return invoiceComponent.getNewInvoiceNumber();
  }

  void saveInvoice(InvoiceEntity invoiceEntity) {
    invoiceComponent.saveInvoice(invoiceEntity);
  }
}
