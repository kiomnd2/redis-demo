package com.example.redistest;

import com.example.redistest.redis.domain.Person;
import com.example.redistest.redis.repository.PersonRedisRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest
public class RedisTest {

    @Autowired
    private PersonRedisRepository repository;

    @Test
    void test() {
        Person person = new Person("kim", 20, LocalDateTime.now());

        repository.save(person);

        Optional<Person> byId = repository.findById(person.getId());

        Assertions.assertThat(byId.get().getName()).isEqualTo("kim");

        System.out.println("current Count : " + repository.count());

        repository.delete(person);

        System.out.println("after delete Count : " + repository.count());;
    }
}
