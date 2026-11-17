package miniproject.miniproject.dao.impl;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import miniproject.miniproject.entity.LedgerEntity;
import miniproject.miniproject.entity.ReportResponseEntity;
import miniproject.miniproject.entity.TransferResponseEntity;
import org.springframework.stereotype.Repository;

import miniproject.miniproject.constand.ConstandSQL;
import miniproject.miniproject.dao.BankingDao;
import miniproject.miniproject.entity.BankingEntity;

@Repository
public class BankingDaoImpl implements BankingDao {

    @Override
    public BankingEntity createAccount(Connection conn, BankingEntity entity) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.INSERT_ACCOUNT)) {
            ps.setString(1, entity.getAccountNo());
            ps.setString(2, entity.getOwnerName());
            ps.setString(3, entity.getCurrency());
            ps.setBigDecimal(4, entity.getInitialBalance());
            ps.setString(5, entity.getStatus());
            ps.setString(6, entity.getPinHash());
            ps.executeUpdate();
            return entity;
        }
    }

    @Override
    public List<BankingEntity> lockAccounts(Connection conn, String no1, String no2) throws Exception {
        List<BankingEntity> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.LOCK_ACCOUNTS)) {
            ps.setString(1, no1);
            ps.setString(2, no2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BankingEntity e = new BankingEntity();
                    e.setAccountNo(rs.getString("account_no"));
                    e.setOwnerName(rs.getString("owner_name"));
                    e.setCurrency(rs.getString("currency"));
                    e.setBalance(rs.getBigDecimal("balance"));
                    e.setStatus(rs.getString("status"));
                    e.setPinHash(rs.getString("pin_hash"));
                    e.setFailedPinAttempts(rs.getInt("failed_pin_attempts"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    @Override
    public int updateBalance(Connection conn, String accountNo, BigDecimal delta) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.UPDATE_BALANCE)) {
            ps.setBigDecimal(1, delta);
            ps.setString(2, accountNo);
            return ps.executeUpdate();
        }
    }

    @Override
    public int updateFailedPin(Connection conn, String accountNo, int attempts, String status) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.UPDATE_FAILED_PIN)) {
            ps.setInt(1, attempts);
            ps.setString(2, status);
            ps.setString(3, accountNo);
            return ps.executeUpdate();
        }
    }

    @Override
    public TransferResponseEntity findTransferByKey(Connection conn, String key) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.FIND_TRANSFER_BY_KEY)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                TransferResponseEntity t = new TransferResponseEntity();
                t.setIdempotencyKey(rs.getString("idempotency_key"));
                t.setFromAccountNo(rs.getString("from_account_no"));
                t.setToAccountNo(rs.getString("to_account_no"));
                t.setAmount(rs.getBigDecimal("amount"));
                t.setCurrency(rs.getString("currency"));
                t.setNote(rs.getString("note"));
                t.setStatus(rs.getString("status"));
                t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return t;
            }
        }
    }

    @Override
    public int insertTransfer(Connection conn, TransferResponseEntity t) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.INSERT_TRANSFER)) {
            ps.setString(1, t.getTransferId());
            ps.setString(2, t.getIdempotencyKey());
            ps.setString(3, t.getFromAccountNo());
            ps.setString(4, t.getToAccountNo());
            ps.setBigDecimal(5, t.getAmount());
            ps.setString(6, t.getCurrency());
            ps.setString(7, t.getNote());
            ps.setString(8, t.getStatus());
            ps.setTimestamp(9, Timestamp.valueOf(t.getCreatedAt()));
            return ps.executeUpdate();
        }
    }

    @Override
    public int insertLedger(Connection conn, LedgerEntity l) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.INSERT_LEDGER)) {
            ps.setString(1, l.getTransferId());
            ps.setString(2, l.getAccountNo());
            ps.setString(3, l.getDirection());
            ps.setBigDecimal(4, l.getAmount());
            ps.setBigDecimal(5, l.getBalanceAfter());
            return ps.executeUpdate();
        }
    }

    @Override
    public List<ReportResponseEntity> reportTransfers(Connection conn, String accountNo, String type,
                                                      LocalDateTime start, LocalDateTime end, int limit, int offset) throws Exception {
        List<ReportResponseEntity> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.REPORT_TRANSFER)) {
            ps.setString(1, accountNo);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));
            ps.setString(4, type);
            ps.setString(5, type);
            ps.setInt(6, limit);
            ps.setInt(7, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReportResponseEntity r = new ReportResponseEntity();
                    r.setTransferId(rs.getString("transferId"));
                    r.setType(rs.getString("type"));
                    r.setAccountNo(rs.getString("accountNo"));
                    r.setAccountName(rs.getString("accountName"));
                    r.setCounterpartAccountNo(rs.getString("counterpartAccountNo"));
                    r.setCounterpartName(rs.getString("counterpartName"));
                    r.setAmount(rs.getBigDecimal("amount"));
                    r.setBalance(rs.getBigDecimal("balance"));
                    r.setCurrency(rs.getString("currency"));
                    r.setNote(rs.getString("note"));
                    r.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
                    list.add(r);
                }
            }
        }
        return list;
    }

    @Override
    public BankingEntity findAccountByNo(Connection conn, String accountNo) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(ConstandSQL.FIND_ACCOUNT_BY_NO)) {
            ps.setString(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                BankingEntity e = new BankingEntity();
                e.setAccountNo(rs.getString("account_no"));
                e.setOwnerName(rs.getString("owner_name"));
                e.setCurrency(rs.getString("currency"));
                e.setBalance(rs.getBigDecimal("balance"));
                e.setStatus(rs.getString("status"));
                return e;
            }
        }
    }
}
