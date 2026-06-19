package com.fs.service;

import com.fs.domain.Position;
import com.fs.domain.Trade;
import com.fs.domain.TradeSide;
import com.fs.dto.FillDto;
import com.fs.dto.TradeDto;
import com.fs.repository.PositionRepository;
import com.fs.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Domain service for trade journal and position updates.
 */
@Service
public class JournalService {

    private static final int SCALE = 8;

    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;

    public JournalService(TradeRepository tradeRepository, PositionRepository positionRepository) {
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Records a fill from TradingTerminal. Idempotent by {@code orderId} (ADR-002 §4).
     */
    @Transactional
    public TradeDto recordFill(FillDto fill) {
        return tradeRepository.findByOrderId(fill.orderId())
                .map(this::toTradeDto)
                .orElseGet(() -> applyFill(fill));
    }

    @Transactional(readOnly = true)
    public List<TradeDto> getTradesForUser(Long userId) {
        return tradeRepository.findByUserIdOrderByExecutedAtDesc(userId).stream()
                .map(this::toTradeDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Position> getPositionsForUser(Long userId) {
        return positionRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Position getPositionForUser(Long userId, String figi) {
        return positionRepository.findByUserIdAndFigi(userId, figi)
                .orElseThrow(() -> new PositionNotFoundException(userId, figi));
    }

    private TradeDto applyFill(FillDto fill) {
        BigDecimal quantity = fill.quantity();
        BigDecimal price = fill.price();
        BigDecimal realizedPnl = null;

        Position position = positionRepository.findByUserIdAndFigi(fill.userId(), fill.figi())
                .orElseGet(() -> newPosition(fill.userId(), fill.figi()));

        if (fill.side() == TradeSide.BUY) {
            position = applyBuy(position, quantity, price);
        } else {
            realizedPnl = applySell(position, quantity, price);
        }

        positionRepository.save(position);

        Trade trade = new Trade();
        trade.setUserId(fill.userId());
        trade.setFigi(fill.figi());
        trade.setSide(fill.side());
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setRealizedPnl(realizedPnl);
        trade.setOrderId(fill.orderId());
        trade.setCommission(fill.commission());
        trade.setExecutedAt(fill.executedAt());

        return toTradeDto(tradeRepository.save(trade));
    }

    private Position applyBuy(Position position, BigDecimal quantity, BigDecimal price) {
        BigDecimal currentQty = position.getQuantity();
        BigDecimal newQty = currentQty.add(quantity);
        if (currentQty.signum() == 0) {
            position.setAvgPrice(price);
        } else {
            BigDecimal totalCost = currentQty.multiply(position.getAvgPrice()).add(quantity.multiply(price));
            position.setAvgPrice(totalCost.divide(newQty, SCALE, RoundingMode.HALF_UP));
        }
        position.setQuantity(newQty);
        return position;
    }

    private BigDecimal applySell(Position position, BigDecimal quantity, BigDecimal price) {
        BigDecimal avgPrice = position.getAvgPrice();
        BigDecimal realizedPnl = price.subtract(avgPrice).multiply(quantity).setScale(SCALE, RoundingMode.HALF_UP);
        position.setQuantity(position.getQuantity().subtract(quantity));
        return realizedPnl;
    }

    private static Position newPosition(Long userId, String figi) {
        Position position = new Position();
        position.setUserId(userId);
        position.setFigi(figi);
        position.setQuantity(BigDecimal.ZERO);
        position.setAvgPrice(BigDecimal.ZERO);
        return position;
    }

    private TradeDto toTradeDto(Trade trade) {
        return new TradeDto(
                trade.getId(),
                trade.getUserId(),
                trade.getFigi(),
                trade.getSide(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getRealizedPnl(),
                trade.getOrderId(),
                trade.getCommission(),
                trade.getExecutedAt()
        );
    }
}
