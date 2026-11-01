package cz.softiq.service.watchdog.service.invoice;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.InvoiceEntity;

public interface IInvoiceService extends IBeanProvider, IBaseService {

  private InvoiceService getInvoiceService() {
    return getBean(InvoiceService.class);
  }

  default Integer getNewInvoiceNumber() {
    return getInvoiceService().getNewInvoiceNumber();
  }

  default void saveInvoice(InvoiceEntity invoiceEntity) {
    getInvoiceService().saveInvoice(invoiceEntity);
  }
}
