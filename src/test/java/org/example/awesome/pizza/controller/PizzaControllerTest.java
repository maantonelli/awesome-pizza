package org.example.awesome.pizza.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.GlobalExceptionHandler;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.exception.NotFoundException;
import org.example.awesome.pizza.model.PizzaDto;
import org.example.awesome.pizza.model.PizzaRequest;
import org.example.awesome.pizza.service.PizzaService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PizzaController.class})
@ContextConfiguration(classes = {PizzaController.class, GlobalExceptionHandler.class})
class PizzaControllerTest {
  private final String resourceUrl = "/pizza";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper mapper;

  @MockitoBean
  private PizzaService service;

  @Test
  void deletePizza_WhenInvalidId_ShouldResponseError() throws Exception {
    final Long id = null;

    this.mockMvc.perform(delete(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isNotFound());

    verify(service, never()).delete(id);
  }

  @Test
  void deletePizza_WhenInvalidIdFromService_ShouldResponseBadRequest() throws Exception {
    final Long id = 0L;
    doThrow(new BadRequestException("Bad request")).when(service).delete(id);

    this.mockMvc.perform(delete(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isBadRequest());

    verify(service).delete(id);
  }

  @Test
  void deletePizza_WhenNoErrors_ShouldResponseOK() throws Exception {
    final Long id = 1L;
    doNothing().when(service).delete(id);

    this.mockMvc.perform(delete(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isOk());

    verify(service).delete(id);
  }

  @Test
  void createPizza_WhenInvalidRequestFromService_ShouldResponseBadRequest() throws Exception {
    doThrow(new BadRequestException("Bad request")).when(service).save(null);

    this.mockMvc.perform(post(resourceUrl)
            .contentType("application/json")
            .content((byte[]) null))
        .andExpect(status().isBadRequest());

    verify(service).save(any());
  }

  @Test
  void createPizza_WhenError_ShouldResponseServerError() throws Exception {
    final PizzaRequest request = new PizzaRequest();
    doThrow(new InternalServerErrorException("Internal server error")).when(service).save(request);

    this.mockMvc.perform(post(resourceUrl)
            .contentType("application/json")
            .content(mapper.writeValueAsBytes(request))
        )
        .andExpect(status().isInternalServerError());

    verify(service).save(any());
  }

  @Test
  void createPizza_WhenNoError_ShouldResponseOk() throws Exception {
    final PizzaRequest request = Instancio.create(PizzaRequest.class);
    final PizzaDto dto = Instancio.of(PizzaDto.class)
        .set(field(PizzaDto::getName), request.getName())
        .set(field(PizzaDto::getDescription), request.getDescription())
        .create();
    doReturn(dto).when(service).save(request);

    final byte[] contentRequest = mapper.writeValueAsBytes(request);
    final byte[] contentResponse = mapper.writeValueAsBytes(dto);

    this.mockMvc.perform(post(resourceUrl)
            .contentType("application/json")
            .content(contentRequest)
        )
        .andExpect(status().isCreated())
        .andExpect(content().bytes(contentResponse))
    ;

    verify(service).save(any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"test", "TEST", "Test", "tESt"})
  void findPizzas_WhenNoErrors_ShouldReturnOK(final String searchString) throws Exception {
    final List<PizzaDto> results = Instancio.createList(PizzaDto.class);

    doReturn(results).when(service).findByFilter(searchString);

    final byte[] contentResponse = mapper.writeValueAsBytes(results);

    this.mockMvc.perform(get(resourceUrl).param("searchString", searchString))
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(service).findByFilter(searchString);
  }

  private static Stream<Arguments> updatePizza_ParametersKO() {
    return Stream.of(
        Arguments.of(null, null, 404),
        Arguments.of(1L, null, 415),
        Arguments.of(null, Instancio.create(PizzaRequest.class), 404)
    );
  }

  @ParameterizedTest
  @MethodSource("updatePizza_ParametersKO")
  void updatePizza_WhenBadInput_ShouldResponseError(final Long id, final PizzaRequest request, int expectedStatus) throws Exception {
    final byte[] contentRequest = mapper.writeValueAsBytes(request);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .content("application/json")
            .content(contentRequest))
        .andExpect(status().is(expectedStatus));

    verify(service, never()).update(any(), any());
  }

  @Test
  void updatePizza_WhenBadInputFromService_ShouldResponseBadRequest() throws Exception {
    final Long id = 0L;
    final PizzaRequest request = new PizzaRequest();
    doThrow(new BadRequestException("Bad request")).when(service).update(id, request);

    final byte[] contentRequest = mapper.writeValueAsBytes(request);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .contentType("application/json")
            .content(contentRequest)
        )
        .andExpect(status().isBadRequest());

    verify(service).update(id, request);
  }

  @Test
  void updatePizza_WhenNotFoundFromService_ShouldResponseNotFound() throws Exception {
    final Long id = 1L;
    final PizzaRequest request = new PizzaRequest();
    doThrow(new NotFoundException("Not found")).when(service).update(id, request);

    final byte[] contentRequest = mapper.writeValueAsBytes(request);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .contentType("application/json")
            .content(contentRequest)
        )
        .andExpect(status().isNotFound());

    verify(service).update(id, request);
  }

  @Test
  void updatePizza_WhenServerErrorFromService_ShouldResponseServerError() throws Exception {
    final Long id = 1L;
    final PizzaRequest request = new PizzaRequest();
    doThrow(new InternalServerErrorException("Internal server error")).when(service).update(id, request);

    final byte[] contentRequest = mapper.writeValueAsBytes(request);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .contentType("application/json")
            .content(contentRequest)
        )
        .andExpect(status().isInternalServerError());

    verify(service).update(id, request);
  }

  @Test
  void updatePizza_WhenNoErrors_ShouldResponseOk() throws Exception {
    final Long id = 1L;
    final PizzaRequest request = Instancio.create(PizzaRequest.class);
    final PizzaDto result = Instancio.of(PizzaDto.class)
        .set(field(PizzaDto::getName), request.getName())
        .set(field(PizzaDto::getDescription), request.getDescription())
        .create();
    doReturn(result).when(service).update(id, request);

    final byte[] contentRequest = mapper.writeValueAsBytes(request);
    final byte[] contentResponse = mapper.writeValueAsBytes(result);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .contentType("application/json")
            .content(contentRequest)
        )
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(service).update(id, request);
  }
}
