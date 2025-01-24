package org.example.awesome.pizza.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.ConflictException;
import org.example.awesome.pizza.exception.GlobalExceptionHandler;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.exception.NotFoundException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.mapper.OrderMapperImpl;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderRequest;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.service.OrderService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderController.class})
@ContextConfiguration(classes = {OrderController.class, OrderMapperImpl.class, GlobalExceptionHandler.class})
class OrderControllerTest {
  private final String resourceUrl = "/order";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper mapper;

  @MockitoBean
  private OrderService service;
  @MockitoSpyBean
  private OrderMapper orderMapper;

  @Test
  void createOrder_WhenInvalidRequestFromService_ShouldResponseBadRequest() throws Exception {
    doThrow(new BadRequestException("Bad request")).when(service).save(any());

    this.mockMvc.perform(post(resourceUrl)
            .contentType("application/json")
            .content(("{}".getBytes())))
        .andExpect(status().isBadRequest());

    verify(service).save(any());
  }

  @Test
  void createOrder_WhenNoRequestBody_ShouldResponseBadRequest() throws Exception {
    final OrderInternalReq request = null;

    this.mockMvc.perform(post(resourceUrl)
            .contentType("application/json")
            .content(mapper.writeValueAsBytes(request))
        )
        .andExpect(status().isBadRequest());

    verify(service, never()).save(any());
  }

  @Test
  void createOrder_WhenError_ShouldResponseServerError() throws Exception {
    final OrderInternalReq request = new OrderInternalReq();
    doThrow(new InternalServerErrorException("Internal server error")).when(service).save(request);

    this.mockMvc.perform(post(resourceUrl)
            .contentType("application/json")
            .content(mapper.writeValueAsBytes(request))
        )
        .andExpect(status().isInternalServerError());

    verify(service).save(any());
  }

  @Test
  void createOrder_WhenNoError_ShouldResponseOk() throws Exception {
    final OrderInternalReq request = Instancio.of(OrderInternalReq.class)
        .ignore(field(OrderInternalReq::getChefId))
        .create();
    final OrderDto dto = Instancio.of(OrderDto.class)
        .set(field(OrderDto::getStatus), request.getStatus())
        .set(field(OrderDto::getPizzas), request.getPizzas())
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

  @Test
  void findOrderByCode_WhenBadInput_ShouldResponseNotFound() throws Exception {
    final Long code = null;

    this.mockMvc.perform(get(resourceUrl.concat("/code/{code}"), code))
        .andExpect(status().isNotFound());

    verify(service, never()).findByCode(code);
  }

  @Test
  void findOrderByCode_WhenInvalidInputFromService_ShouldResponseBadRequest() throws Exception {
    final Long code = 0L;
    doThrow(new BadRequestException("Bad request")).when(service).findByCode(code);

    this.mockMvc.perform(get(resourceUrl.concat("/code/{code}"), code))
        .andExpect(status().isBadRequest());

    verify(service).findByCode(code);
  }

  @Test
  void findOrderByCode_WhenNoEntityFound_ShouldResponseNotFound() throws Exception {
    final Long code = 0L;
    doThrow(new NotFoundException("Not found")).when(service).findByCode(code);

    this.mockMvc.perform(get(resourceUrl.concat("/code/{code}"), code))
        .andExpect(status().isNotFound());

    verify(service).findByCode(code);
  }

  @Test
  void findOrderByCode_WhenNoErrors_ShouldResponseOk() throws Exception {
    final Long code = 1L;
    final OrderDto result = Instancio.create(OrderDto.class);
    doReturn(result).when(service).findByCode(code);

    final byte[] contentResponse = mapper.writeValueAsBytes(result);

    this.mockMvc.perform(get(resourceUrl.concat("/code/{code}"), code))
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(service).findByCode(code);
  }

  @Test
  void findOrderById_WhenBadInput_ShouldResponseNotFound() throws Exception {
    final Long id = null;

    this.mockMvc.perform(get(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isNotFound());

    verify(service, never()).findByCode(id);
  }

  @Test
  void findOrderById_WhenInvalidInputFromService_ShouldResponseBadRequest() throws Exception {
    final Long id = 0L;
    doThrow(new BadRequestException("Bad request")).when(service).findById(id);

    this.mockMvc.perform(get(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isBadRequest());

    verify(service).findById(id);
  }

  @Test
  void findOrderById_WhenNoEntityFound_ShouldResponseNotFound() throws Exception {
    final Long id = 0L;
    doThrow(new NotFoundException("Not found")).when(service).findById(id);

    this.mockMvc.perform(get(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isNotFound());

    verify(service).findById(id);
  }

  @Test
  void findOrderById_WhenNoErrors_ShouldResponseOk() throws Exception {
    final Long id = 1L;
    final OrderDto result = Instancio.create(OrderDto.class);
    doReturn(result).when(service).findById(id);

    final byte[] contentResponse = mapper.writeValueAsBytes(result);

    this.mockMvc.perform(get(resourceUrl.concat("/{id}"), id))
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(service).findById(id);
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = OrderStatus.class)
  void findOrders_WhenErrorFromService_ShouldResponseServerError(final OrderStatus status) throws Exception {
    final Optional<OrderStatus> optStatus = Optional.ofNullable(status);
    final String[] stringStatus = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values())).stream()
        .map(OrderStatus::name)
        .toArray(String[]::new);
    final List<OrderStatus> statuses = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values()));
    doThrow(new RuntimeException("Timeout exception")).when(service).findAllOrders(statuses);

    this.mockMvc.perform(get(resourceUrl).param("statuses", stringStatus))
        .andExpect(status().isInternalServerError());

    verify(service).findAllOrders(statuses);
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = OrderStatus.class)
  void findOrders_WhenFound_ShouldResponseOk(final OrderStatus status) throws Exception {
    final Optional<OrderStatus> optStatus = Optional.ofNullable(status);
    final String[] stringStatus = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values())).stream()
        .map(OrderStatus::name)
        .toArray(String[]::new);
    final List<OrderStatus> statuses = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values()));
    final List<OrderDto> results = Instancio.ofList(OrderDto.class)
        .set(field(OrderDto::getStatus), status)
        .create();
    doReturn(results).when(service).findAllOrders(statuses);

    final byte[] contentResponse = mapper.writeValueAsBytes(results);

    this.mockMvc.perform(get(resourceUrl).param("statuses", stringStatus))
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(service).findAllOrders(statuses);
  }

  @Test
  void updateOrder_WhenBadId_ShouldResponseNotFound() throws Exception {
    final Long id = null;
    final Long chefId = 0L;

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .content("{}".getBytes())
            .header("X-Chef-ID", chefId))
        .andExpect(status().isNotFound());

    verify(orderMapper, never()).toInternalReq(any());
    verify(service, never()).updateOrder(any(), any(), any());
  }

  @Test
  void updateOrder_WhenBadInputFromService_ShouldResponseBadRequest() throws Exception {
    final Long id = 0L;
    final Long chefId = 0L;
    final OrderRequest request = new OrderRequest().status(OrderStatus.CREATED);
    doCallRealMethod().when(orderMapper).toInternalReq(request);
    doThrow(new BadRequestException("Bad request")).when(service).updateOrder(eq(id), any(OrderInternalReq.class), eq(chefId));

    final byte[] contentRequest = mapper.writeValueAsBytes(request);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .content(contentRequest)
            .contentType("application/json")
            .header("X-Chef-ID", chefId))
        .andExpect(status().isBadRequest());

    verify(orderMapper).toInternalReq(request);
    verify(service).updateOrder(eq(id), any(OrderInternalReq.class), eq(chefId));
  }

  @ParameterizedTest
  @EnumSource(value = OrderStatus.class)
  void updateOrder_WhenPatchOk_ShouldResponseOk(final OrderStatus status) throws Exception {
    final Long id = 1L;
    final Long chefId = 1L;
    final OrderRequest request = new OrderRequest().status(status);
    final OrderDto result = Instancio.of(OrderDto.class)
        .set(field(OrderDto::getId), id)
        .set(field(OrderDto::getStatus), status)
        .create();
    doCallRealMethod().when(orderMapper).toInternalReq(request);
    doReturn(result).when(service).updateOrder(eq(id), any(OrderInternalReq.class), eq(chefId));

    final byte[] contentRequest = mapper.writeValueAsBytes(request);
    final byte[] contentResponse = mapper.writeValueAsBytes(result);

    this.mockMvc.perform(put(resourceUrl.concat("/{id}"), id)
            .content(contentRequest)
            .contentType("application/json")
            .header("X-Chef-ID", chefId))
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(orderMapper).toInternalReq(request);
    verify(service).updateOrder(eq(id), any(OrderInternalReq.class), eq(chefId));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {1L})
  void takeChargeNext_WhenFoundNext_ShouldResponseOk(final Long id) throws Exception {
    final Long chefId = 1L;
    final String stringId = Optional.ofNullable(id).map(Objects::toString).orElse(null);
    final OrderDto result = Instancio.of(OrderDto.class)
        .set(field(OrderDto::getStatus), OrderStatus.COOKING)
        .create();
    doReturn(Optional.of(result)).when(service).takeNext(chefId, id);

    final byte[] contentResponse = mapper.writeValueAsBytes(result);

    this.mockMvc.perform(patch(resourceUrl.concat("/next"))
            .param("id", stringId)
            .header("X-Chef-ID", chefId))
        .andExpect(status().isOk())
        .andExpect(content().bytes(contentResponse));

    verify(service).takeNext(chefId, id);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {1L})
  void takeChargeNext_WhenNotFoundNext_ShouldResponseNoContent(final Long id) throws Exception {
    final Long chefId = 1L;
    final String stringId = Optional.ofNullable(id).map(Objects::toString).orElse(null);
    doReturn(Optional.empty()).when(service).takeNext(chefId, id);

    this.mockMvc.perform(patch(resourceUrl.concat("/next"))
            .param("id", stringId)
            .header("X-Chef-ID", chefId))
        .andExpect(status().isNoContent());

    verify(service).takeNext(chefId, id);
  }

  @Test
  void takeChargeNext_WhenConflictFromService_ShouldResponseConflict() throws Exception {
    final Long chefId = 1L;
    doThrow(new ConflictException("Conflict")).when(service).takeNext(chefId, null);

    this.mockMvc.perform(patch(resourceUrl.concat("/next")).header("X-Chef-ID", chefId))
        .andExpect(status().isConflict());

    verify(service).takeNext(chefId, null);
  }

  @Test
  void takeChargeNext_WhenBadRequestFromService_ShouldResponseBadRequest() throws Exception {
    final Long chefId = 1L;
    doThrow(new BadRequestException("Bad request")).when(service).takeNext(chefId, null);

    this.mockMvc.perform(patch(resourceUrl.concat("/next")).header("X-Chef-ID", chefId))
        .andExpect(status().isBadRequest());

    verify(service).takeNext(chefId, null);
  }

  @Test
  void takeChargeNext_WhenNoChefId_ShouldResponseBadRequest() throws Exception {
    this.mockMvc.perform(patch(resourceUrl.concat("/next")))
        .andExpect(status().isBadRequest());

    verify(service, never()).takeNext(any(), any());
  }
}
