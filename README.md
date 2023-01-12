# redis-demo

## 목표
 * Redis 처음 사용해보기
 * Redis 환경설정 구경하기
 * Redis 를 이용한 동시성 제어


## 첫번째 예제
1. jpa를 활용하여 주식을 관리하는 Stock Entity,Repository 생성
2. Stock 수를 줄이고 늘이기 위한 stockService 생성
3. stockService의 decrease 했을 때 정상적으로 수량의 감소를 확인

`StockServiceTest.java`
```java
    @Test
    public void stock_decrease() {
        stockService.decrease(1L, 1L); // 감소 1

        // 100 - 1 = 99
        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(99, stock.getQuantity());

        //
    }
```

### 문제 
```java
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
                } finally {
                    latch.countDown();
                }
            });
        }

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 0 개가 나와야 정상
        assertEquals(0, stock.getQuantity()); // false
    }
```
* 32개의 쓰레드를 동시에 해당 stock의 decrease에 접근시켜서 수량을 줄였을 때 원하는 결과가 나와야 하지만 예상했던 결과가 나오지 않음
실제로 해당 테스트 코드는 실패로 끝남, 
* 이는 동시성 이슈로 인해 발생한 것이며, 여러 쓰레드가 같은 데이터 공간에서 데이터를 바꾸는 과정에서 정합성에 문제가 발생함



