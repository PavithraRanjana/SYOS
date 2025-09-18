package com.syos.repository.interfaces;

import com.syos.domain.models.Bill;
import com.syos.domain.valueobjects.BillSerialNumber;
import java.time.LocalDate;
import java.util.List;

public interface BillRepository extends Repository<Bill, Integer> {
    BillSerialNumber generateNextSerialNumber(LocalDate billDate);
    List<Bill> findByDate(LocalDate date);
    List<Bill> findByDateRange(LocalDate startDate, LocalDate endDate);
    Bill saveBillWithItems(Bill bill);
}