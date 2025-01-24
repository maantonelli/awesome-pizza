package org.example.awesome.pizza.mapper;

public interface BaseMapper<TModel, TRequest, TEntity> {

  TModel toDto(final TEntity entity);
  TEntity toEntity(final TRequest request);
  void patch(final TRequest source, final TEntity target);
}
