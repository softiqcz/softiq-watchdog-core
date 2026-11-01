package cz.softiq.service.watchdog.service.invoice;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;

import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.softiq.service.jwt.IJwtService;
import cz.softiq.service.softiq.service.user.IUserService;
import cz.softiq.service.watchdog.entity.InvoiceEntity;
import cz.softiq.service.watchdog.repository.InvoiceRepository;
import cz.softiq.service.watchdog.service.gopay.IGoPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class InvoiceComponent
    implements IUserService, IJwtService, IWatchDogAccountService, IGoPayService {
  private final InvoiceRepository invoiceRepository;

  public Integer getLastInvoiceNumber() {
    var lastInvoice = invoiceRepository.findTop1ByOrderByInvoiceNumberDesc();
    return lastInvoice == null ? 0 : lastInvoice.getInvoiceNumber();
  }

  public Integer getNewInvoiceNumber() {
    return getLastInvoiceNumber() + 1;
  }

  public void saveInvoice(InvoiceEntity invoiceEntity) {
    invoiceRepository.save(invoiceEntity);
  }
}
