package com.goat.demo.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goat.demo.domain.Resume;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

}
