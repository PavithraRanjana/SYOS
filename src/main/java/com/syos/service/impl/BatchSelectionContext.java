package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.service.interfaces.BatchSelectionStrategy;
import java.util.List;
import java.util.Optional;

/**
 * Context class for Strategy Pattern.
 * Allows runtime switching between different batch selection strategies.
 */
public class BatchSelectionContext {

    private BatchSelectionStrategy strategy;

    public BatchSelectionContext(BatchSelectionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Changes the batch selection strategy at runtime.
     *
     * @param strategy New strategy to use
     */
    public void setStrategy(BatchSelectionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Selects a batch using the current strategy.
     *
     * @param availableBatches Available batches
     * @param productCode Product code
     * @param requiredQuantity Required quantity
     * @return Selected batch with selection reason
     */
    public BatchSelectionResult selectBatch(List<MainInventory> availableBatches,
                                            ProductCode productCode,
                                            int requiredQuantity) {

        Optional<MainInventory> selectedBatch = strategy.selectBatch(availableBatches, productCode, requiredQuantity);

        String reason = selectedBatch.isPresent()
                ? strategy.getSelectionReason(selectedBatch.get(), availableBatches)
                : "No suitable batch found";

        return new BatchSelectionResult(selectedBatch, reason, strategy.getStrategyName());
    }

    /**
     * Gets the current strategy name.
     *
     * @return Current strategy name
     */
    public String getCurrentStrategyName() {
        return strategy.getStrategyName();
    }

    /**
     * Result of batch selection including reasoning.
     */
    public static class BatchSelectionResult {
        private final Optional<MainInventory> selectedBatch;
        private final String selectionReason;
        private final String strategyUsed;

        public BatchSelectionResult(Optional<MainInventory> selectedBatch,
                                    String selectionReason,
                                    String strategyUsed) {
            this.selectedBatch = selectedBatch;
            this.selectionReason = selectionReason;
            this.strategyUsed = strategyUsed;
        }

        public Optional<MainInventory> getSelectedBatch() { return selectedBatch; }
        public String getSelectionReason() { return selectionReason; }
        public String getStrategyUsed() { return strategyUsed; }
        public boolean hasSelection() { return selectedBatch.isPresent(); }
    }
}