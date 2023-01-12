package com.example.redistest.facade;

import com.example.redistest.service.OptimisticLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OptimisticLockStockFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try{
            optimisticLockStockService.decrease(id, quantity);
            break;
            } catch (Exception e) {
                Thread.sleep(100);
            }
        }
    }
}
