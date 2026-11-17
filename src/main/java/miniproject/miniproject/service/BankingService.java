package miniproject.miniproject.service;

import miniproject.miniproject.entity.BankingEntity;
import miniproject.miniproject.entity.ReportRequestEntity;
import miniproject.miniproject.entity.TransferRequestEntity;
import miniproject.miniproject.entity.TransferResponseEntity;

import java.util.List;
import java.util.Map;

public interface BankingService {
    BankingEntity getAccount(String accountNo) throws Exception;
    BankingEntity createAccount(BankingEntity req) throws Exception;
    TransferResponseEntity transfers(TransferRequestEntity req) throws Exception;
    Map<String, Object> reportTransfers(ReportRequestEntity req) throws Exception;
}
