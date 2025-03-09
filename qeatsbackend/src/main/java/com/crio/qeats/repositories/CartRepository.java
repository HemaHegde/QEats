/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.CartEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;



public interface CartRepository extends MongoRepository<CartEntity, String> {

  @Query("{'userId' : {$regex : ?0}}")
  List<CartEntity> findByUserId(String searchString);

}
