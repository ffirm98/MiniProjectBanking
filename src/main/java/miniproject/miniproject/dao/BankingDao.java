package miniproject.miniproject.dao;

import miniproject.miniproject.entity.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public interface BankingDao {
    public BankingEntity createAccount(Connection conn, BankingEntity request) throws Exception;
    List<BankingEntity> lockAccounts(Connection conn, String accountNo1, String accountNo2) throws Exception;
    int updateBalance(Connection conn, String accountNo, BigDecimal delta) throws Exception;
    int updateFailedPin(Connection conn, String accountNo, int attempts, String status) throws Exception;
    TransferResponseEntity findTransferByKey(Connection conn, String idempotencyKey) throws Exception;
    int insertTransfer(Connection conn, TransferResponseEntity transfer) throws Exception;
    int insertLedger(Connection conn, LedgerEntity ledger) throws Exception;
    List<ReportResponseEntity> reportTransfers(Connection conn, String accountNo, String type,
                                               LocalDateTime start, LocalDateTime end, int limit, int offset) throws Exception;
    BankingEntity findAccountByNo(Connection conn, String accountNo) throws Exception;
}
