/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@Primary
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {


    int currentHour = currentTime.getHour();
    int minute = currentTime.getMinute();
    boolean flag = ((currentHour >= 8 && currentHour <= 10)
        || (currentHour >= 13 && currentHour <= 14)
        || (currentHour >= 19 && currentHour <= 21));
    List<Restaurant> rst;
    if (flag) {
      if (((currentHour == 10) || (currentHour == 14) || (currentHour == 21)) && minute > 0) {
        rst = restaurantRepositoryService.findAllRestaurantsCloseBy(
            getRestaurantsRequest.getLatitude(),
            getRestaurantsRequest.getLongitude(), currentTime,
            this.normalHoursServingRadiusInKms);
      } else {
        rst = restaurantRepositoryService.findAllRestaurantsCloseBy(
            getRestaurantsRequest.getLatitude(),
            getRestaurantsRequest.getLongitude(), currentTime,
            this.peakHoursServingRadiusInKms);
      }
    } else {
      rst = restaurantRepositoryService.findAllRestaurantsCloseBy(
          getRestaurantsRequest.getLatitude(),
          getRestaurantsRequest.getLongitude(),
          currentTime, this.normalHoursServingRadiusInKms);
    }
    GetRestaurantsResponse response = new GetRestaurantsResponse();
    response.setRestaurants(rst);
    return response;


  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    String searchString = getRestaurantsRequest.getSearchFor();
    if (searchString == null || searchString.equals("")) {
      return findAllRestaurantsCloseBy(getRestaurantsRequest, currentTime);
    }
    int currentHour = currentTime.getHour();
    int minute = currentTime.getMinute();
    boolean flag = ((currentHour >= 8 && currentHour <= 10)
        || (currentHour >= 13 && currentHour <= 14)
        || (currentHour >= 19 && currentHour <= 21));

    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();
    Set<String> set = new HashSet<>();
    List<Restaurant> allValid = new ArrayList<>();
    if (flag) {
      if (((currentHour == 10) || (currentHour == 14) || (currentHour == 21)) && minute > 0) {
        for (Restaurant restaurant : restaurantRepositoryService.findRestaurantsByName(latitude,
            longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByAttributes(latitude,
                longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByItemName(latitude,
                longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByItemAttributes(latitude,
                longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }
      } else {

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByName(latitude,
                longitude, searchFor, currentTime, peakHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByAttributes(latitude,
                longitude, searchFor, currentTime, peakHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByItemName(latitude,
                longitude, searchFor, currentTime, peakHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }

        for (Restaurant restaurant :
            restaurantRepositoryService.findRestaurantsByItemAttributes(latitude,
                longitude, searchFor, currentTime, peakHoursServingRadiusInKms)) {
          if (!set.contains(restaurant.getRestaurantId())) {
            allValid.add(restaurant);
            set.add(restaurant.getRestaurantId());
          }
        }
      }
    } else {
      for (Restaurant restaurant :
          restaurantRepositoryService.findRestaurantsByName(latitude,
              longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
        if (!set.contains(restaurant.getRestaurantId())) {
          allValid.add(restaurant);
          set.add(restaurant.getRestaurantId());
        }
      }

      for (Restaurant restaurant :
          restaurantRepositoryService.findRestaurantsByAttributes(latitude,
              longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
        if (!set.contains(restaurant.getRestaurantId())) {
          allValid.add(restaurant);
          set.add(restaurant.getRestaurantId());
        }
      }

      for (Restaurant restaurant :
          restaurantRepositoryService.findRestaurantsByItemName(latitude,
              longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
        if (!set.contains(restaurant.getRestaurantId())) {
          allValid.add(restaurant);
          set.add(restaurant.getRestaurantId());
        }
      }

      for (Restaurant restaurant :
          restaurantRepositoryService.findRestaurantsByItemAttributes(latitude,
              longitude, searchFor, currentTime, normalHoursServingRadiusInKms)) {
        if (!set.contains(restaurant.getRestaurantId())) {
          allValid.add(restaurant);
          set.add(restaurant.getRestaurantId());
        }
      }
    }
    GetRestaurantsResponse response = new GetRestaurantsResponse();
    response.setRestaurants(allValid);
    return response;


  }


  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    int hour = currentTime.getHour();
    Double radius;
    if ((hour >= 8 && hour <= 10) || (hour >= 13 && hour <= 14) || (hour >= 19 && hour <= 21)) {
      radius = peakHoursServingRadiusInKms;
    } else {
      radius = normalHoursServingRadiusInKms;
    }
    Double lat = getRestaurantsRequest.getLatitude();
    Double lon = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();
    Callable<List<Restaurant>> callable1 = () -> {
      List<Restaurant> restaurantListName;
      restaurantListName = restaurantRepositoryService
          .findRestaurantsByName(lat, lon, searchFor, currentTime, radius);
      return restaurantListName;
    };
    Callable<List<Restaurant>> callable2 = () -> {
      List<Restaurant> restaurantListAttributes = restaurantRepositoryService
          .findRestaurantsByAttributes(lat, lon, searchFor,
              currentTime, radius);
      return restaurantListAttributes;
    };
    Callable<List<Restaurant>> callable3 = () -> {
      List<Restaurant> restaurantListItemName = restaurantRepositoryService
          .findRestaurantsByItemName(lat, lon, searchFor, currentTime, radius);
      return restaurantListItemName;
    };
    Callable<List<Restaurant>> callable4 = () -> {
      List<Restaurant> restaurantListItemAtrributes =
          restaurantRepositoryService
              .findRestaurantsByItemAttributes(lat, lon, searchFor,
                  currentTime, radius);
      return restaurantListItemAtrributes;
    };

    List<Callable<List<Restaurant>>> taskList = new ArrayList<>();
    taskList.add(callable1);
    taskList.add(callable2);
    taskList.add(callable3);
    taskList.add(callable4);

    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Restaurant> restaurantList = new ArrayList<>();
    try {
      List<Future<List<Restaurant>>> futureList = executor.invokeAll(taskList);
      for (Future<List<Restaurant>> futRes : futureList) {
        restaurantList.addAll(futRes.get());
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    GetRestaurantsResponse response = new GetRestaurantsResponse();
    response.setRestaurants(restaurantList);
    return response;
  }
}
