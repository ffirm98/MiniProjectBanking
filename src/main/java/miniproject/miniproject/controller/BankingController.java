package miniproject.miniproject.controller;

import miniproject.miniproject.entity.BankingEntity;
import miniproject.miniproject.entity.ReportRequestEntity;
import miniproject.miniproject.entity.TransferRequestEntity;
import miniproject.miniproject.entity.TransferResponseEntity;
import miniproject.miniproject.service.BankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class BankingController {
    @Autowired
    private BankingService bankingService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String testConnection() {
        return "testing";
    }

    @RequestMapping(value = "/accounts", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> getAccount(@RequestBody BankingEntity request) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            BankingEntity account = bankingService.getAccount(request.getAccountNo());
            map.put("status", true);
            map.put("data", account);
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
        } catch (NoSuchElementException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("status", false);
            map.put("message", "Terjadi kesalahan pada server");
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/createAccount", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody BankingEntity request) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            BankingEntity created = bankingService.createAccount(request);
            map.put("status", true);
            map.put("data", created);
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CONFLICT);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("status", false);
            map.put("message", "Terjadi kesalahan pada server");
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @RequestMapping(value = "/transfers", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> transfers(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequestEntity request) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            request.setIdempotencyKey(idempotencyKey);
            TransferResponseEntity created = bankingService.transfers(request);
            map.put("status", true);
            map.put("data", created);
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CONFLICT);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("status", false);
            map.put("message", "Terjadi kesalahan pada server");
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/report", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> report(
            @RequestBody ReportRequestEntity request) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Map<String, Object> result = bankingService.reportTransfers(request);
            map.put("status", true);
            map.put("data", result);
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            map.put("status", false);
            map.put("message", e.getMessage());
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CONFLICT);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("status", false);
            map.put("message", "Terjadi kesalahan pada server");
            return new ResponseEntity<Map<String, Object>>(map, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}