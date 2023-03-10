package com.example.redistest.service;

import com.example.redistest.domain.Stock;
import com.example.redistest.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease(Long id, Long quantity) {
        // get stock
        Stock stock = stockRepository.findById(id).orElseThrow();
        // decrease
        stock.decrease(quantity);
        // 갱신된 값 저장
        stockRepository.save(stock);
    }
}
