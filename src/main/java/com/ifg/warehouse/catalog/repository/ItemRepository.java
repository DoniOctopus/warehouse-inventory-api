package com.ifg.warehouse.catalog.repository;

import com.ifg.warehouse.catalog.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
