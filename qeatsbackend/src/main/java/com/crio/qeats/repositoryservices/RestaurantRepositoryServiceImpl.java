/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.dto.Item;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Provider;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MenuRepository menuRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
                                                    Double longitude,
                                                    LocalTime currentTime,
                                                    Double servingRadiusInKms) {
    if (GlobalConstants.isCacheAvailable()) {
      return findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime,
          servingRadiusInKms);
    } else {
      return findAllRestaurantsCloseFromDb(latitude, longitude,
          currentTime, servingRadiusInKms);
    }
  }

  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude,
                                                         Double longitude,
                                                         LocalTime currentTime,
                                                         Double servingRadiusInKms) {


    List<Restaurant> lst = new ArrayList<>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurantEntity : restaurantRepository.findAll()) {

      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime,
          latitude, longitude, servingRadiusInKms)) {
        Restaurant restaurant = new Restaurant();
        modelMapper.map(restaurantEntity, restaurant);
        lst.add(restaurant);
      }
    }
    return lst;

  }


  // TODO: CRIO_TASK_MODULE_REDIS - Implement caching.

  /**
   * Implement caching for restaurants closeby.
   * Whenever the entry is not there in the cache, you will have to populate it from DB.
   * If the entry is already available in the cache, then return it from cache to save DB lookup.
   * The cache entries should expire in GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS.
   * Make sure you use something like a GeoHash with a slightly lower precision,
   * so that for lat/long that are slightly close, the function returns the same set of restaurants.
   */
  private List<Restaurant> findAllRestaurantsCloseByFromCache(
      Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    GeoHash geoHash = GeoHash.withCharacterPrecision(latitude,
        longitude, 7);
    String key = geoHash.toBase32();
    Jedis jedis = GlobalConstants.getJedisPool().getResource();
    List<Restaurant> rst = new ArrayList<>();
    List<String> rstString = new ArrayList<>();
    Restaurant restaurant1 = null;
    String actualJsonString;
    String finalJsonString = null;
    if (jedis.get(key) == null) {
      rst = findAllRestaurantsCloseFromDb(latitude, longitude,
          currentTime, servingRadiusInKms);
      for (Restaurant restaurant : rst) {

        try {
          actualJsonString = objectMapper.writeValueAsString(restaurant);
          rstString.add(actualJsonString);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }

      }
      finalJsonString = String.join(";", rstString);
      jedis.setex(key, GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS, finalJsonString);

    } else {
      finalJsonString = jedis.get(key);
      List<String> items = Arrays.asList(finalJsonString.split(";"));
      for (String item : items) {
        try {
          restaurant1 = objectMapper.readValue(item, Restaurant.class);
          rst.add(restaurant1);
        } catch (IOException e) {
          e.printStackTrace();
        }

      }

    }

    return rst;
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
                                                String searchString,
                                                LocalTime currentTime,
                                                Double servingRadiusInKms) {


    Query query = new Query(where("name").regex("^" + searchString + "$", "i"));

    // Execute the query and find one matching entry

    List<RestaurantEntity> restaurantEntity
        = mongoTemplate.find(query, RestaurantEntity.class, "restaurants");
    List<RestaurantEntity> partialSplit = new ArrayList<>();
    String[] eachSearch = searchString.split("\\s+");
    List<RestaurantEntity> restaurantEntityPartial;
    for (int i = 0; i < eachSearch.length; i++) {
      Query partialMatchQuery
          = new Query(where("name").regex(Pattern.compile(searchString,
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));

      restaurantEntityPartial = mongoTemplate.find(partialMatchQuery, RestaurantEntity.class,
          "restaurants");
      for (RestaurantEntity re : restaurantEntityPartial) {
        partialSplit.add(re);
      }
    }
    Set<RestaurantEntity> set = new HashSet<>();
    List<Restaurant> valid = new ArrayList<>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity rstEntity : restaurantEntity) {
      if (isRestaurantCloseByAndOpen(rstEntity,
          currentTime, latitude, longitude, servingRadiusInKms)) {
        Restaurant restaurant = new Restaurant();
        modelMapper.map(rstEntity, restaurant);
        valid.add(restaurant);
        set.add(rstEntity);
      }
    }
    for (RestaurantEntity rstEntity : partialSplit) {
      if (isRestaurantCloseByAndOpen(rstEntity,
          currentTime, latitude, longitude, servingRadiusInKms)) {
        if (!set.contains(rstEntity)) {
          Restaurant restaurant = new Restaurant();
          modelMapper.map(rstEntity, restaurant);
          valid.add(restaurant);
          set.add(rstEntity);
        }
      }
    }

    return valid;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<RestaurantEntity> partialSplit = new ArrayList<>();
    List<RestaurantEntity> restaurantEntityPartial;
    String[] eachAttribute = searchString.split("\\s+");
    for (String search : eachAttribute) {
      Query partialMatchQuery
          = new Query(where("attributes").regex(Pattern.compile(searchString,
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
      restaurantEntityPartial = mongoTemplate.find(partialMatchQuery, RestaurantEntity.class,
          "restaurants");
      for (RestaurantEntity re : restaurantEntityPartial) {
        partialSplit.add(re);
      }
    }
    List<Restaurant> valid = new ArrayList<>();
    ModelMapper modelMapper = modelMapperProvider.get();
    Set<RestaurantEntity> set = new HashSet<>();
    for (RestaurantEntity rstEntity : partialSplit) {

      if (isRestaurantCloseByAndOpen(rstEntity,
          currentTime, latitude, longitude, servingRadiusInKms)) {
        if (!set.contains(rstEntity)) {
          Restaurant restaurant = new Restaurant();
          modelMapper.map(rstEntity, restaurant);
          valid.add(restaurant);
          set.add(rstEntity);
        }
      }

    }
    return valid;


  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    String[] strings = searchString.split("\\s+");
    List<Restaurant> restaurantsQueried = new ArrayList<>();
    Set<String> uniqueRestaurants = new HashSet<>();
    for (String search : strings) {
      Query query = new Query();
      List<Item> allValidId = new ArrayList<>();
      Set<String> idSet = new HashSet<>();
      query.addCriteria(Criteria.where("name").regex("^" + search + "$", "i"));
      List<ItemEntity> itemEntityList = mongoTemplate.find(query, ItemEntity.class);
      for (ItemEntity itemEntity : itemEntityList) {
        ModelMapper mapper = modelMapperProvider.get();
        Item item = mapper.map(itemEntity, Item.class);
        allValidId.add(item);
        idSet.add(item.getId());
      }

      query = new Query();
      query.addCriteria(Criteria.where("name").regex(search, "i"));
      itemEntityList = mongoTemplate.find(query, ItemEntity.class);
      for (ItemEntity itemEntity : itemEntityList) {
        ModelMapper mapper = modelMapperProvider.get();
        Item item = mapper.map(itemEntity, Item.class);
        if (!idSet.contains(item.getId())) {
          allValidId.add(item);
          idSet.add(item.getId());
        }
      }

      for (Item items : allValidId) {
        query = new Query();
        query.addCriteria(Criteria.where("items.itemId").regex("^" + items.getItemId() + "$"));
        List<MenuEntity> menuEntityList = mongoTemplate.find(query, MenuEntity.class);
        for (MenuEntity menuEntity : menuEntityList) {
          String restaurantId = menuEntity.getRestaurantId();
          query = new Query();
          query.addCriteria(Criteria.where("restaurantId").regex("^" + restaurantId + "$"));
          List<RestaurantEntity> restaurantEntityList
              = mongoTemplate.find(query, RestaurantEntity.class);
          for (RestaurantEntity restaurantEntity : restaurantEntityList) {
            if (isRestaurantCloseByAndOpen(restaurantEntity,
                currentTime, latitude, longitude, servingRadiusInKms)) {
              ModelMapper mapper = modelMapperProvider.get();
              Restaurant restaurant = mapper.map(restaurantEntity, Restaurant.class);
              if (!uniqueRestaurants.contains(restaurant.getRestaurantId())) {
                restaurantsQueried.add(restaurant);
                uniqueRestaurants.add(restaurant.getRestaurantId());
              }
            }
          }
        }
      }
    }

    return restaurantsQueried;

  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude,
                                                          Double longitude,
                                                          String searchString,
                                                          LocalTime currentTime,
                                                          Double servingRadiusInKms) {

    Query query = new Query();
    List<Item> allValidId = new ArrayList<>();

    query.addCriteria(Criteria.where("attributes").regex("^" + searchString + "$", "i"));
    List<ItemEntity> itemEntityList = mongoTemplate.find(query, ItemEntity.class);
    for (ItemEntity itemEntity : itemEntityList) {
      ModelMapper mapper = modelMapperProvider.get();
      Item item = mapper.map(itemEntity, Item.class);
      allValidId.add(item);

    }
    List<Restaurant> restaurantsQueried = new ArrayList<>();
    Set<String> uniqueRestaurants = new HashSet<>();
    for (Item items : allValidId) {
      query = new Query();
      query.addCriteria(Criteria.where("items").is(items));
      List<MenuEntity> menuEntityList = mongoTemplate.find(query, MenuEntity.class);
      for (MenuEntity menuEntity : menuEntityList) {
        String restaurantId = menuEntity.getRestaurantId();
        query = new Query();
        query.addCriteria(Criteria.where("restaurantId").is(restaurantId));
        List<RestaurantEntity> restaurantEntityList
            = mongoTemplate.find(query, RestaurantEntity.class);
        for (RestaurantEntity restaurantEntity : restaurantEntityList) {
          if (isRestaurantCloseByAndOpen(restaurantEntity,
              currentTime, latitude, longitude, servingRadiusInKms)) {
            ModelMapper mapper = modelMapperProvider.get();
            Restaurant restaurant = mapper.map(restaurantEntity, Restaurant.class);
            if (!uniqueRestaurants.contains(restaurant.getRestaurantId())) {
              restaurantsQueried.add(restaurant);
              uniqueRestaurants.add(restaurant.getRestaurantId());
            }
          }
        }
      }
    }

    return restaurantsQueried;


  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   *
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
                                             LocalTime currentTime,
                                             Double latitude,
                                             Double longitude,
                                             Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }


}
