package org.example.awesome.pizza.service;

interface BaseUpdateService<TModel, TRequest> {
  TModel update(final Long id, final TRequest request);
}
