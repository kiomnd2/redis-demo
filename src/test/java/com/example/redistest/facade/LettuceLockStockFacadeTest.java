package com.example.redistest.facade;

import com.example.redistest.domain.Stock;
import com.example.redistest.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LettuceLockStockFacadeTest {

    @Autowired
    private LettuceLockStockFacade stockService;


    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void stock_decrease() throws InterruptedException {
        stockService.decrease(1L, 1L); // 감소 1

        // 100 - 1 = 99
        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(99, stock.getQuantity());

        //
    }

    @Test
    public void 동시에_100개_요청() {
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 스레드가 전부 끝날때까지 대기
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i=0 ; i<threadCount ; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 0 개가 나와야 정상
        assertEquals(0, stock.getQuantity());
    }
}