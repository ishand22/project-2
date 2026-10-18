package com.entitleflow;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface DiagnosticEventRepository extends MongoRepository<DiagnosticEvent,String> {}
