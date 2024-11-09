package com.ecommerce.project.exceptionHandler;

import lombok.NoArgsConstructor;

import java.io.Serial;

@NoArgsConstructor
public class APIExceptionHandler extends RuntimeException{
   @Serial
   private static final long serialVersionUID = 1L ;

    public APIExceptionHandler(String message) {
        super(message);
    }
}
