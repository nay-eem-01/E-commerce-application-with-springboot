package com.ecommerce.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    @OneToOne(mappedBy = "payment",cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    private Order order;

    private String paymentMethod;
    private String paymentGatewayName;
    private Long paymentGatewayId;
    private String paymentGatewayStatus;
    private String paymentGatewayResponseMessage;

    public Payment(String paymentMethod, String paymentGatewayName, Long paymentGatewayId, String paymentGatewayStatus, String paymentGatewayResponseMessage) {
        this.paymentMethod = paymentMethod;
        this.paymentGatewayName = paymentGatewayName;
        this.paymentGatewayId = paymentGatewayId;
        this.paymentGatewayStatus = paymentGatewayStatus;
        this.paymentGatewayResponseMessage = paymentGatewayResponseMessage;
    }
}
