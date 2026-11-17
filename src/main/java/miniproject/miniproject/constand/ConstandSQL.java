package miniproject.miniproject.constand;

public class ConstandSQL {
    private ConstandSQL() {}
    public static final String INSERT_ACCOUNT =
            "INSERT INTO accounts (account_no, owner_name, currency, balance, status, pin_hash) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    public static final String LOCK_ACCOUNTS =
            "SELECT id, account_no, owner_name, currency, balance, status, pin_hash, failed_pin_attempts "
                    + "FROM accounts WHERE account_no IN (?, ?) ORDER BY id FOR UPDATE";

    public static final String UPDATE_BALANCE =
            "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";

    public static final String UPDATE_FAILED_PIN =
            "UPDATE accounts SET failed_pin_attempts = ?, status = ? WHERE account_no = ?";

    public static final String FIND_TRANSFER_BY_KEY =
            "SELECT idempotency_key, from_account_no, to_account_no, amount, currency, note, status, created_at "
                    + "FROM transfers WHERE idempotency_key = ?";

    public static final String INSERT_TRANSFER =
            "INSERT INTO transfers (id, idempotency_key, from_account_no, to_account_no, amount, currency, note, status, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String INSERT_LEDGER =
            "INSERT INTO ledger_entries (transfer_id, account_no, direction, amount, balance_after) "
                    + "VALUES (?, ?, ?, ?, ?)";

    public static final String REPORT_TRANSFER =
            "SELECT t.id AS transferId, l.direction AS type, l.account_no AS accountNo, "
                    + "ao.owner_name AS accountName, cp.account_no AS counterpartAccountNo, "
                    + "cp.owner_name AS counterpartName, l.amount AS amount, l.balance_after AS balance, "
                    + "t.currency AS currency, t.note AS note, t.created_at AS createdAt "
                    + "FROM ledger_entries l "
                    + "JOIN transfers t ON t.id = l.transfer_id "
                    + "JOIN accounts ao ON ao.account_no = l.account_no "
                    + "JOIN accounts cp ON cp.account_no = "
                    + "  CASE WHEN l.direction = 'D' THEN t.to_account_no ELSE t.from_account_no END "
                    + "WHERE l.account_no = ? "
                    + "AND t.created_at >= ? AND t.created_at < ? "
                    + "AND (? = 'ALL' OR l.direction = ?) "
                    + "ORDER BY t.created_at DESC, l.id DESC "
                    + "LIMIT ? OFFSET ?";

    public static final String FIND_ACCOUNT_BY_NO =
            "SELECT account_no, owner_name, currency, balance, status "
                    + "FROM accounts WHERE account_no = ?";
}
