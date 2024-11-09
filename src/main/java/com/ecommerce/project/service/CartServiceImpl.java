package com.ecommerce.project.service;

import com.ecommerce.project.DTO.CartDto;
import com.ecommerce.project.DTO.ProductDTO;
import com.ecommerce.project.Util.AuthUtil;
import com.ecommerce.project.exceptionHandler.APIExceptionHandler;
import com.ecommerce.project.exceptionHandler.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepositories;
import com.ecommerce.project.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {


    private final ProductRepository productRepository;

    private final CartItemRepository cartItemRepository;

    private final CartRepositories cartRepositories;

    private final ModelMapper modelMapper;

    private final AuthUtil authUtil;

    public CartServiceImpl(ProductRepository productRepository, CartItemRepository cartItemRepository, CartRepositories cartRepositories, ModelMapper modelMapper, AuthUtil authUtil) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartRepositories = cartRepositories;
        this.modelMapper = modelMapper;
        this.authUtil = authUtil;
    }

    @Override
    public CartDto addProductInCart(Long productId, Integer quantity) {

        Cart cart = createCart();
        Product product = productRepository.findById(productId).
                orElseThrow(() -> new ResourceNotFoundException("Product", "product id", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cart.getCartId());

        if (cartItem != null) {
            throw new APIExceptionHandler("Product  " + product.getProductName() + "already exists in the cart");
        }
        if (product.getQuantity() == 0) {
            throw new APIExceptionHandler("Product is currently out of stock");
        }
        if (product.getQuantity() < quantity) {
            throw new APIExceptionHandler("Exceeds product's available quantity");
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setPrice(product.getSpecialPrice());

        cart.getCartItems().add(newCartItem);

        cartItemRepository.save(newCartItem);
        product.setQuantity(product.getQuantity());
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepositories.save(cart);

        CartDto cartDto = modelMapper.map(cart, CartDto.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {

            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        });

        cartDto.setProductDTOS(productDTOStream.toList());


        return cartDto;

    }

    @Override
    public List<CartDto> getAllCarts() {
        List<Cart> carts = cartRepositories.findAll();

        if (carts.isEmpty()) {
            throw new APIExceptionHandler("No cart exists");
        }

        List<CartDto> cartDtos = carts.stream().map(cart -> {
            CartDto cartDto = modelMapper.map(cart, CartDto.class);

            List<ProductDTO> productDTOS = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity());
                return productDTO;
            }).collect(Collectors.toList());

            cartDto.setProductDTOS(productDTOS);
            return cartDto;

        }).collect(Collectors.toList());

        return cartDtos;
    }

    @Override
    public CartDto getUserCart(Long cartId, String emailId) {
        Cart cart = cartRepositories.findCartByEmailAndCartId(emailId, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("cart", "cartId", cartId);
        }
        CartDto cartDto = modelMapper.map(cart, CartDto.class);
        cart.getCartItems().forEach(cartItem -> cartItem.getProduct().setQuantity(cartItem.getQuantity()));
        List<ProductDTO> productDTOS = cart.getCartItems().
                stream().
                map(cartItem -> modelMapper.map(cartItem.getProduct(), ProductDTO.class)).
                toList();
        cartDto.setProductDTOS(productDTOS);
        return cartDto;
    }

    @Transactional
    @Override
    public CartDto updateProductQuantity(Long productId, Integer quantity) {
        String email = authUtil.LoggedInEmail();
        Cart userCart = cartRepositories.findByEmailId(email);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepositories.findById(cartId).
                orElseThrow(()->new  ResourceNotFoundException("cart","cartId",cartId));
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("product","productId",productId));

        if (product.getQuantity() == 0){
            throw  new APIExceptionHandler(product.getProductName()+" is not available");
        }
        if (product.getQuantity()<quantity){
            throw  new APIExceptionHandler("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId,cartId);

        if (cartItem.getProduct() == null){
            throw  new APIExceptionHandler("Product " + product.getProductName() + " not available in the cart!!!");
        }

        int newQuantity = cartItem.getQuantity()+quantity;

        if (newQuantity < 0){
            throw new APIExceptionHandler("Resulting quantity can not be negative");
        }
        if (newQuantity == 0){
            deleteProductFromCart(cartId,productId);
        }else {
            cartItem.setPrice(product.getSpecialPrice());
            cartItem.setQuantity(newQuantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice()+(cartItem.getPrice()* quantity));
            cartRepositories.save(cart);
        }

        CartItem updatedCartItem = cartItemRepository.save(cartItem);
        if (updatedCartItem.getQuantity() == 0){
            cartItemRepository.deleteById(updatedCartItem.getCartItemId());
        }

        CartDto cartDto = modelMapper.map(cart,CartDto.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productDTOStream = cartItems.stream().map(cartItem1 -> {
            ProductDTO productDTO = modelMapper.map(cartItem1.getProduct(), ProductDTO.class);
            productDTO.setQuantity(cartItem1.getQuantity());
            return productDTO;
        });

        cartDto.setProductDTOS(productDTOStream.toList());


        return cartDto;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartID, Long productID) {

        Cart cart = cartRepositories.findById(cartID)
                .orElseThrow(()-> new ResourceNotFoundException("Cart", "cart Id",cartID));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productID,cartID);
        if (cartItem == null){
            throw new ResourceNotFoundException("Product","product Id ", productID);
        }

        cart.setTotalPrice(cart.getTotalPrice()- cartItem.getPrice()* cartItem.getQuantity());

        cartItemRepository.deleteCartItemByProductIdAndCartId( cartID, productID);


        return cartItem.getProduct().getProductName() + " is deleted from the cart";
    }

//    @Override
////    public void updateProductInCarts(Long cartID, Long productID) {
////        Carts carts = cartRepositories.findById(cartID)
////                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartID));
////        Product product =productRepository.findById(productID)
////                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productID));
////        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productID,cartID);
////
////        if (cartItem == null){
////            throw new APIExceptionHandler("Product" + product.getProductName() + " not available in the cart!!!")
////        }
////
////        Double cartPrice = (cart.getTotalPrice()-(cartItem.getPrice()*cartItem.getQuantity()));
////
////
////    }

    public Cart createCart() {
        Cart userCart = cartRepositories.findByEmailId(authUtil.LoggedInEmail());
        if (userCart != null) {
            return userCart;
        }
        Cart newCart = new Cart();
        newCart.setTotalPrice(0.00);
        newCart.setUsers(authUtil.LoggedInUser());
        return cartRepositories.save(newCart);
    }

    
}
