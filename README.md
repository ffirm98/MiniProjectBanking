# MiniProject

Mini project backend **simulasi transfer dana antar rekening** dengan ledger debit/kredit, dibuat dengan Spring Boot dan JDBC murni (native SQL). Proyek ini hanya simulasi untuk keperluan belajar dan penilaian, bukan core banking sungguhan, dan tidak memakai data atau kode dari perusahaan mana pun.

## Fitur

| Fitur | Endpoint |
|---|---|
| Buka rekening (dengan PIN 6 digit) | `POST /createAccount` |
| Transfer antar rekening | `POST /transfers` |
| Laporan mutasi (Debit / Kredit / Semua) | `POST /report` |
| Get Info Account | `POST /accounts` |


## Teknologi

- Java 17, Spring Boot 4.1.2, Maven
- MySQL / MariaDB (dikelola lewat phpMyAdmin/XAMPP)
- HikariCP (connection pool), JDBC murni tanpa JPA/Hibernate
- BCrypt (`spring-security-crypto`) untuk hash PIN

## Cara menjalankan

### 1. Prasyarat

- JDK 17 atau lebih baru (`java -version`)
- Maven 3.9+ (`mvn -v`)
- MySQL 8.0+ atau MariaDB 10.2+ (window function dan CTE membutuhkan versi ini)

### 2. Siapkan database

Buat database `miniproject`, lalu jalankan skrip pada bagian **Skema database** di bawah (lewat tab **SQL** di phpMyAdmin).

### 3. Atur koneksi

Edit `src/main/resources/application.properties`:

```properties
server.port=9090
app.datasource.url=jdbc:mysql://localhost:3306/miniproject?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true
app.datasource.username=root
app.datasource.password=
app.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 4. Jalankan aplikasi

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

Aplikasi siap bila di log muncul `Tomcat started on port 9090`.

## Skema database

```sql
CREATE TABLE accounts (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_no           VARCHAR(16) NOT NULL UNIQUE,
  owner_name           VARCHAR(100) NOT NULL,
  currency             CHAR(3) NOT NULL DEFAULT 'IDR',
  balance              DECIMAL(19,2) NOT NULL DEFAULT 0,
  status               VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / LOCKED
  pin_hash             VARCHAR(60) NULL,
  failed_pin_attempts  INT NOT NULL DEFAULT 0,
  created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE transfers (
  id               CHAR(36) PRIMARY KEY,
  idempotency_key  VARCHAR(64) NOT NULL UNIQUE,
  from_account_no  VARCHAR(16) NOT NULL,
  to_account_no    VARCHAR(16) NOT NULL,
  amount           DECIMAL(19,2) NOT NULL,
  currency         CHAR(3) NOT NULL,
  note             VARCHAR(200),
  status           VARCHAR(12) NOT NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Append-only: tidak pernah di-UPDATE atau DELETE
CREATE TABLE ledger_entries (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  transfer_id    CHAR(36) NOT NULL,
  account_no     VARCHAR(16) NOT NULL,
  direction      CHAR(1) NOT NULL,            -- D = debit (keluar), C = kredit (masuk)
  amount         DECIMAL(19,2) NOT NULL,
  balance_after  DECIMAL(19,2) NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_transfers_created ON transfers (created_at);
CREATE INDEX idx_ledger_acc_dir ON ledger_entries (account_no, direction, created_at);
```

## Struktur package

```
miniproject.miniproject
├── common/        DbConnection (penyedia koneksi dari DataSource)
├── constand/      ConstandSQL (tempat seluruh query SQL)
├── controller/    BankingController
├── dao/           BankingDao (interface)
├── dao/impl/      BankingDaoImpl (menjalankan SQL dengan Connection dari service)
├── entity/        BankingEntity, TransferRequest, TransferResponse,
│                  TransferEntity, LedgerEntity, ReportRequestEntity, ReportResponseEntity
├── service/       BankingService (interface)
├── service/impl/  BankingServiceImpl (validasi, aturan bisnis, transaksi)
└── BeanConfig     DataSource (HikariCP), JdbcTemplate, BCryptPasswordEncoder
```

Pembagian tugas:

- **Controller**: menerima request dan memetakan exception ke kode HTTP.
- **ServiceImpl**: validasi, aturan bisnis, serta membuka koneksi dan memegang transaksi (`commit`/`rollback`).
- **DaoImpl**: hanya menjalankan SQL memakai `Connection` yang diberikan service, sehingga beberapa method DAO berjalan dalam satu transaksi.

## Dokumentasi API

Semua respons berbentuk:

```json
{ "status": true, "data": { } }
{ "status": false, "message": "penjelasan error" }
```

### POST /createAccount

```json
{
  "accountNo": "1122334455",
  "ownerName": "Budi Santoso",
  "currency": "IDR",
  "initialBalance": 5000000,
  "pin": "483920"
}
```

| Hasil | Kode |
|---|---|
| Rekening dibuat | `201` |
| Input tidak valid (nomor bukan 10 sampai 16 digit, PIN bukan 6 digit atau terlalu mudah ditebak, saldo negatif) | `400` |
| Nomor rekening sudah terdaftar | `409` |

Respons tidak pernah memuat `pin` maupun `pinHash`.

### POST /transfers

Header wajib: `Idempotency-Key: <string unik, maks 64 karakter>`

```json
{
  "fromAccountNo": "1122334455",
  "toAccountNo": "5544332211",
  "amount": 100000,
  "pin": "483920",
  "note": "Bayar makan"
}
```

| Hasil | Kode |
|---|---|
| Transfer berhasil | `201` |
| Request dengan key yang sama diulang | `201` dengan `transferId` yang sama, saldo tidak berubah lagi |
| Key sama tapi isi transfer berbeda | `409` |
| Saldo tidak cukup, rekening tidak aktif, rekening terkunci | `409` |
| PIN salah atau rekening asal tidak ditemukan | `400` ("Nomor rekening atau PIN salah") |
| Rekening sama, nominal tidak valid, mata uang berbeda | `400` |

### POST /report

```json
{
  "fromAccountNo": "1122334455",
  "type": "ALL",
  "from": "2026-11-01",
  "to": "2026-11-30",
  "page": 0,
  "size": 20
}
```

- `type`: `D` (uang keluar), `C` (uang masuk), atau `ALL`. Sudut pandang selalu pemilik rekening di `fromAccountNo`.
- `to` tidak boleh lebih kecil dari `from`, rentang maksimal 90 hari, `size` maksimal 100.
- Respons berisi `summary` (total dan jumlah debit/kredit, nominal terbesar) dan `transactions`.

## Keputusan desain

| Masalah | Solusi |
|---|---|
| Dua transfer bersamaan dari satu rekening | `SELECT ... ORDER BY id FOR UPDATE`: kedua rekening di-lock dalam urutan `id` yang tetap, sehingga saldo konsisten dan transfer A ke B bersamaan dengan B ke A tidak deadlock |
| Request dikirim ulang (klik ganda, timeout) | Header `Idempotency-Key` dengan kolom `UNIQUE` di tabel `transfers`; request ulang mengembalikan hasil lama |
| Uang hilang atau dobel | Satu `Connection` untuk seluruh langkah transfer (update saldo, insert transfer, dua baris ledger), lalu `commit` atau `rollback` bersama |
| Audit saldo | `ledger_entries` hanya di-`INSERT`, tiap baris mencatat `balance_after` |
| Presisi uang | `BigDecimal` di Java dan `DECIMAL(19,2)` di database, bukan `double` |
| Keamanan PIN | Disimpan sebagai hash BCrypt, tidak pernah dikembalikan di respons atau ditulis ke log |
| Tebak PIN | Setelah 3 kali salah berturut-turut, rekening menjadi `LOCKED`. Counter di-`commit` walau transfer dibatalkan, supaya batas tercapai |
| Enumerasi rekening | Pesan error generik "Nomor rekening atau PIN salah" |
| SQL injection | Seluruh query memakai `PreparedStatement` berparameter, disimpan di `ConstandSQL` |
| Race condition nomor rekening ganda | Constraint `UNIQUE` di database, bukan "SELECT dulu baru INSERT" |

Service Spring bersifat singleton, jadi `Connection`, `PreparedStatement`, dan `ResultSet` selalu variabel lokal di method, tidak pernah disimpan sebagai field.


## Pengujian

Koleksi Postman tersedia di `postman/miniProject.postman_collection.json`. Urutan yang disarankan:

1. Buat dua rekening.
2. Transfer dari rekening pertama ke kedua.
3. Ulangi transfer dengan `Idempotency-Key` yang sama (`transferId` harus sama, saldo tidak berubah lagi).
4. Coba PIN salah tiga kali (rekening menjadi `LOCKED`).
5. Panggil `/report` dengan `type` `ALL`, `D`, dan `C`.

Cek hasilnya di tabel: satu baris `transfers` per `Idempotency-Key`, dan dua baris `ledger_entries` (`D` dan `C`) per transfer.
