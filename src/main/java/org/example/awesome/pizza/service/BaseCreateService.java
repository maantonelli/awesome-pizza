package org.example.awesome.pizza.service;

interface BaseCreateService<TModel, TRequest> {
  TModel save(final TRequest request);
}
