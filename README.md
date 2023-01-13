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

### 해결
동시성이 발생하는 구간에 하나의 쓰레드만 동시에 접근하게 끔 처리


## Synchronized 이용
1. 동시성 이슈가 발생하는 구간에 synchronized 적용

```java
    public synchronized void decrease(Long quantity) {
        if (this.quantity - quantity < 0) {
            throw new RuntimeException("재고 없음");
        }
        this.quantity -=  quantity;
    }
```

### 문제점
* 이는 해당 인스턴스에 효과가 적용됨, 즉 서버가 2대라면 서버 2대에 대해 데이터 접근에 대한 동시성 문제가 해결되지 않음

### 해결 
* Database 이용
  * Pessimistic Lock : 실제로 데이터에 Lock 을 결어 정합성을 맞추는 방법, exclusive lock 을 걸게 되면 다른 트렌젝션에서는 lock 이 해제되기 전에 데이터를 가져올 수 없음
  * Optimistic Lock : 실제 Lock을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법, 데이터를 읽은 후에 update를 수행할 때현재 읽은 버전이 맞는지 확인하여 업데이트, 내가 읽은 버전에 수정사항이 발생하면 오류가 발생
  * Named Lock : 이름을 가진 metadata locking 이름을 가진 lock 을 획득한 후 해제할 때까지 다른 세션은 이 lock 을 획득할 수 없도록 함, transaction 이 종료될 때 lock 이 자동으로 해제되지 않으니, 명령어로 해제를 수행

## Pessimistic Lock (배타적 락)
```java
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdWithPessimisticLock(Long id);
```

## Optimistic Lock 

```java
    @Lock(value = LockModeType.OPTIMISTIC)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdWithOptimisticLock(Long id);
```

## Named Lock
* 분산락을 구현할 때 사용,,
* 락해제와 세션관리를 잘 해줘야함
```java
    @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
    void getLock(String key);

    @Query(value = "select release_lock(:key)", nativeQuery = true)
    void releaseLock(String key);
```

## Redis 를 이용한 해결
1. Lettuce
   1. setnx 명령어를 활용하여 분산락 구현
   2. spin lock 방식 : 락을 획득하려는 쓰레드가 락의 획득 가능 여부를 반복해서 확인
2. Redisson
   1. pub-sub 기반의 락 구현 제공
      1. 락 획득을 대기중인 쓰레드에 락 사용가능 여부 전달

### Lettuce 사용
 * named Lock 과 사용방식이 비슷, 세션관리가 크게 필요 없다

```java
   while (!repository.lock(key)) {
            Thread.sleep(100);
        }

        try {
            stockService.decrease(key, quantity);
        } finally {
            repository.unlock(key);
        }
```
* spin 락 방식이기 때문에, 레디스에 부하를 줄 수 있음


### Redisson 사용
- pub-sub 기반 
- 레디슨의 경우 최초부터 락 api를 제공하고 있음
```java

        RLock lock = redissonClient.getLock(key.toString());

        try {
            boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("lock 획득 실패");
                return;
            }

            stockService.decrease(key, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
```
- lettuce 에 비해 구현이 복잡해 질 수 있음


### 각 라이브러리 장단점
Lettuce
- 구현이 간단
- 스프링 redis 를 사용하면 추가로 라이브러리 등록하지 않아도 사용 가능
- spin 락 방식이기 때문에 동시에 많은 스레드가 lock 획득을 위해 대기중이라면 redis에 부하가 갈 수 있음

Redisson
- 락 획득 재시도를 기본으로 제공
- pub-sub 방식이기 때문에 redis에 부하가 덜감
- 별도 라이브러리 사용해야함
- lock 을 라이브러리 차원에서 제공해주기 때문에 학습이 필요

- 재시도가 필요하지 않은 lock 은 lettuce 사용
- 재시도가 필요한 경우 redisson 활용


## mysql 과 redis 비교

### Mysql
- 이미 mysql 을 사용하면 별도의 비용없이 사용 가능
- 어느정도 트래픽까지는 문제없이 사용 가능
- redis보다는 성능이 좋지않다

### Redis
- 활용중인 redis가 없다면 별도의 구축비용과 인프라 관리비용 발생 
- Mysql 에 비해 성능이 뛰어남