/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import com.crio.qeats.dto.Cart;
import com.crio.qeats.dto.Order;

import com.crio.qeats.exceptions.CartNotFoundException;
import com.crio.qeats.models.OrderEntity;

import java.time.LocalTime;
import javax.inject.Provider;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;


@Service
public class OrderRepositoryServiceImpl implements OrderRepositoryService {

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private MongoTemplate mongoTemplate;


  @Override
  public Order placeOrder(Cart cart) {
    Order order = new Order();
    if (cart == null) {
      throw new CartNotFoundException();
    }
    order.setRestaurantId(cart.getRestaurantId());
    order.setUserId(cart.getUserId());
    order.setItems(cart.getItems());
    order.setTotal(cart.getTotal());
    order.setTimePlaced(LocalTime.now().toString());
    ModelMapper mapper = modelMapperProvider.get();
    OrderEntity orderEntity = mapper.map(order, OrderEntity.class);
    mongoTemplate.save(orderEntity, "orders");
    return order;
  }
}