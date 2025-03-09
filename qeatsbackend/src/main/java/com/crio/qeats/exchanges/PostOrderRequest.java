package com.crio.qeats.exchanges;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostOrderRequest {

  @NotNull
  @NotEmpty
  private String cartId;

}