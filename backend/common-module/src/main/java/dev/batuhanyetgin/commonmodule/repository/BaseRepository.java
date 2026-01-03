package dev.batuhanyetgin.commonmodule.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<D, T> extends MongoRepository<D, T> {
}