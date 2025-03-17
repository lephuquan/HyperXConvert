package com.hyperxconvert.api.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String packageName;
    private Double price;
    private Integer durationInDays;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}