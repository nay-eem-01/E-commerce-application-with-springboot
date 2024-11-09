# E-Commerce Application

Welcome to the E-Commerce Application! This project is a comprehensive web-based e-commerce platform designed to facilitate online product browsing, purchasing, and order management with user authentication, role-based access control, cart management, and an order processing system. The project uses **Spring Boot** as the backend framework and **JWT** for secure user authentication.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Usage](#usage)
- [Endpoints](#endpoints)
- [Contributing](#contributing)


## Features
- **User Authentication & Authorization**: Utilizes JWT for token-based authentication. Supports user roles like Admin, Seller, and Customer.
- **Role-Based Access Control (RBAC)**: Permissions are granted based on user roles.
- **CRUD Operations for Products and Orders**: Allows admins to manage product inventory and order processing.
- **Cart Management**: Supports adding, updating, and removing products from the cart.
- **Address Management**: Allows users to add, update, and manage their addresses.
- **Order Categorization**: Orders can be categorized for better management.
- **Secure Payments**: Facilitates order placement with payment methods and gateway integration.
- **User Management**: Sign up, sign in, and sign out functionalities.

## Tech Stack
- **Backend**: Spring Boot
- **Security**: Spring Security with JWT
- **Database**: MySQL
- **Dependencies**: Hibernate (ORM), Lombok, Spring Data JPA, Validation libraries
- **Build Tool**: Maven

## Architecture
This project follows a typical layered architecture:
- **Controllers**: Handle HTTP requests and responses.
- **Services**: Contain business logic.
- **Repositories**: Handle data access.
- **Entities**: Represent database tables.
- **DTOs (Data Transfer Objects)**: Used for data transfer between client and server.
- **Utilities**: Helper classes for common functions, including JWT management.
## Usage

- Access the application at [http://localhost:8080](http://localhost:8080).
- Use Postman or any API client to test endpoints for authentication, cart management, and product management.
- Sign up using `/api/auth/signup` and log in using `/api/auth/signin` to receive a JWT token.
- Use the token for subsequent authenticated requests.

## Endpoints

### Authentication
- **POST** `/api/auth/signin`: Sign in and get a JWT token.
- **POST** `/api/auth/signup`: Register a new user.
- **POST** `/api/auth/signout`: Sign out the user.

### Products
- **POST** `/api/admin/categories/{categoryId}/products`: Add a new product (Admin-only).
- **GET** `/api/public/products`: Get all products.
- **PUT** `/api/admin/products/{productId}/update`: Update product (Admin-only).
- **DELETE** `/api/admin/products/{productId}/delete`: Delete a product (Admin-only).

### Address Management
- **POST** `/api/user/address/add`: Add a new address for the logged-in user.
- **GET** `/api/user/address/all`: Retrieve all addresses of the logged-in user.
- **PUT** `/api/user/address/{addressId}/update`: Update an existing address.
- **DELETE** `/api/user/address/{addressId}/delete`: Delete an address by ID.

### Cart Management
- **POST** `/api/cart/products/{productId}/quantity/{quantity}`: Add a product to the cart or update its quantity.
- **GET** `/api/cart/all/carts`: Get all items in the cart for the logged-in user.
- **DELETE** `/api/cart/products/{productId}/remove`: Remove a product from the cart.

### Order Management & Categorization
- **POST** `/api/order/place/{paymentMethod}`: Place an order using a specified payment method.
- **GET** `/api/order/category/{category}`: Retrieve orders by category for better organization and filtering.

## Contributing
Contributions are welcome! Please fork this repository, make changes, and open a pull request. For major changes, open an issue first to discuss what you'd like to change.
