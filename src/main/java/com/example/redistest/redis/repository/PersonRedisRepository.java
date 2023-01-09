package com.example.redistest.redis.repository;

import com.example.redistest.redis.domain.Person;
import org.springframework.data.repository.CrudRepository;

public interface PersonRedisRepository extends CrudRepository<Person, String> {

}
