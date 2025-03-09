/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.crio.qeats.dto.Cart;
import com.crio.qeats.dto.Item;
import com.crio.qeats.exceptions.CartNotFoundException;
import com.crio.qeats.exceptions.ItemNotFromSameRestaurantException;
import com.crio.qeats.models.CartEntity;
import com.crio.qeats.repositories.CartRepository;

import java.util.List;
import java.util.Optional;
import javax.inject.Provider;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;


@Service
public class CartRepositoryServiceImpl implements CartRepositoryService {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private CartRepository cartRepository;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Override
  @NonNull
  public String createCart(Cart cart) {
    ModelMapper mapper = modelMapperProvider.get();
    CartEntity cartEntity = mapper.map(cart, CartEntity.class);

    //System.out.println(cartEntity.getId() + " " + cartEntity.getUserId() + " $$$$$$");
    cartRepository.save(cartEntity);

    return cartEntity.getId();
  }

  /**
   * TODO: CRIO_TASK_MODULE_MENUAPI - Find and return the cart for the corresponding userId.
   *
   * @param userId - userId
   * @return return Cart - cart corresponding to user id.
   */

  @Override
  public Optional<Cart> findCartByUserId(String userId) {



    List<CartEntity> cartEntities = cartRepository.findByUserId(userId);
    Optional<Cart> optionalCart = Optional.empty();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (CartEntity cartEntity : cartEntities) {
      Cart cart = new Cart();
      modelMapper.map(cartEntity, cart);
      optionalCart = Optional.of(cart);
    }

    return optionalCart;
  }

  @Override
  public Cart findCartByCartId(String cartId) throws CartNotFoundException {
    /**
     * TODO: CRIO_TASK_MODULE_MENUAPI - Find and return the cart for the given cartId.
     * @param cartId - id of the cart to be found
     * @return Cart - cart corresponding to the given cartId
     * @throws CartNotFoundException - if cartId is invalid
     */

    Optional<CartEntity> cartEntities = cartRepository.findById(cartId);

    if (!(cartEntities.isPresent()) || cartEntities.get().getId().equals("")) {
      throw new CartNotFoundException();
    }
    ModelMapper modelMapper = modelMapperProvider.get();
    Cart cart;
    CartEntity cartEntity = cartEntities.get();
    cart = modelMapper.map(cartEntity, Cart.class);
    return cart;

  }


  /**
   * TODO: CRIO_TASK_MODULE_MENUAPI - Add the given item to cart with the given cartId.
   *
   * @param item         - item to be added
   * @param cartId       - if of the cart
   * @param restaurantId - restaurant id of the the item
   * @return Cart - updated cart after adding item
   * @throws CartNotFoundException - if cartId is invalid
   */
  public Cart addItem(Item item, String cartId, String restaurantId)
      throws CartNotFoundException {

    Cart cart = findCartByCartId(cartId);
    ModelMapper mapper = modelMapperProvider.get();
    CartEntity cartEntity = new CartEntity();
    mapper.map(cart, cartEntity);
    cartEntity.addItem(item);
    cartEntity.setRestaurantId(restaurantId);
    cartRepository.save(cartEntity);
    mapper.map(cartEntity, cart);
    return cart;

  }

  /**
   * TODO: CRIO_TASK_MODULE_MENUAPI - Remove the given item from cart with the given cartId.
   *
   * @param item         - item to be removed
   * @param cartId       - id of the cart
   * @param restaurantId - restaurant id of the item
   * @return Cart - updated cart after removing item
   * @throws CartNotFoundException if cartId is invalid
   */
  public Cart removeItem(Item item, String cartId,
                         String restaurantId) throws CartNotFoundException {

    Cart cart = findCartByCartId(cartId);
    ModelMapper mapper = modelMapperProvider.get();
    CartEntity cartEntity = new CartEntity();
    mapper.map(cart, cartEntity);
    cartEntity.removeItem(item);
    if (cartEntity.getItems().size() == 0) {
      cartEntity.setRestaurantId("");

    }
    cartRepository.save(cartEntity);
    mapper.map(cartEntity, cart);
    return cart;
  }


}