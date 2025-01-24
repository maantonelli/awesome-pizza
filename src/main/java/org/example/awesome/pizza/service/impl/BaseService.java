package org.example.awesome.pizza.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.example.awesome.pizza.domain.BaseEntity;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.exception.NotFoundException;
import org.example.awesome.pizza.mapper.BaseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
abstract class BaseService<TModel, TRequest, TEntity extends BaseEntity> {
  protected final JpaRepository<TEntity, Long> repository;
  protected final BaseMapper<TModel, TRequest, TEntity> mapper;

  public TModel findById(final Long id) {
    if (id == null)
      throw new BadRequestException("Invalid input ID");

    return repository.findById(id)
        .map(mapper::toDto)
        .orElseThrow(() -> new NotFoundException("No entity found by ID %d".formatted(id)));
  }

  public Page<TModel> findAll(final Pageable page) {
    final Pageable actualPage = ObjectUtils.defaultIfNull(page, Pageable.unpaged(Sort.by("createdDate")));

    return Optional.of(repository.findAll(actualPage))
        .map(p -> p.map(mapper::toDto))
        .orElseGet(() -> Page.empty(page));
  }

  public void delete(final Long id) {
    if (id == null)
      throw new BadRequestException("Invalid input ID");

    repository.deleteById(id);
  }

  protected abstract TEntity prePersist(final TEntity entity, final TRequest request);

  public TModel save(final TRequest request) {
    if (request == null)
      throw new BadRequestException("No valid input entity to save");

    return Optional.of(mapper.toEntity(request))
        .map(toSave -> this.prePersist(toSave, request))
        .map(repository::save)
        .map(mapper::toDto)
        .orElseThrow(() -> new InternalServerErrorException("Error while inserting entity: %s".formatted(request.toString())));
  }

  public TModel update(final Long id, final TRequest request) {
    if (ObjectUtils.anyNull(id, request))
      throw new BadRequestException("No valid input parameters");

    final TEntity entity = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Entity with id %d does not exists".formatted(id)));

    mapper.patch(request, entity);

    return Optional.of(this.prePersist(entity, request))
        .map(repository::save)
        .map(mapper::toDto)
        .orElseThrow(() -> new InternalServerErrorException("Error while updating entity id %d: %s".formatted(id, request.toString())));
  }
}
