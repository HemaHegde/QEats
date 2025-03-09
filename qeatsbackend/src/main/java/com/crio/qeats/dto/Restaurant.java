/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;


// TODO: CRIO_TASK_MODULE_SERIALIZATION - Implement Restaurant class.
// Complete the class such that it produces the following JSON during serialization.
// {
//  "restaurantId": "10",
//  "name": "A2B",
//  "city": "Hsr Layout",
//  "imageUrl": "www.google.com",
//  "latitude": 20.027,
//  "longitude": 30.0,
//  "opensAt": "18:00",
//  "closesAt": "23:00",
//  "attributes": [
//    "Tamil",
//    "South Indian"
//  ]
// }
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(value = {"restaurantId","name","city","imageUrl","opensAt","closesAt",
    "latitude","longitude","attributes"})
public class Restaurant {

  private String restaurantId;
  private String name;
  private String city;
  private String imageUrl;
  private String opensAt;
  private String closesAt;
  @NotNull
  @NotEmpty
  @Range(min = -90, max = 90)
  private double latitude;
  @NotNull
  @NotEmpty
  @Range(min = -180,max = 180)
  private  double longitude;
  private List<String> attributes;



  public String getRestaurantId() {
    return this.restaurantId;
  }

  public void setRestaurantId(String restaurantId) {
    this.restaurantId = restaurantId;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getImageUrl() {
    return this.imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getOpensAt() {
    return this.opensAt;
  }

  public void setOpensAt(String opensAt) {
    this.opensAt = opensAt;
  }

  public String getClosesAt() {
    return this.closesAt;
  }

  public void setClosesAt(String closesAt) {
    this.closesAt = closesAt;
  }

  public double getLatitude() {
    return this.latitude;
  }

  public double getLongitude() {
    return this.longitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public List<String> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<String> attributes) {
    this.attributes = attributes;
  }

}

