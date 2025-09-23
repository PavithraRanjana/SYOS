package com.syos.service.interfaces;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import java.util.List;
import java.util.Optional;

/**
 * Strategy Pattern for batch selection algorithms.
 * Allows different strategies for selecting which batch to use for issuing stock.
 */
public interface BatchSelectionStrategy {

    /**
     * Selects the best batch for issuing stock based on the strategy implementation.
     *
     * @param availableBatches List of available batches for the product
     * @param productCode Product code for context
     * @param requiredQuantity Quantity needed
     * @return Selected batch or empty if no suitable batch found
     */
    Optional<MainInventory> selectBatch(List<MainInventory> availableBatches,
                                        ProductCode productCode,
                                        int requiredQuantity);

    /**
     * Provides explanation of why this batch was selected.
     * Used for displaying information to inventory manager.
     *
     * @param selectedBatch The batch that was selected
     * @param availableBatches All available batches
     * @return Human-readable explanation
     */
    String getSelectionReason(MainInventory selectedBatch, List<MainInventory> availableBatches);

    /**
     * Gets the strategy name for display purposes.
     *
     * @return Strategy name
     */
    String getStrategyName();
}