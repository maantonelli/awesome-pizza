package org.example.awesome.pizza.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface BaseReadService<TModel> {
  TModel findById(final Long id);
  Page<TModel> findAll(final Pageable page);
}
