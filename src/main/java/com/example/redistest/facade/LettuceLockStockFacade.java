package com.example.redistest.facade;

import com.example.redistest.redis.repository.RedisLockRepository;
import com.example.redistest.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LettuceLockStockFacade {

    private final RedisLockRepository repository;

    private final StockService stockService;

    public void decrease(Long key, Long quantity) throws InterruptedException {
        while (!repository.lock(key)) {
            Thread.sleep(100);
        }

        try {
            stockService.decrease(key, quantity);
        } finally {
            repository.unlock(key);
        }
    }
}
