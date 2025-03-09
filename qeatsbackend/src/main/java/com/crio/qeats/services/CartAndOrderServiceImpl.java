package com.crio.qeats.services;

import com.crio.qeats.dto.Cart;
import com.crio.qeats.dto.Item;
import com.crio.qeats.dto.Order;
import com.crio.qeats.exceptions.CartNotFoundException;
import com.crio.qeats.exceptions.EmptyCartException;
import com.crio.qeats.exceptions.ItemNotFromSameRestaurantException;
import com.crio.qeats.exchanges.CartModifiedResponse;
import com.crio.qeats.models.CartEntity;
import com.crio.qeats.models.OrderEntity;
import com.crio.qeats.repositoryservices.CartRepositoryService;
import com.crio.qeats.repositoryservices.OrderRepositoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import javax.swing.text.html.Option;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class CartAndOrderServiceImpl implements CartAndOrderService {


  @Autowired
  private CartRepositoryService cartRepositoryService;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private OrderRepositoryService orderRepositoryService;

  @Autowired
  private MenuService menuService;

  @Autowired(required = true)
  private Provider<ModelMapper> modelMapperProvider;

  //adding throws CartNotFoundException, add will throw exception and handle it in the controller

  @Override
  public boolean isUserThere(String userId) {
    Optional<Cart> optionalCart = cartRepositoryService.findCartByUserId(userId);
    if (optionalCart.isPresent()) {
      return true;
    }
    return false;
  }

  @Override
  public Cart findOrCreateCart(String userId) throws CartNotFoundException {
    Optional<Cart> optionalCart = cartRepositoryService.findCartByUserId(userId);
    if (optionalCart.isPresent()) {
      return optionalCart.get();
    } else {
      Cart cart = new Cart();
      cart.setUserId(userId);
      cart.setRestaurantId("");
      cartRepositoryService.findCartByCartId(cart.getId());
      cartRepositoryService.createCart(cart);
      return cart;
    }
  }

  /**
   * TODO: CRIO_TASK_MODULE_MENUAPI - Implement addItemToCart.
   * Add item to the given cart.
   * - All items added should be from same restaurant
   * - If the above constraint is not satisfied, throw ItemNotFromSameRestaurantException exception
   *
   * @param itemId       - id of the item to be added
   * @param cartId       - id of the cart where item should be added
   * @param restaurantId - id of the restaurant where the given item comes from
   * @return - return - CartModifiedResponse
   * @throws ItemNotFromSameRestaurantException - when item to be added comes from
   *                                            different restaurant.
   *                                            You should set cartResponseType
   *                                            to 102(ITEM_NOT_FROM_SAME_RESTAURANT)
   *                                            the documentation should have been
   *                                            1) if the cartId is valid and item is
   *                                            from same restaurant then add to the cart
   *                                            2) If item is from different restaurant
   *                                            then fill cartResponseType to 102
   */
  @Override
  public CartModifiedResponse addItemToCart(String itemId, String cartId, String restaurantId)
      throws ItemNotFromSameRestaurantException, CartNotFoundException {

    try {
      Item item = menuService.findItem(itemId, restaurantId);
      Cart cartFound = cartRepositoryService.findCartByCartId(cartId);
      if (!cartFound.getRestaurantId().equals("")
          && !cartFound.getRestaurantId().equals(restaurantId)) {
        CartModifiedResponse cartModifiedResponse = new CartModifiedResponse();
        cartModifiedResponse.setCartResponseType(102);
        cartModifiedResponse.setCart(new Cart());
        return cartModifiedResponse;
      }
      Cart cart = cartRepositoryService.addItem(item, cartId, restaurantId);
      CartModifiedResponse cartModifiedResponse = new CartModifiedResponse();
      cartModifiedResponse.setCartResponseType(0);
      cartModifiedResponse.setCart(cart);
      return cartModifiedResponse;
    } catch (ItemNotFromSameRestaurantException e) {
      CartModifiedResponse cartModifiedResponse = new CartModifiedResponse();
      cartModifiedResponse.setCartResponseType(102);
      cartModifiedResponse.setCart(new Cart());
      return cartModifiedResponse;
    }
  }

  /**
   * TODO: CRIO_TASK_MODULE_MENUAPI - Implement removeItemFromCart.
   * Remove item from the given cart.
   *
   * @param itemId       - id of the item to be removed
   * @param cartId       - id of the cart where item should be removed
   * @param restaurantId - id of the restaurant where the given item comes from
   * @return - return - CartModifiedResponse, set cartResponseType to 0
   */
  @Override
  public CartModifiedResponse removeItemFromCart(String itemId,
                                                 String cartId,
                                                 String restaurantId) {
    try {
      Item item = menuService.findItem(itemId, restaurantId);

      Cart cart = cartRepositoryService.removeItem(item, cartId, restaurantId);
      CartModifiedResponse cartModifiedResponse = new CartModifiedResponse();
      cartModifiedResponse.setCartResponseType(0);
      cartModifiedResponse.setCart(cart);
      return cartModifiedResponse;
    } catch (ItemNotFromSameRestaurantException e) {
      CartModifiedResponse cartModifiedResponse = new CartModifiedResponse();
      cartModifiedResponse.setCartResponseType(102);
      cartModifiedResponse.setCart(cartRepositoryService.findCartByCartId(cartId));
      return cartModifiedResponse;
    }

  }

  /**
   * TODO: CRIO_TASK_MODULE_MENUAPI - Implement postOrder.
   * Place order for the given cart
   *
   * @param cartId - id of the cart to be converted to order
   * @return Order - return the order that was just placed
   * @throws EmptyCartException - should throw this exception when cart is empty
   */
  @Override
  public Order postOrder(String cartId) throws EmptyCartException {
    try {
      Cart cart = cartRepositoryService.findCartByCartId(cartId);
    } catch (CartNotFoundException e) {
      throw new EmptyCartException("Cart is Empty");
    }
    Cart cart = cartRepositoryService.findCartByCartId(cartId);
    return orderRepositoryService.placeOrder(cart);
  }
}
