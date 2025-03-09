package com.crio.qeats.exchanges;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCartRequest {

  @NotNull
  @NotEmpty
  private String cartId;
  @NotNull
  @NotEmpty
  private String itemId;
  @NotNull
  @NotEmpty
  private String restaurantId;

}

