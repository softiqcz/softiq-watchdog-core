package cz.softiq.service.realiq.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioMonthlySummary {
  // Verified transactions
  private int verifiedTransactionCount;
  private BigDecimal verifiedIncomeTotal;
  private BigDecimal verifiedExpenseTotal;
  private BigDecimal verifiedNetIncome; // income - expense

  // Unverified transactions
  private int unverifiedTransactionCount;
  private BigDecimal unverifiedIncomeTotal;
  private BigDecimal unverifiedExpenseTotal;
  private BigDecimal unverifiedNetIncome;

  // Total (verified + unverified)
  private int totalTransactionCount;
  private BigDecimal totalIncomeTotal;
  private BigDecimal totalExpenseTotal;
  private BigDecimal totalNetIncome;

  // Estate information
  private int estateCount;
  private String estateName; // For single estate summary

  public boolean hasUnverifiedTransactions() {
    return unverifiedTransactionCount > 0;
  }

  public boolean hasVerifiedTransactions() {
    return verifiedTransactionCount > 0;
  }

  public boolean hasTransactions() {
    return totalTransactionCount > 0;
  }

  public String getVerificationStatusText() {
    if (verifiedTransactionCount == 0 && unverifiedTransactionCount == 0) {
      return "Žádné transakce";
    } else if (unverifiedTransactionCount == 0) {
      return "Všechny transakce ověřeny ✓";
    } else if (verifiedTransactionCount == 0) {
      return "Žádná transakce není ověřena";
    } else {
      return verifiedTransactionCount
          + " ověřených, "
          + unverifiedTransactionCount
          + " neověřených";
    }
  }

  public String getUnverifiedPromptText() {
    if (unverifiedTransactionCount == 0) {
      return "";
    } else if (unverifiedTransactionCount == 1) {
      return "Máte 1 neověřenou transakci";
    } else if (unverifiedTransactionCount < 5) {
      return "Máte " + unverifiedTransactionCount + " neověřené transakce";
    } else {
      return "Máte " + unverifiedTransactionCount + " neověřených transakcí";
    }
  }
}
