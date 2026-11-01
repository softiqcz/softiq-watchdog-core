package cz.softiq.service.realiq.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransactionVerification {
  private List<String> listOfVerifiedTransactions = new ArrayList<>();
}
