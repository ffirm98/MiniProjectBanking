package miniproject.miniproject.service.impl;

import miniproject.miniproject.DbConnection;
import miniproject.miniproject.dao.BankingDao;
import miniproject.miniproject.entity.*;
import miniproject.miniproject.service.BankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class BankingServiceImpl extends DbConnection implements BankingService {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private BankingDao bankingDao;

    private static final Set<String> WEAK_PINS = Set.of(
            "123456", "654321", "111111", "000000", "121212", "123123");
    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final String WRONG_CREDENTIAL = "Nomor rekening atau PIN salah";
    private static final int MAX_RANGE_DAYS = 90;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private String validateCurrency(String currency) {
        String value = (currency == null || currency.isBlank()) ? "IDR" : currency.trim().toUpperCase();
        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency harus 3 huruf, contoh: IDR");
        }
        return value;
    }

    private void validatePin(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new IllegalArgumentException("pin harus 6 digit angka");
        }
        if (WEAK_PINS.contains(pin) || pin.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("pin terlalu mudah ditebak");
        }
    }
    private String validateAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            throw new IllegalArgumentException("accountNo wajib diisi");
        }
        String value = accountNo.trim();
        if (!value.matches("\\d{10,16}")) {
            throw new IllegalArgumentException("accountNo harus 10 sampai 16 digit angka");
        }
        return value;
    }
    private BigDecimal validateBalance(BigDecimal balance) {
        BigDecimal value = (balance == null) ? BigDecimal.ZERO : balance;
        if (value.signum() < 0) {
            throw new IllegalArgumentException("initialBalance tidak boleh negatif");
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException("initialBalance maksimal 2 angka desimal");
        }
        return value;
    }

    private String validateOwnerName(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("ownerName wajib diisi");
        }
        String value = ownerName.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("ownerName maksimal 100 karakter");
        }
        return value;
    }

    private LedgerEntity buildLedger(String transferId, String accountNo, String direction,
                                     BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntity l = new LedgerEntity();
        l.setTransferId(transferId);
        l.setAccountNo(accountNo);
        l.setDirection(direction);
        l.setAmount(amount);
        l.setBalanceAfter(balanceAfter);
        return l;
    }

    private TransferResponseEntity toResponse(TransferResponseEntity t) {
        TransferResponseEntity r = new TransferResponseEntity();
        r.setTransferId(t.getTransferId());
        r.setFromAccountNo(t.getFromAccountNo());
        r.setToAccountNo(t.getToAccountNo());
        r.setAmount(t.getAmount());
        r.setCurrency(t.getCurrency());
        r.setStatus(t.getStatus());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }

    @Override
    public BankingEntity getAccount(String accountNo) throws Exception {
        String no = validateAccountNo(accountNo);

        Connection conn = null;
        try {
            conn = getConnection();
            BankingEntity account = bankingDao.findAccountByNo(conn, no);
            if (account == null) {
                throw new NoSuchElementException("Rekening tidak ditemukan");
            }
            return account;
        } finally {
            close(conn, null, null);
        }
    }

    @Override
    public BankingEntity createAccount(BankingEntity request) throws Exception {
        String accountNo = validateAccountNo(request.getAccountNo());
        String ownerName = validateOwnerName(request.getOwnerName());
        BigDecimal balance = validateBalance(request.getInitialBalance());
        String currency = validateCurrency(request.getCurrency());
        validatePin(request.getPin());

        BankingEntity entity = new BankingEntity();
        entity.setAccountNo(accountNo);
        entity.setOwnerName(ownerName);
        entity.setCurrency(currency);
        entity.setInitialBalance(balance);
        entity.setStatus("ACTIVE");
        entity.setPinHash(passwordEncoder.encode(request.getPin()));
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            bankingDao.createAccount(conn, entity);

            conn.commit();
        } catch (SQLIntegrityConstraintViolationException e) {
            if (conn != null) conn.rollback();
            throw new IllegalStateException("Nomor rekening sudah terdaftar: " + accountNo);
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
            close(conn, null, null);
        }

        entity.setPin(null);
        entity.setPinHash(null);
        return entity;
    }


    @Override
    public TransferResponseEntity transfers(TransferRequestEntity req) throws Exception {
        // 1. Validasi format (sebelum membuka koneksi)
        String fromNo = validateAccountNo(req.getFromAccountNo());
        String toNo = validateAccountNo(req.getToAccountNo());
        if (fromNo.equals(toNo)) {
            throw new IllegalArgumentException("Rekening asal dan tujuan tidak boleh sama");
        }
        BigDecimal amount = req.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount harus lebih dari 0");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount maksimal 2 angka desimal");
        }
        if (req.getPin() == null || !req.getPin().matches("\\d{6}")) {
            throw new IllegalArgumentException("pin harus 6 digit angka");
        }
        String key = req.getIdempotencyKey();
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new IllegalArgumentException("Idempotency-Key wajib diisi (maksimal 64 karakter)");
        }
        String note = (req.getNote() == null) ? null : req.getNote().trim();
        if (note != null && note.length() > 200) {
            throw new IllegalArgumentException("note maksimal 200 karakter");
        }

        // 2. Koneksi dan transaksi dipegang service
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // a. Kunci kedua rekening (urut id)
            List<BankingEntity> locked = bankingDao.lockAccounts(conn, fromNo, toNo);
            Map<String, BankingEntity> byNo = locked.stream()
                    .collect(Collectors.toMap(BankingEntity::getAccountNo, Function.identity()));

            BankingEntity from = byNo.get(fromNo);
            if (from == null) {
                throw new IllegalArgumentException(WRONG_CREDENTIAL);
            }
            if ("LOCKED".equals(from.getStatus())) {
                throw new IllegalStateException("Rekening terkunci karena salah PIN berulang kali");
            }
            if (!"ACTIVE".equals(from.getStatus())) {
                throw new IllegalStateException("Rekening asal tidak aktif");
            }
            if (from.getPinHash() == null) {
                throw new IllegalStateException("Rekening asal belum memiliki PIN");
            }

            // b. Cek PIN. Salah PIN: simpan counter lalu batalkan transfer
            if (!passwordEncoder.matches(req.getPin(), from.getPinHash())) {
                int attempts = from.getFailedPinAttempts() + 1;
                String newStatus = attempts >= MAX_PIN_ATTEMPTS ? "LOCKED" : from.getStatus();
                bankingDao.updateFailedPin(conn, fromNo, attempts, newStatus);
                conn.commit();   // counter harus tersimpan walau transfer gagal
                throw new IllegalArgumentException(WRONG_CREDENTIAL);
            }
            if (from.getFailedPinAttempts() > 0) {
                bankingDao.updateFailedPin(conn, fromNo, 0, from.getStatus());
            }

            // c. Idempotency: request dengan key yang sama dikembalikan hasil lamanya
            TransferResponseEntity existing = bankingDao.findTransferByKey(conn, key);
            if (existing != null) {
                boolean sameRequest = existing.getFromAccountNo().equals(fromNo)
                        && existing.getToAccountNo().equals(toNo)
                        && existing.getAmount().compareTo(amount) == 0;
                if (!sameRequest) {
                    throw new IllegalStateException("Idempotency-Key sudah dipakai untuk transfer lain");
                }
                conn.commit();
                return toResponse(existing);
            }

            // d. Validasi rekening tujuan dan saldo
            BankingEntity to = byNo.get(toNo);
            if (to == null) {
                throw new IllegalArgumentException("Rekening tujuan tidak ditemukan");
            }
            if (!"ACTIVE".equals(to.getStatus())) {
                throw new IllegalStateException("Rekening tujuan tidak aktif");
            }
            if (!from.getCurrency().equals(to.getCurrency())) {
                throw new IllegalArgumentException("Mata uang rekening asal dan tujuan berbeda");
            }
            if (from.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Saldo tidak cukup");
            }

            // e. Proses transfer
            BigDecimal fromAfter = from.getBalance().subtract(amount);
            BigDecimal toAfter = to.getBalance().add(amount);

            bankingDao.updateBalance(conn, fromNo, amount.negate());
            bankingDao.updateBalance(conn, toNo, amount);

            TransferResponseEntity transfer = new TransferResponseEntity();
            transfer.setTransferId(UUID.randomUUID().toString());
            transfer.setIdempotencyKey(key);
            transfer.setFromAccountNo(fromNo);
            transfer.setToAccountNo(toNo);
            transfer.setAmount(amount);
            transfer.setCurrency(from.getCurrency());
            transfer.setNote(note);
            transfer.setStatus("COMPLETED");
            transfer.setCreatedAt(LocalDateTime.now());
            bankingDao.insertTransfer(conn, transfer);

            bankingDao.insertLedger(conn, buildLedger(transfer.getTransferId(), fromNo, "D", amount, fromAfter));
            bankingDao.insertLedger(conn, buildLedger(transfer.getTransferId(), toNo, "C", amount, toAfter));

            conn.commit();
            return toResponse(transfer);

        } catch (SQLIntegrityConstraintViolationException e) {
            // jaring pengaman: Idempotency-Key yang sama masuk bersamaan
            if (conn != null) conn.rollback();
            throw new IllegalStateException("Transfer dengan Idempotency-Key ini sedang diproses");
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
            close(conn, null, null);
        }
    }

    @Override
    public Map<String, Object> reportTransfers(ReportRequestEntity req) throws Exception {
       String accountNo = validateAccountNo(req.getFromAccountNo());

        String type = (req.getType() == null || req.getType().isBlank())
                ? "ALL" : req.getType().trim().toUpperCase();
        if (!type.equals("D") && !type.equals("C") && !type.equals("ALL")) {
            throw new IllegalArgumentException("type harus D, C, atau ALL");
        }

        if (req.getFrom() == null || req.getTo() == null) {
            throw new IllegalArgumentException("from dan to wajib diisi");
        }
        if (req.getTo().isBefore(req.getFrom())) {
            throw new IllegalArgumentException("Tanggal to tidak boleh lebih kecil dari tanggal from");
        }
        if (ChronoUnit.DAYS.between(req.getFrom(), req.getTo()) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Rentang tanggal maksimal " + MAX_RANGE_DAYS + " hari");
        }
        if (req.getPage() < 0) {
            throw new IllegalArgumentException("page tidak boleh negatif");
        }
        int size = req.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(req.getSize(), MAX_PAGE_SIZE);
        int offset = req.getPage() * size;

        LocalDateTime start = req.getFrom().atStartOfDay();
        LocalDateTime end = req.getTo().plusDays(1).atStartOfDay();

        List<ReportResponseEntity> rows;
        Connection conn = null;
        try {
            conn = getConnection();
            rows = bankingDao.reportTransfers(conn, accountNo, type, start, end, size, offset);
        } finally {
            close(conn, null, null);
        }

        Map<String, BigDecimal> totalByType = rows.stream()
                .collect(Collectors.groupingBy(
                        ReportResponseEntity::getType,
                        Collectors.reducing(BigDecimal.ZERO, ReportResponseEntity::getAmount, BigDecimal::add)));

        Map<String, Long> countByType = rows.stream()
                .collect(Collectors.groupingBy(ReportResponseEntity::getType, Collectors.counting()));

        Optional<ReportResponseEntity> largest = rows.stream()
                .max(Comparator.comparing(ReportResponseEntity::getAmount));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDebit", totalByType.getOrDefault("D", BigDecimal.ZERO));
        summary.put("totalCredit", totalByType.getOrDefault("C", BigDecimal.ZERO));
        summary.put("countDebit", countByType.getOrDefault("D", 0L));
        summary.put("countCredit", countByType.getOrDefault("C", 0L));
        summary.put("largestAmount", largest.map(ReportResponseEntity::getAmount).orElse(BigDecimal.ZERO));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountNo", accountNo);
        result.put("type", type);
        result.put("from", req.getFrom());
        result.put("to", req.getTo());
        result.put("page", req.getPage());
        result.put("size", size);
        result.put("count", rows.size());
        result.put("summary", summary);
        result.put("transactions", rows);
        return result;
    }

}
