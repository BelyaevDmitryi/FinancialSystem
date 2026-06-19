package com.fs.backtest;

import com.fs.dto.BrokerCandleDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory cash and position for backtest simulation.
 */
public class SimulatedJournal {

    private static final int SCALE = 8;

    private final BigDecimal initialCash;
    private final int slippageBps;

    private BigDecimal cash;
    private BigDecimal positionQty = BigDecimal.ZERO;
    private BigDecimal avgPrice = BigDecimal.ZERO;
    private final List<SimulatedTrade> trades = new ArrayList<>();

    public SimulatedJournal(BigDecimal initialCash, int slippageBps) {
        this.initialCash = initialCash;
        this.cash = initialCash;
        this.slippageBps = slippageBps;
    }

    public BigDecimal getPositionQty() {
        return positionQty;
    }

    public List<SimulatedTrade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public BigDecimal maxBuyQuantity(BigDecimal marketPrice) {
        if (marketPrice.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal effectivePrice = applySlippage(marketPrice, true);
        return cash.divide(effectivePrice, SCALE, RoundingMode.DOWN);
    }

    public void buy(BrokerCandleDto bar, BigDecimal quantity) {
        if (quantity.signum() <= 0) {
            return;
        }
        BigDecimal price = applySlippage(bar.getClose(), true);
        BigDecimal cost = price.multiply(quantity).setScale(SCALE, RoundingMode.HALF_UP);
        if (cost.compareTo(cash) > 0) {
            return;
        }
        cash = cash.subtract(cost);
        if (positionQty.signum() == 0) {
            positionQty = quantity;
            avgPrice = price;
        } else {
            BigDecimal totalCost = avgPrice.multiply(positionQty).add(cost);
            positionQty = positionQty.add(quantity);
            avgPrice = totalCost.divide(positionQty, SCALE, RoundingMode.HALF_UP);
        }
        trades.add(new SimulatedTrade(bar.getTime(), "BUY", quantity, price, null));
    }

    public void sell(BrokerCandleDto bar, BigDecimal quantity) {
        if (quantity.signum() <= 0 || positionQty.signum() == 0) {
            return;
        }
        BigDecimal sellQty = quantity.min(positionQty);
        BigDecimal price = applySlippage(bar.getClose(), false);
        BigDecimal proceeds = price.multiply(sellQty).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal realizedPnl = price.subtract(avgPrice).multiply(sellQty).setScale(SCALE, RoundingMode.HALF_UP);
        cash = cash.add(proceeds);
        positionQty = positionQty.subtract(sellQty);
        if (positionQty.signum() == 0) {
            avgPrice = BigDecimal.ZERO;
        }
        trades.add(new SimulatedTrade(bar.getTime(), "SELL", sellQty, price, realizedPnl));
    }

    public BigDecimal equity(BigDecimal markPrice) {
        return cash.add(positionQty.multiply(markPrice)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getCash() {
        return cash;
    }

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    private BigDecimal applySlippage(BigDecimal price, boolean isBuy) {
        BigDecimal factor = BigDecimal.valueOf(slippageBps)
                .divide(BigDecimal.valueOf(10_000), SCALE, RoundingMode.HALF_UP);
        if (isBuy) {
            return price.multiply(BigDecimal.ONE.add(factor)).setScale(SCALE, RoundingMode.HALF_UP);
        }
        return price.multiply(BigDecimal.ONE.subtract(factor)).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
