package miniproject.miniproject.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportResponseEntity {
    private String transferId;
    private String type;                  // D atau C, dari sisi pemilik
    private String accountNo;
    private String accountName;
    private String counterpartAccountNo;
    private String counterpartName;
    private BigDecimal amount;
    private BigDecimal balance;           // saldo pemilik setelah transaksi
    private String currency;
    private String note;
    private LocalDateTime createdAt;

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCounterpartName() {
        return counterpartName;
    }

    public void setCounterpartName(String counterpartName) {
        this.counterpartName = counterpartName;
    }

    public String getCounterpartAccountNo() {
        return counterpartAccountNo;
    }

    public void setCounterpartAccountNo(String counterpartAccountNo) {
        this.counterpartAccountNo = counterpartAccountNo;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
