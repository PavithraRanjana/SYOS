package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.service.interfaces.BatchSelectionStrategy;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * FIFO (First In, First Out) strategy with expiry date priority.
 *
 * Selection Logic:
 * 1. Prioritize batches that have sufficient quantity
 * 2. Among sufficient batches, select the one with earliest expiry date
 * 3. If expiry dates are same, select oldest batch (earliest purchase date)
 * 4. If no single batch has sufficient quantity, select earliest expiring batch
 */
public class FIFOWithExpiryStrategy implements BatchSelectionStrategy {

    private static final int CRITICAL_EXPIRY_DAYS = 30; // Days to consider critical

    @Override
    public Optional<MainInventory> selectBatch(List<MainInventory> availableBatches,
                                               ProductCode productCode,
                                               int requiredQuantity) {

        if (availableBatches.isEmpty()) {
            return Optional.empty();
        }

        // First, try to find batches with sufficient quantity
        List<MainInventory> sufficientBatches = availableBatches.stream()
                .filter(batch -> batch.getRemainingQuantity() >= requiredQuantity)
                .toList();

        if (!sufficientBatches.isEmpty()) {
            // Select from sufficient batches using expiry-first, then FIFO
            return sufficientBatches.stream()
                    .min(Comparator
                            .comparing(MainInventory::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(MainInventory::getPurchaseDate))
                    .or(() -> Optional.empty());
        }

        // No single batch has sufficient quantity, select earliest expiring
        return availableBatches.stream()
                .filter(batch -> batch.getRemainingQuantity() > 0)
                .min(Comparator
                        .comparing(MainInventory::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MainInventory::getPurchaseDate))
                .or(() -> Optional.empty());
    }

    @Override
    public String getSelectionReason(MainInventory selectedBatch, List<MainInventory> availableBatches) {
        if (selectedBatch == null) {
            return "No batch selected";
        }

        StringBuilder reason = new StringBuilder();
        reason.append("Selected Batch #").append(selectedBatch.getBatchNumber()).append(" because:\n");

        // Check if expiry is critical
        LocalDate today = LocalDate.now();
        if (selectedBatch.getExpiryDate() != null) {
            long daysToExpiry = ChronoUnit.DAYS.between(today, selectedBatch.getExpiryDate());

            if (daysToExpiry <= CRITICAL_EXPIRY_DAYS) {
                reason.append("⚠️  CRITICAL: Expires in ").append(daysToExpiry).append(" days (")
                        .append(selectedBatch.getExpiryDate()).append(")\n");
            } else {
                reason.append("📅 Earliest expiry date: ").append(selectedBatch.getExpiryDate())
                        .append(" (").append(daysToExpiry).append(" days)\n");
            }
        }

        // Check if it's the oldest
        boolean isOldest = availableBatches.stream()
                .noneMatch(batch -> batch.getPurchaseDate().isBefore(selectedBatch.getPurchaseDate()));

        if (isOldest) {
            reason.append("⏰ Oldest batch: Purchased on ").append(selectedBatch.getPurchaseDate()).append("\n");
        }

        // Check if newer batch has earlier expiry
        Optional<MainInventory> newerBatchWithEarlierExpiry = availableBatches.stream()
                .filter(batch -> batch.getPurchaseDate().isAfter(selectedBatch.getPurchaseDate()))
                .filter(batch -> batch.getExpiryDate() != null && selectedBatch.getExpiryDate() != null)
                .filter(batch -> batch.getExpiryDate().isBefore(selectedBatch.getExpiryDate()))
                .findFirst();

        if (newerBatchWithEarlierExpiry.isPresent()) {
            reason.append("ℹ️  Note: Newer batch #").append(newerBatchWithEarlierExpiry.get().getBatchNumber())
                    .append(" has earlier expiry but was not selected due to other factors\n");
        }

        reason.append("✅ Available quantity: ").append(selectedBatch.getRemainingQuantity()).append(" units");

        return reason.toString();
    }

    @Override
    public String getStrategyName() {
        return "FIFO with Expiry Priority";
    }
}